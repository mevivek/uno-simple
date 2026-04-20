package com.vivek.unosimple.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.ui.theme.LocalClayTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single UNO card tile, styled as a claymorph "chip":
 *
 * 1. Vertical gradient base — a lighter shade at the top fades into the
 *    main color, faking an inset bevel.
 * 2. Soft drop shadow behind the rounded rectangle so cards read as
 *    layered on top of the cream table.
 * 3. Inner tilted ellipse — the classic playing-card motif, carried from
 *    the previous design.
 * 4. Bold glyph on top; action icons drawn with Canvas because Unicode
 *    glyph support on Skia's web font is unreliable.
 *
 * @param card the card to render; `null` renders a face-down card back.
 * @param enabled dims the card when false (used to signal illegal plays).
 */
@Composable
fun CardView(
    card: Card?,
    modifier: Modifier = Modifier,
    width: Dp = CARD_WIDTH,
    height: Dp = CARD_HEIGHT,
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val faceUp = card != null
    val baseColor: Color = when {
        !faceUp -> CARD_BACK_COLOR
        card.color == null -> WILD_BACKGROUND
        else -> cardColorToCompose(card.color!!)
    }
    val topShade = baseColor.lighten(0.22f)
    val bottomShade = baseColor.darken(0.12f)
    val gradient = Brush.verticalGradient(
        0f to topShade,
        0.55f to baseColor,
        1f to bottomShade,
    )

    val shape = RoundedCornerShape(CORNER_RADIUS)
    val shadowColor = LocalClayTokens.current.shadowColor
    val highlight = LocalClayTokens.current.highlightColor

    val clickModifier = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .drawBehind {
                // Soft drop shadow below + slightly right of the card.
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                )
            }
            .clip(shape)
            .background(gradient, shape)
            .drawBehind {
                // Top inner highlight — gives the "polished clay" sheen.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to highlight,
                        0.45f to Color.Transparent,
                    ),
                    cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            .alpha(if (enabled) 1f else 0.4f)
            .then(tagModifier)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (faceUp) {
            InnerEllipse()
            CardFaceContent(card!!)
        } else {
            CardBackPattern()
        }
    }
}

