package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.vivek.unosimple.ui.theme.LocalReducedMotion
import kotlin.random.Random

/**
 * Pink-hearts-rain overlay. Used as a one-shot easter egg reward when the
 * player types Geet's name during onboarding. Draws ~60 translucent
 * heart-shaped particles falling across the screen over [durationMs]ms.
 * A private surprise — not documented in the main UI. 💛
 */
@Composable
fun HeartsRainOverlay(
    visible: Boolean,
    durationMs: Int = 3800,
) {
    if (LocalReducedMotion.current) return
    val seed = remember(visible) { if (visible) Random.nextLong() else 0L }
    var started by remember(visible) { mutableStateOf(false) }
    LaunchedEffect(visible) { started = visible }

    val progress by animateFloatAsState(
        targetValue = if (visible && started) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) durationMs else 0, easing = LinearEasing),
        label = "hearts-progress",
    )

    if (!visible) return

    val rng = remember(seed) { Random(seed) }
    val hearts = remember(seed) { buildHearts(rng) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        hearts.forEach { h ->
            val t = progress
            val cx = (h.startX + h.driftX * t) * size.width
            val cy = (h.startY * size.height) + (t * size.height * 1.25f)
            val alpha = when {
                t < 0.08f -> t / 0.08f
                t > 0.85f -> ((1f - t) / 0.15f).coerceAtLeast(0f)
                else -> 1f
            }
            drawHeart(
                center = Offset(cx, cy),
                size = h.size,
                color = h.color.copy(alpha = alpha * 0.8f),
            )
        }
    }
}

private data class HeartParticle(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val size: Float,
    val color: Color,
)

private fun buildHearts(rng: Random): List<HeartParticle> {
    val palette = listOf(
        Color(0xFFFF5168),
        Color(0xFFFF8FA3),
        Color(0xFFFFB3C1),
        Color(0xFFFFCD3C),
        Color(0xFFFFFFFF),
    )
    return List(60) {
        HeartParticle(
            startX = rng.nextFloat(),
            startY = -0.1f - rng.nextFloat() * 0.3f,
            driftX = (rng.nextFloat() - 0.5f) * 0.35f,
            size = 14f + rng.nextFloat() * 20f,
            color = palette[rng.nextInt(palette.size)],
        )
    }
}

/** A simple heart glyph drawn from two semicircles + a triangle. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    center: Offset,
    size: Float,
    color: Color,
) {
    val w = size
    val h = size
    val path = Path().apply {
        val left = center.x - w / 2f
        val top = center.y - h / 2f
        // Two rounded bumps on top + a V down to the bottom point.
        moveTo(center.x, top + h * 0.25f)
        cubicTo(
            left + w * 0.05f, top - h * 0.05f,
            left - w * 0.15f, top + h * 0.45f,
            center.x, top + h,
        )
        cubicTo(
            left + w * 1.15f, top + h * 0.45f,
            left + w * 0.95f, top - h * 0.05f,
            center.x, top + h * 0.25f,
        )
        close()
    }
    drawPath(path, color = color)
}
