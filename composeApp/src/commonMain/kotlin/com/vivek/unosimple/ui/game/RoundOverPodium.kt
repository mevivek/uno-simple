package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.ui.common.TrophyIcon
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface

/**
 * Post-win overlay: a centered panel showing the winner's name + the round
 * delta (points they just gained), followed by a ranked list of every player
 * with their hand-score contribution. A "New round" button sits below.
 *
 * Appears over the table while confetti is still raining from
 * [CelebrationOverlay]. Dismissible only via "New round" or the menu — the
 * game state stays visible behind it so the player can review the final
 * board before dealing again.
 */
@Composable
fun RoundOverPodium(
    state: GameState,
    onNewRound: () -> Unit,
) {
    if (!state.isRoundOver) return
    val winner = state.players.find { it.id == state.winnerId } ?: return

    // "Pop in" scale animation on first show. We key on winnerId so a new
    // round resets the pop animation for the next win.
    var shown by remember(state.winnerId) { mutableStateOf(false) }
    LaunchedEffect(state.winnerId) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "podium-pop",
    )

    // Ranking: winner first (0 points remaining), then others sorted by
    // remaining hand-score ascending — the least-costly hand loses the
    // fewest points.
    val losers = state.players
        .filter { it.id != state.winnerId }
        .sortedBy { it.handScore }

    val delta = losers.sumOf { it.handScore }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.scale(scale).padding(horizontal = 24.dp)) {
            ClaySurface(
                color = MaterialTheme.colorScheme.surface,
                cornerRadius = 28.dp,
                elevation = 16.dp,
                modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TrophyIcon(size = 44.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = winner.name,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "+$delta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary,
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PodiumRow(
                            rank = 1,
                            player = winner,
                            scoreThisRound = +delta,
                            highlight = true,
                        )
                        losers.forEachIndexed { i, p ->
                            PodiumRow(
                                rank = i + 2,
                                player = p,
                                scoreThisRound = -p.handScore,
                                highlight = false,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    ClayButton(
                        onClick = onNewRound,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "New round",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumRow(
    rank: Int,
    player: Player,
    scoreThisRound: Int,
    highlight: Boolean,
) {
    val bg = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }
    val prefix = if (scoreThisRound >= 0) "+" else ""
    val deltaColor = if (scoreThisRound >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = medal,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 10.dp),
        )
        PlayerAvatar(id = player.id, name = player.name, size = 28.dp)
        Column(
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${player.score}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$prefix$scoreThisRound",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = deltaColor,
        )
    }
}
