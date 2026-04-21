package com.vivek.unosimple.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivek.unosimple.persistence.HistoryRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.theme.ClaySurface

/**
 * Stats dashboard: win rate, games played, streaks, best round, and a
 * frequent-opponents breakdown. All pulled from [HistoryRepository.stats].
 * The win-rate ring is Canvas-drawn so it stays crisp.
 */
@Composable
fun StatsScreen(
    history: HistoryRepository,
    humanId: String,
    onBack: () -> Unit,
    onOpenAchievements: () -> Unit = {},
) {
    val records by history.records.collectAsState()
    // Recompute stats whenever records change.
    val stats = remember(records, humanId) { history.stats(humanId) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("stats_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "YOUR STATS",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(28.dp))

                WinRateRing(
                    winRatePct = stats.winRatePct,
                    wins = stats.gamesWon,
                    total = stats.gamesPlayed,
                )

                Spacer(Modifier.height(28.dp))

                // Row of three stat chips — streak, best, played.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatChip(label = "STREAK", value = "${stats.currentWinStreak}", modifier = Modifier.weight(1f))
                    StatChip(label = "BEST", value = "${stats.bestWinStreak}", modifier = Modifier.weight(1f))
                    StatChip(label = "BIG WIN", value = "${stats.bestRoundDelta}", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                if (stats.opponentCounts.isNotEmpty()) {
                    Text(
                        "OPPONENTS FACED",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                    )
                    ClaySurface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        cornerRadius = 14.dp,
                        elevation = 6.dp,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            stats.opponentCounts.entries.sortedByDescending { it.value }.forEach { (name, count) ->
                                OpponentRowStat(
                                    name = name,
                                    faced = count,
                                    lostTo = stats.opponentLosses[name] ?: 0,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                ClaySurface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAchievements),
                    color = MaterialTheme.colorScheme.primary,
                    cornerRadius = 14.dp,
                    elevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ACHIEVEMENTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "VIEW \u203A",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                if (stats.gamesPlayed == 0) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Finish a round to start filling this up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
        }
    }
}

@Composable
private fun WinRateRing(winRatePct: Int, wins: Int, total: Int) {
    val size = 180.dp
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val frac = winRatePct.coerceIn(0, 100) / 100f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f
            val topLeft = Offset(inset, inset)
            val boxSize = Size(this.size.width - stroke, this.size.height - stroke)
            // Background track.
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = boxSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Filled win-rate arc.
            drawArc(
                color = fill,
                startAngle = -90f,
                sweepAngle = 360f * frac,
                useCenter = false,
                topLeft = topLeft,
                size = boxSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$winRatePct%",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
            )
            Text(
                "$wins / $total WINS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    ClaySurface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun OpponentRowStat(name: String, faced: Int, lostTo: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
        Row {
            Text(
                "faced $faced",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "lost $lostTo",
                style = MaterialTheme.typography.labelMedium,
                color = if (lostTo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
