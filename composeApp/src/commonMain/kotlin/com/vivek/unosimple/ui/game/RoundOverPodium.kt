package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.achievements.Achievement
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen round-result panel. Replaces the earlier centered modal —
 * takes the entire surface so the celebration has room to breathe and
 * everything the user cares about (who won, by how much, what did
 * everyone have left, did I unlock anything) is visible in one scroll.
 *
 * Sequence:
 *   - Dark scrim + title slide-in
 *   - Winner name + +delta (Bungee display)
 *   - Canvas gold / silver / bronze medal tiles with rank animate-in
 *   - Each loser's remaining-hand preview (first ~6 cards face-up) so
 *     the player can see what the bots were holding
 *   - Newly-unlocked achievement banner (if any)
 *   - Big NEXT ROUND CTA + BACK TO HOME ghost button
 */
@Composable
fun RoundOverPodium(
    state: GameState,
    onNewRound: () -> Unit,
    onBackToHome: () -> Unit = {},
    newUnlocks: Set<Achievement> = emptySet(),
) {
    if (!state.isRoundOver) return
    val winner = state.players.find { it.id == state.winnerId } ?: return

    var shown by remember(state.winnerId) { mutableStateOf(false) }
    LaunchedEffect(state.winnerId) { shown = true }
    val introScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "result-pop",
    )

    val losers = state.players
        .filter { it.id != state.winnerId }
        .sortedBy { it.handScore }
    val delta = losers.sumOf { it.handScore }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0B0F17)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(introScale)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Headline — "ROUND OVER".
            Text(
                "ROUND OVER",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            // Winner hero.
            Text(
                text = winner.name.uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "WINS +$delta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.tertiary,
            )

            Spacer(Modifier.height(28.dp))

            // Podium rows — winner first, staggered entrance.
            RankedRow(
                rank = 1,
                player = winner,
                scoreThisRound = +delta,
                highlight = true,
                revealHand = false,
                introDelayMs = 100,
            )
            losers.forEachIndexed { i, p ->
                RankedRow(
                    rank = i + 2,
                    player = p,
                    scoreThisRound = -p.handScore,
                    highlight = false,
                    revealHand = true,
                    introDelayMs = 250 + i * 150,
                )
            }

            // Newly-unlocked achievements — amber banner(s) below the podium.
            if (newUnlocks.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                newUnlocks.forEach { a ->
                    AchievementUnlockedBanner(achievement = a)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            ClayButton(
                onClick = onNewRound,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 18.dp),
            ) {
                Text(
                    "NEXT ROUND",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(10.dp))
            GhostButton(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text(
                    "BACK TO HOME",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun RankedRow(
    rank: Int,
    player: Player,
    scoreThisRound: Int,
    highlight: Boolean,
    revealHand: Boolean,
    introDelayMs: Int,
) {
    // Stagger each row's entrance.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(introDelayMs.toLong())
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(200)) + scaleIn(
            initialScale = 0.6f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (highlight) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedalDisc(rank = rank, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                PlayerAvatar(id = player.id, name = player.name, size = 40.dp)
                Column(
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Total ${player.score}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (scoreThisRound >= 0) "+$scoreThisRound" else "$scoreThisRound",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (scoreThisRound >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
            }
            // Hand reveal — the cards each loser was still holding. Capped
            // at 8 so a penalty-stack hand doesn't overflow the row.
            if (revealHand && player.hand.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    player.hand.take(8).forEach { card ->
                        CardView(
                            card = card,
                            width = 36.dp,
                            height = 54.dp,
                            enabled = true,
                            onClick = null,
                        )
                    }
                    if (player.hand.size > 8) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 54.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "+${player.hand.size - 8}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Canvas-drawn medal disc — gold / silver / bronze for ranks 1-3, slate
 * with the rank number for anything else. Replaces the emoji 🥇🥈🥉 which
 * rendered as tofu in Skiko Wasm.
 */
@Composable
private fun MedalDisc(rank: Int, size: Dp) {
    val (outer, inner) = when (rank) {
        1 -> Color(0xFFFFB53B) to Color(0xFFE8931F) // gold
        2 -> Color(0xFFD8D8DE) to Color(0xFFA1A3AE) // silver
        3 -> Color(0xFFE0884D) to Color(0xFFB66534) // bronze
        else -> Color(0xFF3A4458) to Color(0xFF2A3347)
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = minOf(this.size.width, this.size.height) / 2f
            drawCircle(outer, r, Offset(cx, cy))
            drawCircle(inner, r * 0.72f, Offset(cx, cy))
            if (rank in 1..3) {
                drawStar(cx, cy, r * 0.5f, r * 0.22f, Color.White)
            }
        }
        if (rank > 3) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
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

@Composable
private fun AchievementUnlockedBanner(achievement: Achievement) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedalDisc(rank = 1, size = 34.dp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "ACHIEVEMENT UNLOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Black,
                )
                Text(
                    achievement.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

