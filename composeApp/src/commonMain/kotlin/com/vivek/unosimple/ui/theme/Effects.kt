package com.vivek.unosimple.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Motion + decorative primitives for the game surface. Every animation here
 * consults [LocalReducedMotion] and short-circuits or renders a stationary
 * fallback when it's true.
 *
 * Pattern:
 * - Controllers are plain classes holding Compose-observable `Animatable`
 *   values. Read `controller.<thing>.value` from within a composition; it
 *   recomposes automatically as the animation ticks.
 * - Firing an animation happens from a coroutine scope (`scope.launch {
 *   controller.shake() }`).
 */

// ---------------------------------------------------------------------------
// Shake — horizontal judder for opponent tiles that take a +2/+4 hit.
// ---------------------------------------------------------------------------

class ShakeController {
    /** Current X offset in dp-units (Compose-observable). */
    val offsetDp: Animatable<Float, *> = Animatable(0f)

    suspend fun shake(intensity: Float = 6f) {
        val steps = floatArrayOf(
            intensity,
            -intensity * 0.8f,
            intensity * 0.6f,
            -intensity * 0.4f,
            intensity * 0.2f,
            0f,
        )
        for (s in steps) {
            offsetDp.animateTo(s, tween(durationMillis = 60, easing = FastOutSlowInEasing))
        }
    }
}

@Composable
fun rememberShakeController(): ShakeController = remember { ShakeController() }

/** Apply the controller's current offset as a horizontal translation. */
fun Modifier.shakeOffset(controller: ShakeController): Modifier = this.offset(x = controller.offsetDp.value.dp)

fun CoroutineScope.launchShake(c: ShakeController, intensity: Float = 6f) {
    launch { c.shake(intensity) }
}

// ---------------------------------------------------------------------------
// Flash — full-screen color wash for big moments (Wild +4 land, UNO declare).
// ---------------------------------------------------------------------------

class FlashController {
    val alpha: Animatable<Float, *> = Animatable(0f)
    var color: Color by mutableStateOf(Color.White)
        internal set

    suspend fun flash(
        flashColor: Color = Color.White,
        peakAlpha: Float = 0.7f,
        durationMs: Int = 360,
    ) {
        color = flashColor
        alpha.snapTo(peakAlpha)
        alpha.animateTo(0f, tween(durationMillis = durationMs, easing = FastOutSlowInEasing))
    }
}

@Composable
fun rememberFlashController(): FlashController = remember { FlashController() }

/**
 * Full-screen flash overlay. Drop as the topmost child of any
 * `Box(fillMaxSize)`. Non-interactive — just paints a rect at the
 * controller's current alpha. Short-circuits when reduced motion is on.
 */
@Composable
fun FlashOverlay(controller: FlashController) {
    if (LocalReducedMotion.current) return
    val a = controller.alpha.value
    if (a <= 0f) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                drawRect(controller.color.copy(alpha = a))
            },
    )
}

fun CoroutineScope.launchFlash(
    c: FlashController,
    color: Color = Color.White,
    peakAlpha: Float = 0.7f,
    durationMs: Int = 360,
) {
    launch { c.flash(color, peakAlpha, durationMs) }
}

// ---------------------------------------------------------------------------
// Noise-textured background — subtle dither over a base fill.
// ---------------------------------------------------------------------------

/**
 * Paints [base] as the surface fill, then scatters faint white speckles
 * across it so the dark felt doesn't read as a flat gradient. The speckle
 * positions are deterministic per [seed] — no per-frame cost after the
 * first draw is cached.
 *
 * Call it as the FIRST modifier in the chain of a container box.
 *
 * @param density speckles per 100,000 sq px. Default is tuned for phones.
 */
fun Modifier.noiseBackground(
    base: Color,
    speckleColor: Color = Color.White.copy(alpha = 0.04f),
    density: Float = 0.5f,
    seed: Int = 1729,
): Modifier = this.drawWithContent {
    drawRect(base)
    val rng = Random(seed)
    val count = ((size.width * size.height) / 100_000f * density).toInt().coerceAtMost(500)
    repeat(count) {
        val x = rng.nextFloat() * size.width
        val y = rng.nextFloat() * size.height
        drawCircle(
            color = speckleColor,
            radius = rng.nextFloat() * 1.1f + 0.3f,
            center = Offset(x, y),
        )
    }
    drawContent()
}
