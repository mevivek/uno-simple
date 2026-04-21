package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Self-emote system — tap the reaction button, a small palette of six
 * Canvas-drawn icons slides up, pick one and it pops as a bubble near
 * the button. Fades out after ~2.2s.
 *
 * We draw the icons ourselves instead of using Unicode emoji because
 * Skiko's bundled Wasm font has no emoji glyph table — `😊` et al render
 * as tofu boxes in the browser. Same approach that fixed the Skip/Reverse
 * action-card glyphs.
 *
 * Not yet broadcast in multiplayer; [onPick] fires locally. Phase 6 will
 * wire a `sendEmote` channel through GameSyncService so everyone in the
 * room sees the sender's bubble at their seat.
 */

internal enum class Reaction {
    SMILE,   // general-good-vibes / "nice"
    SHOCK,   // "wow"
    CLAP,    // "gg"
    FIRE,    // "heat" / on-a-roll
    THINK,   // "hmm" / eye-roll
    PARTY,   // "yay" / celebrate
}

internal val REACTIONS: List<Reaction> = Reaction.entries.toList()

/**
 * Bottom/top-left corner holding the palette toggle + the active bubble.
 * Caller positions this; the composable itself wraps its content.
 *
 * @param onBroadcast callback invoked when the user picks a reaction — in
 *   online + hotseat contexts this forwards the emote to GameSyncService;
 *   in solo-vs-bots it's a no-op (default).
 */
@Composable
internal fun EmoteCorner(
    modifier: Modifier = Modifier,
    onBroadcast: (Reaction) -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    var active: Reaction? by remember { mutableStateOf(null) }

    LaunchedEffect(active) {
        if (active != null) {
            delay(2200)
            active = null
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        // Palette — slides out to the LEFT of the toggle (button is pinned
        // to the right edge of the screen).
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(140)) + slideInHorizontally(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                initialOffsetX = { it / 2 },
            ),
            exit = fadeOut(tween(140)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 52.dp),
        ) {
            EmotePalette(onPick = { r ->
                active = r
                open = false
                onBroadcast(r)
            })
        }

        // Active bubble — also pops to the LEFT of the toggle so the user
        // can still see their last emote after picking it.
        AnimatedVisibility(
            visible = active != null,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.5f),
            exit = fadeOut(tween(240)) + scaleOut(targetScale = 1.3f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 52.dp),
        ) {
            active?.let { ReactionBubble(it) }
        }

        // 😊-style toggle itself — small round button with a smile icon.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { open = !open }
                .testTag("emote.toggle"),
            contentAlignment = Alignment.Center,
        ) {
            ReactionIcon(reaction = Reaction.SMILE, sizeDp = 26.dp)
        }
    }
}

@Composable
private fun EmotePalette(onPick: (Reaction) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        REACTIONS.forEach { r ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onPick(r) }
                    .testTag("emote.pick.${r.name}"),
                contentAlignment = Alignment.Center,
            ) {
                ReactionIcon(reaction = r, sizeDp = 28.dp)
            }
        }
    }
}

@Composable
private fun ReactionBubble(reaction: Reaction) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReactionIcon(reaction = reaction, sizeDp = 32.dp)
    }
}

// ---------------------------------------------------------------------------
// Canvas-drawn reaction icons.
// ---------------------------------------------------------------------------

/** Router — draws the requested [reaction] into a [sizeDp] square box. */
@Composable
internal fun ReactionIcon(reaction: Reaction, sizeDp: Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        when (reaction) {
            Reaction.SMILE -> drawSmileFace()
            Reaction.SHOCK -> drawShockFace()
            Reaction.CLAP -> drawClap()
            Reaction.FIRE -> drawFire()
            Reaction.THINK -> drawThinkFace()
            Reaction.PARTY -> drawPartyFace()
        }
    }
}

private fun DrawScope.drawYellowDisc() {
    val r = size.minDimension / 2f
    drawCircle(
        color = Color(0xFFFFCD3C),
        radius = r,
        center = Offset(size.width / 2f, size.height / 2f),
    )
}

