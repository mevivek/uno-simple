package com.vivek.unosimple.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.ui.game.CardView
import com.vivek.unosimple.ui.theme.LocalReducedMotion

/**
 * Soft decorative layer for the HomeScreen: a handful of playing cards
 * scattered at the edges of the background at low alpha, each gently
 * bobbing up and down. Sits BELOW the menu so the CTAs stay primary.
 *
 * All cards are face-up and rotated — the intent is mood, not information.
 */
@Composable
fun HomeBackdrop() {
    // When reduced motion is on, freeze the bob at 0 — cards are still
    // rendered (decorative), just stationary.
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "home-bob")
    val bobA by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob-a",
    )
    val bobB by transition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob-b",
    )
    val a = if (reduced) 0f else bobA
    val b = if (reduced) 0f else bobB

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left: red 7, tilted left, gentle bob.
        Box(
            modifier = Modifier
                .offset(x = (-12).dp, y = (80 + a.toInt()).dp)
                .rotate(-18f)
                .alpha(0.18f),
        ) {
            CardView(card = NumberCard(CardColor.RED, 7))
        }
        // Top-right: Skip blue, tilted right.
        Box(
            modifier = Modifier
                .offset(x = 260.dp, y = (60 + b.toInt()).dp)
                .rotate(22f)
                .alpha(0.16f),
        ) {
            CardView(card = SkipCard(CardColor.BLUE))
        }
        // Mid-left: wild star.
        Box(
            modifier = Modifier
                .offset(x = (-20).dp, y = (360 + b.toInt()).dp)
                .rotate(14f)
                .alpha(0.16f),
        ) {
            CardView(card = WildCard)
        }
        // Mid-right: reverse green.
        Box(
            modifier = Modifier
                .offset(x = 270.dp, y = (340 + a.toInt()).dp)
                .rotate(-24f)
                .alpha(0.18f),
        ) {
            CardView(card = ReverseCard(CardColor.GREEN))
        }
        // Bottom-center: marigold 4.
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = (640 + a.toInt()).dp)
                .rotate(8f)
                .alpha(0.14f),
        ) {
            CardView(card = NumberCard(CardColor.YELLOW, 4))
        }
    }
}
