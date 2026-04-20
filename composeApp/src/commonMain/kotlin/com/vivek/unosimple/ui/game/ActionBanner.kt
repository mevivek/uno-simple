package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import kotlinx.coroutines.delay

/**
 * Narrates the most recent action to the player. Derives what happened by
 * diffing the previous [GameState] against the current one — no changes
 * required in the engine or view-models.
 *
 * Detection rules:
 * - Discard pile grew → someone played the top card. Message names the
 *   actor from the *previous* state's currentPlayer (turn has already
 *   advanced in the new state).
 * - Draw pile shrank without discard growing → someone drew a card. The
 *   action belonged to whichever player's hand grew.
 * - Winner transitions to non-null → round-over (silenced; the
 *   CelebrationOverlay handles this).
 *
 * Banner auto-dismisses after [DISPLAY_MS]. Multiple quick events clobber
 * each other — whichever is most recent wins.
 */
@Composable
fun ActionBanner(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    var previous by remember { mutableStateOf<GameState?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var accentColor by remember { mutableStateOf<CardColor?>(null) }

    LaunchedEffect(state) {
        val prev = previous
        previous = state
        if (prev == null) return@LaunchedEffect

        // Silence round-over; CelebrationOverlay is louder.
        if (!prev.isRoundOver && state.isRoundOver) {
            message = null
            return@LaunchedEffect
        }

        val (newMsg, newColor) = deriveEvent(prev, state)
        if (newMsg != null) {
            message = newMsg
            accentColor = newColor
            delay(DISPLAY_MS)
            if (message == newMsg) {
                message = null
            }
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        val msg = message
        if (msg != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                accentColor?.let { color ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(cardColorToCompose(color)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun deriveEvent(prev: GameState, now: GameState): Pair<String?, CardColor?> {
    val actor = prev.currentPlayer

    // 1) Discard pile grew → that player played the top discard.
    if (now.discardPile.size > prev.discardPile.size) {
        val played = now.topDiscard
        val label = when (played) {
            is NumberCard -> "${played.color.short()} ${played.number}"
            is SkipCard -> "${played.color.short()} Skip"
            is ReverseCard -> "${played.color.short()} Reverse"
            is DrawTwoCard -> "${played.color.short()} +2"
            WildCard -> "Wild (${now.activeColor.short()})"
            WildDrawFourCard -> "Wild +4 (${now.activeColor.short()})"
        }
        return "${actor.name} played $label" to played.color
    }

    // 2) Draw pile shrank without discard growing → someone drew.
    //    Look at whose hand grew to attribute correctly (could be actor,
    //    or the next player for a Draw Two / Wild Draw Four penalty).
    val growers = prev.players.zip(now.players).filter { (p, n) -> n.handSize > p.handSize }
    if (growers.isNotEmpty() && prev.drawPile.size > now.drawPile.size) {
        val (_, nowPlayer) = growers.first()
        val drew = growers.first().second.handSize - growers.first().first.handSize
        val verb = if (drew == 1) "drew a card" else "drew $drew cards"
        return "${nowPlayer.name} $verb" to null
    }

    return null to null
}

/** Single-letter color short name used in banner text. */
private fun CardColor.short(): String = when (this) {
    CardColor.RED -> "R"
    CardColor.YELLOW -> "Y"
    CardColor.GREEN -> "G"
    CardColor.BLUE -> "B"
}

@Suppress("unused")
private fun Player.shortSignature(): String = "$name (${handSize})"

private const val DISPLAY_MS: Long = 1800L
