package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Avatar for any seat. Bot seats ("bot1", "bot2", …) render an illustrated
 * character face drawn entirely with Canvas primitives — ears, eyes, mouth,
 * optional blush + accessory — so every opponent has a distinct personality
 * without needing packaged image assets. Human/hotseat seats fall back to
 * the original colored disc with their first initial.
 */
@Composable
internal fun PlayerAvatar(
    id: String,
    name: String,
    size: Dp = 36.dp,
) {
    if (isBotId(id)) {
        BotAvatar(persona = personaFor(id), size = size)
        return
    }
    val bg = avatarColorFor(id)
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    0f to bg.lighten(0.18f),
                    1f to bg.darken(0.08f),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
    }
}

@Composable
private fun BotAvatar(persona: BotPersona, size: Dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val faceR = minOf(w, h) * 0.42f

        // Ears / antennae — drawn behind the face so they tuck under.
        when (persona.earShape) {
            EarShape.ROUND -> {
                val er = faceR * 0.38f
                drawCircle(persona.earColor, er, androidx.compose.ui.geometry.Offset(cx - faceR * 0.72f, cy - faceR * 0.72f))
                drawCircle(persona.earColor, er, androidx.compose.ui.geometry.Offset(cx + faceR * 0.72f, cy - faceR * 0.72f))
            }
            EarShape.POINTY -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - faceR * 0.9f, cy - faceR * 0.2f)
                    lineTo(cx - faceR * 0.55f, cy - faceR * 1.25f)
                    lineTo(cx - faceR * 0.2f, cy - faceR * 0.6f)
                    close()
                }
                drawPath(path, persona.earColor)
                val path2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx + faceR * 0.2f, cy - faceR * 0.6f)
                    lineTo(cx + faceR * 0.55f, cy - faceR * 1.25f)
                    lineTo(cx + faceR * 0.9f, cy - faceR * 0.2f)
                    close()
                }
                drawPath(path2, persona.earColor)
            }
            EarShape.ANTENNA -> {
                val stem = androidx.compose.ui.graphics.drawscope.Stroke(width = faceR * 0.10f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(
                    persona.earColor,
                    androidx.compose.ui.geometry.Offset(cx - faceR * 0.35f, cy - faceR * 0.6f),
                    androidx.compose.ui.geometry.Offset(cx - faceR * 0.55f, cy - faceR * 1.25f),
                    strokeWidth = faceR * 0.10f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    persona.earColor,
                    androidx.compose.ui.geometry.Offset(cx + faceR * 0.35f, cy - faceR * 0.6f),
                    androidx.compose.ui.geometry.Offset(cx + faceR * 0.55f, cy - faceR * 1.25f),
                    strokeWidth = faceR * 0.10f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawCircle(persona.earColor, faceR * 0.16f, androidx.compose.ui.geometry.Offset(cx - faceR * 0.55f, cy - faceR * 1.28f))
                drawCircle(persona.earColor, faceR * 0.16f, androidx.compose.ui.geometry.Offset(cx + faceR * 0.55f, cy - faceR * 1.28f))
            }
            EarShape.NONE -> Unit
        }

        // Face.
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to persona.faceColor.lighten(0.12f),
                1f to persona.faceColor.darken(0.08f),
            ),
            radius = faceR,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
        )

        // Optional brown "spot" patch over one eye (Max the dog).
        if (persona.accessory == Accessory.SPOT) {
            drawCircle(persona.earColor, faceR * 0.35f, androidx.compose.ui.geometry.Offset(cx - faceR * 0.32f, cy - faceR * 0.08f))
        }

        // Blush.
        if (persona.blush) {
            val blush = Color(0xFFFF8FA3).copy(alpha = 0.55f)
            drawCircle(blush, faceR * 0.16f, androidx.compose.ui.geometry.Offset(cx - faceR * 0.55f, cy + faceR * 0.25f))
            drawCircle(blush, faceR * 0.16f, androidx.compose.ui.geometry.Offset(cx + faceR * 0.55f, cy + faceR * 0.25f))
        }

        // Heart-cheek accessory (Bea).
        if (persona.accessory == Accessory.HEART_CHEEKS) {
            drawCircle(Color(0xFFE5596C), faceR * 0.08f, androidx.compose.ui.geometry.Offset(cx - faceR * 0.5f, cy + faceR * 0.3f))
            drawCircle(Color(0xFFE5596C), faceR * 0.08f, androidx.compose.ui.geometry.Offset(cx + faceR * 0.5f, cy + faceR * 0.3f))
        }

        // Eyes.
        val eyeL = androidx.compose.ui.geometry.Offset(cx - faceR * 0.32f, cy - faceR * 0.1f)
        val eyeR = androidx.compose.ui.geometry.Offset(cx + faceR * 0.32f, cy - faceR * 0.1f)
        when (persona.eyeStyle) {
            EyeStyle.ROUND -> {
                drawCircle(Color.White, faceR * 0.18f, eyeL)
                drawCircle(Color.White, faceR * 0.18f, eyeR)
                drawCircle(Color(0xFF2A1E3C), faceR * 0.10f, eyeL)
                drawCircle(Color(0xFF2A1E3C), faceR * 0.10f, eyeR)
                drawCircle(Color.White, faceR * 0.04f, androidx.compose.ui.geometry.Offset(eyeL.x + faceR * 0.03f, eyeL.y - faceR * 0.03f))
                drawCircle(Color.White, faceR * 0.04f, androidx.compose.ui.geometry.Offset(eyeR.x + faceR * 0.03f, eyeR.y - faceR * 0.03f))
            }
            EyeStyle.DOT -> {
                drawCircle(Color(0xFF2A1E3C), faceR * 0.10f, eyeL)
                drawCircle(Color(0xFF2A1E3C), faceR * 0.10f, eyeR)
            }
            EyeStyle.WINK -> {
                // Left eye: wink (small arc). Right: dot.
                drawArc(
                    color = Color(0xFF2A1E3C),
                    startAngle = -10f,
                    sweepAngle = -160f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(eyeL.x - faceR * 0.14f, eyeL.y - faceR * 0.10f),
                    size = androidx.compose.ui.geometry.Size(faceR * 0.28f, faceR * 0.20f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = faceR * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                drawCircle(Color(0xFF2A1E3C), faceR * 0.10f, eyeR)
            }
            EyeStyle.SLEEPY -> {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = faceR * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawArc(
                    color = Color(0xFF2A1E3C),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(eyeL.x - faceR * 0.14f, eyeL.y - faceR * 0.05f),
                    size = androidx.compose.ui.geometry.Size(faceR * 0.28f, faceR * 0.14f),
                    style = stroke,
                )
                drawArc(
                    color = Color(0xFF2A1E3C),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(eyeR.x - faceR * 0.14f, eyeR.y - faceR * 0.05f),
                    size = androidx.compose.ui.geometry.Size(faceR * 0.28f, faceR * 0.14f),
                    style = stroke,
                )
            }
        }

        // Mouth.
        val mouthCenter = androidx.compose.ui.geometry.Offset(cx, cy + faceR * 0.38f)
        val inkStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = faceR * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        when (persona.mouth) {
            Mouth.SMILE -> drawArc(
                color = Color(0xFF2A1E3C),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(mouthCenter.x - faceR * 0.22f, mouthCenter.y - faceR * 0.14f),
                size = androidx.compose.ui.geometry.Size(faceR * 0.44f, faceR * 0.28f),
                style = inkStroke,
            )
            Mouth.SMIRK -> drawArc(
                color = Color(0xFF2A1E3C),
                startAngle = 10f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(mouthCenter.x - faceR * 0.10f, mouthCenter.y - faceR * 0.12f),
                size = androidx.compose.ui.geometry.Size(faceR * 0.32f, faceR * 0.22f),
                style = inkStroke,
            )
            Mouth.O -> drawCircle(Color(0xFF2A1E3C), faceR * 0.09f, mouthCenter)
            Mouth.LINE -> drawLine(
                Color(0xFF2A1E3C),
                androidx.compose.ui.geometry.Offset(mouthCenter.x - faceR * 0.15f, mouthCenter.y),
                androidx.compose.ui.geometry.Offset(mouthCenter.x + faceR * 0.15f, mouthCenter.y),
                strokeWidth = faceR * 0.08f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }

        // Bolt accessory (Juno the robot).
        if (persona.accessory == Accessory.BOLT) {
            val path = androidx.compose.ui.graphics.Path().apply {
                val bx = cx + faceR * 0.1f
                val by = cy - faceR * 0.95f
                moveTo(bx, by)
                lineTo(bx - faceR * 0.12f, by + faceR * 0.25f)
                lineTo(bx + faceR * 0.02f, by + faceR * 0.25f)
                lineTo(bx - faceR * 0.08f, by + faceR * 0.55f)
                lineTo(bx + faceR * 0.18f, by + faceR * 0.20f)
                lineTo(bx + faceR * 0.02f, by + faceR * 0.20f)
                lineTo(bx + faceR * 0.14f, by)
                close()
            }
            drawPath(path, Color(0xFFFFD84D))
        }
    }
}

/**
 * Opponent's hand rendered as fanned face-down mini cards. Shows up to
 * [maxVisible] cards spread at small rotation offsets; for larger hands a
 * `+N` chip appears at the right edge so the row stays readable.
 */
@Composable
internal fun FannedOpponentHand(
    handSize: Int,
    cardWidth: Dp = 20.dp,
    cardHeight: Dp = 28.dp,
    maxVisible: Int = 6,
) {
    if (handSize <= 0) {
        Text(
            "empty",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val visible = minOf(handSize, maxVisible)
    val overflow = handSize - visible

    // Total horizontal width scales with visible count. Cards overlap by ~60%
    // so a 6-card fan is roughly 3 card-widths wide.
    val stepPx = cardWidth * 0.45f
    val rowWidth = cardWidth + stepPx * (visible - 1)

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(rowWidth)
                .height(cardHeight + 4.dp),
        ) {
            for (i in 0 until visible) {
                val centerOffset = (i - (visible - 1) / 2f)
                val rotation = centerOffset * 6f // degrees
                val xOffset = stepPx * i
                Box(
                    modifier = Modifier
                        .padding(start = xOffset)
                        .rotate(rotation),
                ) {
                    MiniCardBack(width = cardWidth, height = cardHeight)
                }
            }
        }
        if (overflow > 0) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Tiny face-down card used in the opponent fan. A simplified CardView — we
 * skip the inner ellipse pattern at this scale since it reads as noise.
 */
@Composable
private fun MiniCardBack(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF5A3F74),
                    1f to Color(0xFF2E1F42),
                )
            ),
    )
}

