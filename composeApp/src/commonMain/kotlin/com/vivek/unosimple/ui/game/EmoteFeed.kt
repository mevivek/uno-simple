package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.multiplayer.EmoteEvent
import com.vivek.unosimple.multiplayer.PlayerSeat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/**
 * Renders incoming [EmoteEvent]s as transient chat-style bubbles. Each
 * event auto-fades after ~2.5s. Caller positions the column (typically
 * top-center below the opponent row, or stacked above the hand).
 *
 * @param events flow of emote events from [GameSyncService.emoteEvents].
 * @param players current seat list — used to resolve senderId → display
 *   name + avatar. Unknown senders show as their raw id.
 */
@Composable
internal fun EmoteFeed(
    events: SharedFlow<EmoteEvent>,
    players: List<PlayerSeat>,
    modifier: Modifier = Modifier,
) {
    // Keep up to 3 recent events in a rolling queue. Each one lives ~2.5s
    // after it lands, then auto-removes.
    val live = remember { mutableStateListOf<StampedEmote>() }

    LaunchedEffect(events) {
        events.collect { event ->
            val stamp = StampedEmote(event = event, id = nextStampId++)
            live.add(stamp)
            if (live.size > 3) live.removeAt(0)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        live.toList().forEach { stamp ->
            LaunchedEffect(stamp.id) {
                delay(2500)
                live.remove(stamp)
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(160)) + slideInVertically(
                    animationSpec = tween(200),
                    initialOffsetY = { it / 2 },
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { -it / 2 }),
            ) {
                EmoteBubbleForSender(stamp.event, players)
            }
        }
    }
}

private data class StampedEmote(val event: EmoteEvent, val id: Long)
private var nextStampId: Long = 0L

@Composable
private fun EmoteBubbleForSender(event: EmoteEvent, players: List<PlayerSeat>) {
    val seat = players.firstOrNull { it.id == event.senderId }
    val name = seat?.displayName ?: event.senderId
    val reaction = runCatching { Reaction.valueOf(event.reaction) }.getOrNull() ?: Reaction.SMILE
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(id = event.senderId, name = name, size = 22.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.width(6.dp))
        ReactionIcon(reaction = reaction, sizeDp = 24.dp)
    }
}
