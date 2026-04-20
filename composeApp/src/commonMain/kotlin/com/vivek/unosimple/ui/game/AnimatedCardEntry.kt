package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Wraps a card composable with a one-shot slide-up + fade-in animation on
 * first composition. Used in the player's hand so dealt / drawn cards feel
 * alive rather than popping into place.
 *
 * The animation runs when this [AnimatedCardEntry] first mounts. If the
 * underlying card list later mutates (playing a card, reshuffle, new round),
 * cards that already mounted stay static while newly-added cards animate in
 * with their own stagger.
 *
 * @param index position in the parent row; drives the stagger delay so
 *   multiple cards fanning out feel sequenced rather than all-at-once.
 */
@Composable
fun AnimatedCardEntry(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * STAGGER_DELAY_MS)
        visible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else ENTRY_OFFSET,
        animationSpec = tween(durationMillis = ENTRY_DURATION_MS, easing = FastOutSlowInEasing),
        label = "card-entry-offset",
    )
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = ENTRY_DURATION_MS),
        label = "card-entry-alpha",
    )

    Box(modifier = Modifier.offset(y = offsetY).alpha(fade)) {
        content()
    }
}

private const val STAGGER_DELAY_MS = 80L
private const val ENTRY_DURATION_MS = 380
private val ENTRY_OFFSET = 100.dp
