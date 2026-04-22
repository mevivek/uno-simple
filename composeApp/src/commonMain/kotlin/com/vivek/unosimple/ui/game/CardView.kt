package com.vivek.unosimple.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single UNO card rendered in the classic Mattel visual language:
 *
 * 1. Outer white bezel (~5dp) with rounded corners — the "card edge".
 * 2. Flat saturated color inside (no gradient).
 * 3. Tilted white oval centered, carrying the big glyph.
 * 4. Small corner glyphs (top-left + bottom-right, tilted with the oval) —
 *    the little digits / icons that make a real UNO card scannable when
 *    fanned in a hand.
 * 5. Card back: red (#EF3B32) with a yellow-outlined white oval containing
 *    a red "UNO" wordmark in italic bold (mimicking the Mattel back face).
 *
 * @param card the card to render; `null` renders a face-down card back.
 * @param enabled dims the card when false (used to signal illegal plays).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardView(
    card: Card?,
    modifier: Modifier = Modifier,
    width: Dp = CARD_WIDTH,
    height: Dp = CARD_HEIGHT,
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    /** Invoked on long-press (typically to show an enlarged preview).
     *  Fires even for disabled / illegal cards so the player can study
     *  every card in their hand before committing. */
    onLongPress: (() -> Unit)? = null,
) {
    val faceUp = card != null
    val faceColor: Color = when {
        !faceUp -> CARD_BACK_RED
        card.color == null -> WILD_BACKGROUND
        else -> cardFaceColor(card.color!!)
    }

    val outerShape = RoundedCornerShape(CORNER_RADIUS)
    val clickModifier = when {
        onLongPress != null -> Modifier.combinedClickable(
            onClick = { if (enabled && onClick != null) onClick() },
            onLongClick = onLongPress,
        )
        onClick != null && enabled -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .size(width = width, height = height)
            // Outer drop shadow — dark, tight, so the card pops off the red felt.
            .drawBehind {
                drawRoundRect(
                    color = Color(0x55000000),
                    topLeft = Offset(0f, 3.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                )
            }
            // White bezel.
            .clip(outerShape)
            .background(Color.White, outerShape)
            // Disabled cards: keep at 0.72 alpha (was 0.4 — too dim to read)
            // and layer a dark tint on top in the body below.
            .alpha(if (enabled) 1f else 0.72f)
            .then(tagModifier)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        // Inner face panel — solid saturated color, smaller than the outer
        // card so the white bezel shows around it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val inset = BEZEL_INSET.toPx()
                    drawRoundRect(
                        color = faceColor,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius((CORNER_RADIUS - BEZEL_INSET).toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (faceUp) {
                CardFace(card!!)
            } else {
                CardBack()
            }
            // Dark wash on disabled cards — keeps the face color identifiable
            // (vs the previous 0.4-alpha wash which made everything ghost-
            // white) but clearly signals "not playable".
            if (!enabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val inset = BEZEL_INSET.toPx()
                            drawRoundRect(
                                color = Color(0x3A000000),
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2, size.height - inset * 2),
                                cornerRadius = CornerRadius((CORNER_RADIUS - BEZEL_INSET).toPx()),
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun CardFace(card: Card) {
    // Tilted inner white oval — the classic UNO motif behind the glyph.
    TiltedInnerOval()
    // Big center glyph.
    CenterGlyph(card)
    // Small corner glyphs — top-left + bottom-right (rotated 180).
    CornerGlyph(card, alignment = Alignment.TopStart, rotation = 0f)
    CornerGlyph(card, alignment = Alignment.BottomEnd, rotation = 180f)
}

@Composable
private fun TiltedInnerOval() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotateDraw(degrees = -22f) {
            val padX = size.width * 0.08f
            val padY = size.height * 0.22f
            drawOval(
                color = Color.White,
                topLeft = Offset(padX, padY),
                size = Size(size.width - padX * 2, size.height - padY * 2),
            )
        }
    }
}

@Composable
private fun CenterGlyph(card: Card) {
    // Big centered number / glyph. Sits on top of the tilted white oval, so
    // it renders in the face color (red / yellow / green / blue) rather
    // than white. The text is lightly tilted to match the oval rotation.
    Box(modifier = Modifier.fillMaxSize().rotate(-10f), contentAlignment = Alignment.Center) {
        when (card) {
            is NumberCard -> Text(
                text = card.number.toString(),
                color = cardInkColor(card.color),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            is SkipCard -> SkipGlyph(color = cardInkColor(card.color), sizeDp = 40.dp)
            is ReverseCard -> ReverseGlyph(color = cardInkColor(card.color), sizeDp = 38.dp)
            is DrawTwoCard -> Text(
                text = "+2",
                color = cardInkColor(card.color),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            WildCard -> WildPinwheel(sizeDp = 48.dp)
            WildDrawFourCard -> {
                // "+4" tilted, with a mini pinwheel behind it.
                Box(contentAlignment = Alignment.Center) {
                    WildPinwheel(sizeDp = 52.dp)
                    Text(
                        text = "+4",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}

@Composable
private fun CornerGlyph(card: Card, alignment: Alignment, rotation: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .rotate(rotation)
                .fillMaxSize(),
            contentAlignment = if (rotation == 0f) Alignment.TopStart else Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 4.dp,
                ),
            ) {
                when (card) {
                    is NumberCard -> Text(
                        text = card.number.toString(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    is SkipCard -> SkipGlyph(color = Color.White, sizeDp = 14.dp)
                    is ReverseCard -> ReverseGlyph(color = Color.White, sizeDp = 14.dp)
                    is DrawTwoCard -> Text(
                        "+2",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    WildCard -> Text(
                        "W",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    WildDrawFourCard -> Text(
                        "+4",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Glyphs
// ---------------------------------------------------------------------------

@Composable
private fun SkipGlyph(color: Color, sizeDp: Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val stroke = size.minDimension * 0.12f
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val inset = radius * 0.25f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = color,
            start = Offset(cx - radius + inset, cy + radius - inset),
            end = Offset(cx + radius - inset, cy - radius + inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Reverse glyph — two offset diagonal arrows pointing in opposite ways,
 * echoing the Mattel Reverse card. Top arrow goes from lower-left to
 * upper-right with arrowhead at the top; bottom arrow mirrors it.
 */
@Composable
private fun ReverseGlyph(color: Color, sizeDp: Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val s = size.minDimension
        val stroke = s * 0.12f
        val head = s * 0.16f
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Geometry: each arrow is a short diagonal line. The two arrows are
        // parallel to each other but reversed in direction, laid slightly
        // apart like the Mattel symbol.
        // Offset perpendicular to the arrow axis so they sit side-by-side.
        val arrowLen = s * 0.42f
        val halfLen = arrowLen
        // Arrow A: from lower-left to upper-right (tip at top-right).
        val aStart = Offset(cx - halfLen * 0.85f, cy + halfLen * 0.25f)
        val aEnd = Offset(cx + halfLen * 0.25f, cy - halfLen * 0.85f)
        drawLine(color, aStart, aEnd, strokeWidth = stroke, cap = StrokeCap.Round)
        // Arrowhead at aEnd pointing along direction (aEnd - aStart).
        val aDirX = (aEnd.x - aStart.x)
        val aDirY = (aEnd.y - aStart.y)
        val aLen = kotlin.math.sqrt(aDirX * aDirX + aDirY * aDirY)
        val aUx = aDirX / aLen
        val aUy = aDirY / aLen
        // Perpendicular, for the "V" of the arrowhead.
        val aPx = -aUy
        val aPy = aUx
        val aBaseX = aEnd.x - head * aUx
        val aBaseY = aEnd.y - head * aUy
        drawLine(color, aEnd, Offset(aBaseX + head * 0.6f * aPx, aBaseY + head * 0.6f * aPy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, aEnd, Offset(aBaseX - head * 0.6f * aPx, aBaseY - head * 0.6f * aPy), strokeWidth = stroke, cap = StrokeCap.Round)

        // Arrow B: from upper-right to lower-left (tip at bottom-left).
        val bStart = Offset(cx + halfLen * 0.85f, cy - halfLen * 0.25f)
        val bEnd = Offset(cx - halfLen * 0.25f, cy + halfLen * 0.85f)
        drawLine(color, bStart, bEnd, strokeWidth = stroke, cap = StrokeCap.Round)
        val bDirX = (bEnd.x - bStart.x)
        val bDirY = (bEnd.y - bStart.y)
        val bLen = kotlin.math.sqrt(bDirX * bDirX + bDirY * bDirY)
        val bUx = bDirX / bLen
        val bUy = bDirY / bLen
        val bPx = -bUy
        val bPy = bUx
        val bBaseX = bEnd.x - head * bUx
        val bBaseY = bEnd.y - head * bUy
        drawLine(color, bEnd, Offset(bBaseX + head * 0.6f * bPx, bBaseY + head * 0.6f * bPy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, bEnd, Offset(bBaseX - head * 0.6f * bPx, bBaseY - head * 0.6f * bPy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * Wild pinwheel — four colored quadrants (red / yellow / green / blue) laid
 * out inside a circle, mimicking Mattel's wild card. Used as the center
 * glyph for [WildCard] and as the backdrop for [WildDrawFourCard].
 */
@Composable
private fun WildPinwheel(sizeDp: Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f
        // Four quadrants. Start angles go red-top, yellow-right, green-bottom, blue-left.
        val quadrants = listOf(
            180f to Color(0xFFEF3B32), // top-left = red
            270f to Color(0xFFFFCC00), // top-right = yellow
            0f to Color(0xFF49A84C),   // bottom-right = green
            90f to Color(0xFF1D84C4),  // bottom-left = blue
        )
        quadrants.forEach { (start, c) ->
            drawArc(
                color = c,
                startAngle = start,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
            )
        }
        // Thin white divider cross.
        drawLine(Color.White, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = r * 0.1f)
        drawLine(Color.White, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = r * 0.1f)
    }
}

/**
 * Card back — Mattel red with a tilted yellow-outlined white oval framing
 * the italic "UNO" wordmark. Matches the classic face-down UNO card look.
 */
@Composable
private fun CardBack() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotateDraw(degrees = -22f) {
            val padX = size.width * 0.08f
            val padY = size.height * 0.22f
            val ovalTL = Offset(padX, padY)
            val ovalSize = Size(size.width - padX * 2, size.height - padY * 2)
            // Yellow outer ring.
            drawOval(
                color = Color(0xFFFFCC00),
                topLeft = Offset(ovalTL.x - 3f, ovalTL.y - 3f),
                size = Size(ovalSize.width + 6f, ovalSize.height + 6f),
            )
            // White fill.
            drawOval(color = Color.White, topLeft = ovalTL, size = ovalSize)
        }
    }
    // Italic "UNO" wordmark in red, tilted to match the oval.
    Box(modifier = Modifier.fillMaxSize().rotate(-22f), contentAlignment = Alignment.Center) {
        Text(
            text = "UNO",
            color = Color(0xFFEF3B32),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
    }
}

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------

/** Authentic-UNO face colors (Mattel-ish). */
internal fun cardFaceColor(color: CardColor): Color = when (color) {
    CardColor.RED -> Color(0xFFEF3B32)
    CardColor.YELLOW -> Color(0xFFFFCC00)
    CardColor.GREEN -> Color(0xFF49A84C)
    CardColor.BLUE -> Color(0xFF1D84C4)
}

/** Ink color for the center-oval glyph on number cards — same hue as the face. */
internal fun cardInkColor(color: CardColor?): Color = when (color) {
    null -> Color(0xFFEF3B32) // fallback (wild: red)
    else -> cardFaceColor(color)
}

/** Legacy alias kept for callers that already import cardColorToCompose. */
internal fun cardColorToCompose(color: CardColor): Color = cardFaceColor(color)

// ---------------------------------------------------------------------------
// Dimensions
// ---------------------------------------------------------------------------

internal val CARD_WIDTH: Dp = 56.dp
internal val CARD_HEIGHT: Dp = 84.dp
private val CORNER_RADIUS: Dp = 10.dp
private val BEZEL_INSET: Dp = 4.dp

private val CARD_BACK_RED: Color = Color(0xFFEF3B32)
private val WILD_BACKGROUND: Color = Color(0xFF111111) // near-black like real Wild cards
