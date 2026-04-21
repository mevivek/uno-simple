package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Simple self-emote system — tap a circular 😊 button bottom-left, a small
 * palette of six emojis slides up, pick one and it pops as a speech bubble
 * next to the human's hand area. Fades out after ~2s.
 *
 * No multiplayer broadcast yet; local-only for now. When F2 / online polish
 * lands, the [onEmote] callback here will fan out through [com.vivek
 * .unosimple.multiplayer.GameSyncService] so everyone in the room sees it.
 */

/**
 * Fixed emoji palette. Kept small on purpose — Clash Royale-style "enough
 * to emote, not enough to spam". Order roughly matches common reactions:
 * happy → surprised → praise → fire → skeptical → celebrate.
 */
internal val EMOTES: List<String> = listOf(
    "\uD83D\uDE0A",   // 😊 smile
    "\uD83D\uDE31",   // 😱 screaming
    "\uD83D\uDC4F",   // 👏 clap
    "\uD83D\uDD25",   // 🔥 fire
    "\uD83D\uDE44",   // 🙄 eye-roll
    "\uD83C\uDF89",   // 🎉 party
)

/**
 * Bottom-left emote corner. Contains:
 * - a round 😊 button that toggles the palette
 * - the palette row that slides up above the button when open
 * - the active speech-bubble that appears briefly when the user picks one
 */
@Composable
internal fun EmoteCorner(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    var active: String? by remember { mutableStateOf(null) }

    // Clear the active emote after 2.2s so the bubble fades away on its own.
    LaunchedEffect(active) {
        if (active != null) {
            delay(2200)
            active = null
        }
    }

    Box(modifier = modifier) {
        // Palette (slides up from the button when open).
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                initialOffsetY = { it / 2 },
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 58.dp),
        ) {
            EmotePalette(
                onPick = {
                    active = it
                    open = false
                },
            )
        }

        // Active-emote speech bubble — anchored just above the 😊 button.
        AnimatedVisibility(
            visible = active != null,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.5f),
            exit = fadeOut(tween(240)) + scaleOut(targetScale = 1.3f),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 12.dp),
        ) {
            EmoteBubble(emoji = active ?: "")
        }

        // The 😊 button itself.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { open = !open }
                .testTag("emote.toggle"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDE0A",
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
private fun EmotePalette(onPick: (String) -> Unit) {
    // Dark rounded pill containing six tappable emoji tiles in a row.
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EMOTES.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onPick(emoji) }
                    .testTag("emote.pick.$emoji"),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun EmoteBubble(emoji: String) {
    // Pill-style speech bubble with a little "tail" on the bottom-left
    // pointing back at the 😊 button.
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}