@Composable
private fun CardFaceContent(card: Card) {
    when (card) {
        is NumberCard -> Text(
            text = card.number.toString(),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        is SkipCard -> SkipIcon()
        is ReverseCard -> ReverseIcon()
        is DrawTwoCard -> Text(
            text = "+2",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        WildCard -> WildStarIcon()
        WildDrawFourCard -> Text(
            text = "+4",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Tilted translucent-white ellipse behind the glyph — the classic
 * playing-card motif. Slightly more opaque than before so it reads well
 * against the pastel gradient.
 */
@Composable
private fun InnerEllipse() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotate(degrees = -30f) {
            val padX = size.width * 0.08f
            val padY = size.height * 0.20f
            val topLeft = Offset(padX, padY)
            drawOval(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = topLeft,
                size = Size(size.width - padX * 2, size.height - padY * 2),
            )
        }
    }
}

@Composable
private fun SkipIcon() {
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = 3.dp.toPx()
        val pad = stroke / 2f
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val inset = radius * 0.25f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = Color.White,
            start = Offset(cx - radius + inset + pad, cy - radius + inset + pad),
            end = Offset(cx + radius - inset - pad, cy + radius - inset - pad),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ReverseIcon() {
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = 3.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f - stroke

        val topPath = Path().apply {
            moveTo(cx - r, cy)
            quadraticTo(cx, cy - r * 1.4f, cx + r, cy)
        }
        drawPath(topPath, color = Color.White, style = Stroke(width = stroke, cap = StrokeCap.Round))
        val headSize = r * 0.35f
        drawLine(
            Color.White,
            Offset(cx + r, cy),
            Offset(cx + r - headSize, cy - headSize * 0.8f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(cx + r, cy),
            Offset(cx + r - headSize, cy + headSize * 0.8f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )

        val bottomPath = Path().apply {
            moveTo(cx + r, cy + stroke * 1.4f)
            quadraticTo(cx, cy + r * 1.4f + stroke * 1.4f, cx - r, cy + stroke * 1.4f)
        }
        drawPath(bottomPath, color = Color.White, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(
            Color.White,
            Offset(cx - r, cy + stroke * 1.4f),
            Offset(cx - r + headSize, cy + stroke * 1.4f - headSize * 0.8f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(cx - r, cy + stroke * 1.4f),
            Offset(cx - r + headSize, cy + stroke * 1.4f + headSize * 0.8f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun WildStarIcon() {
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension / 2f * 0.95f
        val innerR = outerR * 0.4f
        val points = 5

        val path = Path().apply {
            for (i in 0 until points * 2) {
                val radius = if (i % 2 == 0) outerR else innerR
                val angleRad = -PI / 2 + i * PI / points
                val x = cx + (radius * cos(angleRad)).toFloat()
                val y = cy + (radius * sin(angleRad)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path, color = Color.White)
    }
}

/**
 * Card back pattern: a tilted "UNO" oval with the four color-corner
 * quadrants showing, echoing the classic face-down card look. All strokes
 * are muted against the plum background so the pattern feels designed,
 * not noisy.
 */
@Composable
private fun CardBackPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotate(degrees = -30f) {
            val padX = size.width * 0.10f
            val padY = size.height * 0.22f
            val ovalTopLeft = Offset(padX, padY)
            val ovalSize = Size(size.width - padX * 2, size.height - padY * 2)
            // Filled oval (bright center).
            drawOval(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = ovalTopLeft,
                size = ovalSize,
            )
            // Outline ring just outside the fill.
            drawOval(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(ovalTopLeft.x - 3f, ovalTopLeft.y - 3f),
                size = Size(ovalSize.width + 6f, ovalSize.height + 6f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        // Small dot quadrants at each corner to echo the four card colors.
        val corner = size.minDimension * 0.10f
        val dotR = size.minDimension * 0.035f
        drawCircle(CARD_BACK_CORAL, dotR, Offset(corner, corner))
        drawCircle(CARD_BACK_MARIGOLD, dotR, Offset(size.width - corner, corner))
        drawCircle(CARD_BACK_MINT, dotR, Offset(corner, size.height - corner))
        drawCircle(CARD_BACK_SKY, dotR, Offset(size.width - corner, size.height - corner))
    }
}

internal fun cardColorToCompose(color: CardColor): Color = when (color) {
    // Pastel palette matched to the claymorph theme tokens.
    CardColor.RED -> Color(0xFFFF6B7D)   // coral
    CardColor.YELLOW -> Color(0xFFFFB547) // marigold
    CardColor.GREEN -> Color(0xFF5FCF95)  // mint
    CardColor.BLUE -> Color(0xFF5AAEE8)   // sky
}

// Small helpers to shade colors without pulling in a library.
private fun Color.lighten(fraction: Float): Color = Color(
    red = (red + (1f - red) * fraction).coerceIn(0f, 1f),
    green = (green + (1f - green) * fraction).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * fraction).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.darken(fraction: Float): Color = Color(
    red = (red * (1f - fraction)).coerceIn(0f, 1f),
    green = (green * (1f - fraction)).coerceIn(0f, 1f),
    blue = (blue * (1f - fraction)).coerceIn(0f, 1f),
    alpha = alpha,
)

internal val CARD_WIDTH: Dp = 56.dp
internal val CARD_HEIGHT: Dp = 84.dp
private val ICON_SIZE: Dp = 36.dp
private val CORNER_RADIUS: Dp = 14.dp
private val CARD_BACK_COLOR: Color = Color(0xFF3E2A52) // plum
private val WILD_BACKGROUND: Color = Color(0xFF2C1D3E)

private val CARD_BACK_CORAL = Color(0xFFFF6B7D)
private val CARD_BACK_MARIGOLD = Color(0xFFFFB547)
private val CARD_BACK_MINT = Color(0xFF5FCF95)
private val CARD_BACK_SKY = Color(0xFF5AAEE8)
