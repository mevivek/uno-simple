package com.vivek.unosimple.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.ui.game.CardView
import com.vivek.unosimple.ui.theme.LocalClayTokens
import kotlinx.coroutines.delay

/**
 * 1.4-second brand splash — fan of four cards settles into place under the
 * UNO SIMPLE wordmark, amber spotlight blooms, then hands off to [onDone].
 * Shown on every cold launch (first run + returning users alike) so the
 * app has a consistent "opening sting" rather than a blank-to-Home jump.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val cardFan = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        cardFan.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        title.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
        delay(400)
        onDone()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to LocalClayTokens.current.tableAccent.copy(alpha = 0.7f),
                        0.55f to MaterialTheme.colorScheme.background,
                        radius = 900f,
                        center = Offset(600f, 700f),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Fan of four cards behind the title. Each card rotates +
                // scales in from the center using its own delay offset.
                Box(contentAlignment = Alignment.Center) {
                    CardFanPiece(
                        rotationTarget = -28f,
                        xOffsetDp = -70,
                        yOffsetDp = 14,
                        progress = cardFan.value,
                        delay = 0f,
                    ) { CardView(card = NumberCard(CardColor.RED, 7)) }
                    CardFanPiece(
                        rotationTarget = -10f,
                        xOffsetDp = -24,
                        yOffsetDp = -4,
                        progress = cardFan.value,
                        delay = 0.2f,
                    ) { CardView(card = SkipCard(CardColor.BLUE)) }
                    CardFanPiece(
                        rotationTarget = 10f,
                        xOffsetDp = 24,
                        yOffsetDp = -4,
                        progress = cardFan.value,
                        delay = 0.4f,
                    ) { CardView(card = NumberCard(CardColor.YELLOW, 4)) }
                    CardFanPiece(
                        rotationTarget = 28f,
                        xOffsetDp = 70,
                        yOffsetDp = 14,
                        progress = cardFan.value,
                        delay = 0.6f,
                    ) { CardView(card = WildCard) }
                }

                Spacer(Modifier.height(48.dp))

                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = title.value
                        val s = 0.85f + 0.15f * title.value
                        scaleX = s
                        scaleY = s
                    },
                ) {
                    Text(
                        text = "UNO SIMPLE",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFanPiece(
    rotationTarget: Float,
    xOffsetDp: Int,
    yOffsetDp: Int,
    progress: Float,
    delay: Float,
    content: @Composable () -> Unit,
) {
    // Each card's "own progress" is a slice of the overall progress so they
    // fan in sequentially (left → right) rather than all at once.
    val t = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
    val rot = rotationTarget * t
    val alpha = t
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = xOffsetDp.dp.toPx() * t
                translationY = yOffsetDp.dp.toPx() * t
                this.alpha = alpha
                scaleX = 0.6f + 0.4f * t
                scaleY = 0.6f + 0.4f * t
            }
            .rotate(rot),
    ) { content() }
}