private fun DrawScope.drawSmileFace() {
    drawYellowDisc()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val ink = Color(0xFF2A1E3C)
    // Eyes (two arc-smiles for happy closed eyes).
    val stroke = r * 0.12f
    listOf(-0.35f, 0.35f).forEach { xOff ->
        drawArc(
            color = ink,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + xOff * r - r * 0.22f, cy - r * 0.18f),
            size = Size(r * 0.44f, r * 0.3f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
    // Smile arc.
    drawArc(
        color = ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.45f, cy),
        size = Size(r * 0.9f, r * 0.6f),
        style = Stroke(width = stroke * 1.2f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawShockFace() {
    drawYellowDisc()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val ink = Color(0xFF2A1E3C)
    // Wide round eyes.
    listOf(-0.38f, 0.38f).forEach { xOff ->
        drawCircle(
            color = ink,
            radius = r * 0.13f,
            center = Offset(cx + xOff * r, cy - r * 0.1f),
        )
    }
    // "O" mouth.
    drawCircle(
        color = ink,
        radius = r * 0.16f,
        center = Offset(cx, cy + r * 0.35f),
        style = Stroke(width = r * 0.08f),
    )
}

private fun DrawScope.drawThinkFace() {
    drawYellowDisc()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val ink = Color(0xFF2A1E3C)
    // Eyes looking up (small circles near top).
    listOf(-0.38f, 0.38f).forEach { xOff ->
        drawCircle(
            color = ink,
            radius = r * 0.1f,
            center = Offset(cx + xOff * r, cy - r * 0.25f),
        )
    }
    // Flat "hmm" mouth.
    drawLine(
        color = ink,
        start = Offset(cx - r * 0.3f, cy + r * 0.3f),
        end = Offset(cx + r * 0.3f, cy + r * 0.3f),
        strokeWidth = r * 0.12f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPartyFace() {
    drawYellowDisc()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val ink = Color(0xFF2A1E3C)
    // Cone hat — triangle on top-left, dots for confetti on the right.
    val hat = Path().apply {
        moveTo(cx - r * 0.7f, cy - r * 0.6f)
        lineTo(cx - r * 0.2f, cy - r * 1.2f)
        lineTo(cx - r * 0.1f, cy - r * 0.5f)
        close()
    }
    drawPath(hat, color = Color(0xFFFF5168))
    // Pom-pom on top of the hat.
    drawCircle(Color(0xFFFFB53B), r * 0.12f, Offset(cx - r * 0.2f, cy - r * 1.2f))
    // Smiling eyes (closed arcs).
    val stroke = r * 0.1f
    listOf(-0.3f, 0.38f).forEach { xOff ->
        drawArc(
            color = ink,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + xOff * r - r * 0.18f, cy - r * 0.05f),
            size = Size(r * 0.36f, r * 0.25f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
    // Open smile.
    drawArc(
        color = ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.35f, cy + r * 0.15f),
        size = Size(r * 0.7f, r * 0.5f),
        style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round),
    )
    // Confetti specks — right side.
    val confetti = listOf(
        Offset(cx + r * 0.85f, cy - r * 0.4f) to Color(0xFF2EE89B),
        Offset(cx + r * 0.65f, cy - r * 0.9f) to Color(0xFF49B6FF),
        Offset(cx + r * 0.95f, cy) to Color(0xFFA06BFF),
    )
    confetti.forEach { (pos, c) -> drawCircle(c, r * 0.08f, pos) }
}

private fun DrawScope.drawClap() {
    // Two overlapping yellow palms tilted toward each other.
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val palmColor = Color(0xFFFFCD3C)
    val edge = Color(0xFFBF8A1B)

    // Left palm — rounded rectangle rotated -20°.
    rotated(-20f, Offset(cx, cy)) {
        drawRoundRect(
            color = palmColor,
            topLeft = Offset(cx - r * 0.9f, cy - r * 0.55f),
            size = Size(r * 0.9f, r * 1.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.25f),
        )
    }
    rotated(20f, Offset(cx, cy)) {
        drawRoundRect(
            color = palmColor,
            topLeft = Offset(cx, cy - r * 0.55f),
            size = Size(r * 0.9f, r * 1.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.25f),
        )
    }
    // Motion lines on the outside.
    val stroke = r * 0.08f
    drawLine(edge, Offset(cx - r * 1.1f, cy - r * 0.3f), Offset(cx - r * 0.7f, cy - r * 0.1f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(edge, Offset(cx + r * 1.1f, cy - r * 0.3f), Offset(cx + r * 0.7f, cy - r * 0.1f), strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun DrawScope.drawFire() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    // Outer flame — orange.
    val outer = Path().apply {
        moveTo(cx, cy - r * 0.95f)
        cubicTo(
            cx + r * 0.7f, cy - r * 0.4f,
            cx + r * 0.8f, cy + r * 0.4f,
            cx + r * 0.1f, cy + r * 0.95f,
        )
        cubicTo(
            cx + r * 0.4f, cy + r * 0.5f,
            cx + r * 0.1f, cy + r * 0.2f,
            cx, cy + r * 0.4f,
        )
        cubicTo(
            cx - r * 0.1f, cy + r * 0.2f,
            cx - r * 0.4f, cy + r * 0.5f,
            cx - r * 0.1f, cy + r * 0.95f,
        )
        cubicTo(
            cx - r * 0.8f, cy + r * 0.4f,
            cx - r * 0.7f, cy - r * 0.4f,
            cx, cy - r * 0.95f,
        )
        close()
    }
    drawPath(outer, color = Color(0xFFFF6A2E))
    // Inner flame — yellow.
    val inner = Path().apply {
        moveTo(cx, cy - r * 0.5f)
        cubicTo(
            cx + r * 0.4f, cy - r * 0.1f,
            cx + r * 0.45f, cy + r * 0.35f,
            cx, cy + r * 0.7f,
        )
        cubicTo(
            cx - r * 0.45f, cy + r * 0.35f,
            cx - r * 0.4f, cy - r * 0.1f,
            cx, cy - r * 0.5f,
        )
        close()
    }
    drawPath(inner, color = Color(0xFFFFCD3C))
}

/** Small helper so we don't spell out `androidx.compose.ui.graphics.drawscope.rotate` each time. */
private inline fun DrawScope.rotated(
    degrees: Float,
    pivot: Offset,
    crossinline block: DrawScope.() -> Unit,
) {
    rotateDraw(degrees, pivot) { block() }
}
