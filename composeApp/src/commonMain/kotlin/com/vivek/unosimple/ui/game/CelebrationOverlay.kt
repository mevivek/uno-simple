package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import com.vivek.unosimple.ui.theme.LocalReducedMotion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.vivek.unosimple.engine.models.CardColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti burst drawn on a full-screen Canvas overlay. Fires once on every
 * round win — N particles spawn near the top, fall under gravity with
 * random horizontal drift, rotate, and fade out. The overlay doesn't
 * intercept touches; the UI behind it stays interactive.
 *
 * Uses the theme's card colors + primary to match the game palette.
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    durationMs: Int = 3200,
) {
    // Reduced-motion: skip confetti entirely. The podium + trophy already
    // communicate "round over" without the particle rain.
    if (LocalReducedMotion.current) return
    // Progress 0..1 drives every particle's position/alpha/rotation. Seeds
    // are re-drawn each time `visible` flips true so the pattern varies
    // between rounds.
    val seed = remember(visible) { if (visible) Random.nextLong() else 0L }
    var started by remember(visible) { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) started = true else started = false
    }

    val progress by animateFloatAsState(
        targetValue = if (visible && started) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) durationMs else 0, easing = LinearEasing),
        label = "celebration-progress",
    )

    if (!visible) return

    val rng = remember(seed) { Random(seed) }
    val particles = remember(seed) { buildParticles(rng) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            // t is this particle's normalized lifetime within [0, 1].
            val t = progress

            val px = (p.startX + p.driftX * t) * size.width
            val py = (p.startY * size.height) + (t * t * size.height * 1.2f) - (t * 0.15f * size.height)
            val alpha = when {
                t < 0.1f -> t / 0.1f                   // fade in
                t > 0.8f -> ((1f - t) / 0.2f).coerceAtLeast(0f)  // fade out
                else -> 1f
            }
            // Tiny rotated rectangle for "confetti flake" feel.
            val angle = (p.rotationStart + t * p.rotationSpeed) % (2 * PI).toFloat()
            val half = p.size / 2f
            val cosA = cos(angle)
            val sinA = sin(angle)
            val corners = listOf(
                Offset(-half, -half),
                Offset(half, -half),
                Offset(half, half),
                Offset(-half, half),
            ).map { c -> Offset(px + c.x * cosA - c.y * sinA, py + c.x * sinA + c.y * cosA) }

            // Flat rectangle via two triangles using drawPath.
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(corners[0].x, corners[0].y)
                lineTo(corners[1].x, corners[1].y)
                lineTo(corners[2].x, corners[2].y)
                lineTo(corners[3].x, corners[3].y)
                close()
            }
            drawPath(path, color = p.color.copy(alpha = alpha))
        }
    }
}

private fun buildParticles(rng: Random): List<ConfettiParticle> {
    val palette = listOf(
        Color(0xFFE63946), // red
        Color(0xFFFFC857), // yellow
        Color(0xFF38B000), // green
        Color(0xFF2A9DF4), // blue
        Color(0xFFFFB627), // gold
        Color(0xFF52D4E4), // cyan
    )
    return List(80) {
        ConfettiParticle(
            startX = rng.nextFloat(),             // 0..1 of canvas width
            startY = -0.05f,                      // just above the top
            driftX = (rng.nextFloat() - 0.5f) * 0.4f,
            size = 8f + rng.nextFloat() * 10f,    // px
            color = palette[rng.nextInt(palette.size)],
            rotationStart = rng.nextFloat() * (2 * PI).toFloat(),
            rotationSpeed = (rng.nextFloat() - 0.5f) * 12f,
        )
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val size: Float,
    val color: Color,
    val rotationStart: Float,
    val rotationSpeed: Float,
)

// Suppress unused-import warning on release of CardColor reference if we
// decide to incorporate the official card palette directly here later.
@Suppress("unused")
private val unusedCardColorRef: CardColor? = null