/**
 * Three-dot "thinking" bubble. Dots cycle through three states of opacity
 * so it animates even when the underlying state isn't changing.
 */
@Composable
internal fun ThinkingBubble() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "thinking-phase",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                val active = (phase.toInt() % 3) == i
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .alpha(if (active) 1f else 0.25f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private val AVATAR_PALETTE = listOf(
    Color(0xFFFF7A8A), // coral
    Color(0xFF7DC6F5), // sky
    Color(0xFF6BDCA6), // mint
    Color(0xFFFFB547), // marigold
    Color(0xFFCBB8E8), // lavender
    Color(0xFFFF9F6B), // peach
    Color(0xFF77D4C1), // teal
    Color(0xFFEAC46F), // sand
)

private fun avatarColorFor(id: String): Color {
    // Stable pseudo-hash so the same id always lands on the same color.
    val seed = id.fold(0) { acc, c -> acc * 31 + c.code }
    return AVATAR_PALETTE[abs(seed) % AVATAR_PALETTE.size]
}

private fun Color.lighten(f: Float): Color = Color(
    red = (red + (1f - red) * f).coerceIn(0f, 1f),
    green = (green + (1f - green) * f).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * f).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.darken(f: Float): Color = Color(
    red = (red * (1f - f)).coerceIn(0f, 1f),
    green = (green * (1f - f)).coerceIn(0f, 1f),
    blue = (blue * (1f - f)).coerceIn(0f, 1f),
    alpha = alpha,
)
