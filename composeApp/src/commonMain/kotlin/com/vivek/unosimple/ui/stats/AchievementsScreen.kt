package com.vivek.unosimple.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.achievements.Achievement
import com.vivek.unosimple.persistence.AchievementRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.theme.ClaySurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Grid of all achievements. Unlocked ones glow amber with a canvas-drawn
 * star/trophy medal; locked ones are greyed and show the description as
 * the "how to unlock" hint.
 */
@Composable
fun AchievementsScreen(
    achievements: AchievementRepository,
    onBack: () -> Unit,
) {
    val unlocked by achievements.unlocked.collectAsState()
    val all = Achievement.entries

    Surface(
        modifier = Modifier.fillMaxSize().testTag("achievements_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                BackIcon(size = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 72.dp, bottom = 32.dp),
            ) {
                Text(
                    "ACHIEVEMENTS",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${unlocked.size} of ${all.size} unlocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(Modifier.height(20.dp))

                // 2-column grid via manual Row batching.
                val chunks = all.chunked(2)
                chunks.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { a ->
                            AchievementTile(
                                achievement = a,
                                unlocked = a in unlocked,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AchievementTile(
    achievement: Achievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    ClaySurface(
        modifier = modifier,
        color = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        cornerRadius = 14.dp,
        elevation = if (unlocked) 8.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .alpha(if (unlocked) 1f else 0.55f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MedalBadge(unlocked = unlocked, size = 56.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = achievement.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Canvas-drawn medal — concentric circles with a five-point star inside.
 * Unlocked = amber + gold with a soft glow; locked = slate monochrome.
 */
@Composable
private fun MedalBadge(unlocked: Boolean, size: androidx.compose.ui.unit.Dp) {
    val primary = if (unlocked) Color(0xFFFFB53B) else Color(0xFF5A6478)
    val secondary = if (unlocked) Color(0xFFE8931F) else Color(0xFF404A5E)
    val star = if (unlocked) Color.White else Color(0xFF1D2435)
    Box(modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = minOf(this.size.width, this.size.height) / 2f
            // Outer ring.
            drawCircle(primary, radius = r, center = Offset(cx, cy))
            drawCircle(secondary, radius = r * 0.78f, center = Offset(cx, cy))
            // Star.
            drawStar(cx, cy, r * 0.55f, r * 0.23f, star)
        }
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, outer: Float, inner: Float, color: Color) {
    val path = Path().apply {
        val points = 5
        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outer else inner
            val angleRad = -PI / 2 + i * PI / points
            val x = cx + (radius * cos(angleRad)).toFloat()
            val y = cy + (radius * sin(angleRad)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
}
