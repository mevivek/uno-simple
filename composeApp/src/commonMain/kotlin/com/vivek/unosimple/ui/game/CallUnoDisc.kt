package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.ui.TestTags

/**
 * Big floating "CALL UNO" disc — the bottom-right red pill that appears when
 * the human holds exactly two cards and hasn't declared yet. Lifted straight
 * from the reference mobile UNO app; this is the one-button moment where
 * the player has to react, so it deserves its own persistent overlay rather
 * than being tucked inline into the hand row.
 *
 * States:
 * - eligible + not declared → vivid red disc, pulsing, tappable
 * - eligible + declared     → muted disc, "DECLARED", not tappable
 * - not eligible            → AnimatedVisibility fades the whole thing out
 */
@Composable
internal fun CallUnoDisc(
    visible: Boolean,
    declared: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.6f, animationSpec = tween(260, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.8f, animationSpec = tween(160)),
        modifier = modifier,
    ) {
        CallUnoDiscBody(declared = declared, onTap = onTap)
    }
}

@Composable
private fun CallUnoDiscBody(declared: Boolean, onTap: () -> Unit) {
    // Pulse while the declaration is still open; frozen once declared.
    val pulse by rememberInfiniteTransition(label = "call-uno-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "call-uno-scale",
    )
    val scale = if (declared) 1f else pulse

    // Color spec — red (#E63946) fading into a deeper red (#B92036). Matches
    // the reference "CALL UNO" disc on the Mattel app.
    val redTop = Color(0xFFFF4A5A)
    val redBottom = Color(0xFFB92036)
    val ring = Color(0xFFFFD84D) // amber ring around the disc
    val textColor = Color.White

    val gradient = if (declared) {
        Brush.verticalGradient(0f to Color(0xFF777777), 1f to Color(0xFF4B4B4B))
    } else {
        Brush.verticalGradient(0f to redTop, 1f to redBottom)
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(90.dp)
            .clip(CircleShape)
            .drawBehind {
                // Soft red halo behind the disc so it feels like it's glowing.
                drawCircle(
                    color = if (declared) Color(0x33000000) else redBottom.copy(alpha = 0.4f),
                    radius = size.minDimension / 2f + 12.dp.toPx(),
                )
            }
            .background(gradient, CircleShape)
            .drawBehind {
                // Amber ring on the outer edge.
                drawCircle(
                    color = if (declared) Color.Transparent else ring,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx()),
                )
                // White inner-top highlight sheen for the glossy disc feel.
                if (!declared) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.1f),
                        size = Size(size.width * 0.6f, size.height * 0.35f),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
            }
            .clickable(enabled = !declared, onClick = onTap)
            .testTag(TestTags.UNO_BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        if (declared) {
            Text(
                text = "OK!",
                color = textColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        } else {
            // Two stacked lines — "CALL" (small) over "UNO!" (huge).
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(78.dp)) {
                    // Subtle tilted ribbon behind the text for "casino sign" feel.
                    rotateDraw(degrees = -6f) {
                        drawRoundRect(
                            color = Color(0xFFFFD84D).copy(alpha = 0.25f),
                            topLeft = Offset(size.width * 0.08f, size.height * 0.34f),
                            size = Size(size.width * 0.84f, size.height * 0.32f),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                        )
                    }
                }
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "CALL",
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "UNO!",
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

