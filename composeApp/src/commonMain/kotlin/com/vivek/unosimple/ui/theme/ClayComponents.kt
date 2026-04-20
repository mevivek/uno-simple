package com.vivek.unosimple.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dark "arcade panel" surface — raised rectangle on the obsidian felt with a
 * tight drop shadow, a thin top-edge rim highlight, and a 1px stroke so the
 * silhouette reads crisp against the dark background. Replaces the former
 * claymorph cream tile; name preserved for backwards compat with callers.
 */
@Composable
fun ClaySurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    cornerRadius: Dp = LocalClayTokens.current.corner,
    elevation: Dp = LocalClayTokens.current.elevation,
    content: @Composable () -> Unit,
) {
    val tokens = LocalClayTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .drawBehind {
                // Tight drop shadow — the dark theme wants a denser shadow
                // than the claymorph cream ever did.
                val offsetY = elevation.toPx() * 0.55f
                val spread = elevation.toPx() * 0.25f
                drawRoundRect(
                    color = tokens.shadowColor,
                    topLeft = Offset(-spread * 0.5f, offsetY),
                    size = Size(size.width + spread, size.height + spread * 0.8f),
                    cornerRadius = CornerRadius(cornerRadius.toPx() + spread * 0.5f),
                )
            }
            .clip(shape)
            .background(color, shape)
            .border(1.dp, tokens.strokeColor, shape)
            .drawBehind {
                // Top rim highlight — a thin cool-white gradient at the top
                // edge that gives panels the "rim-lit" premium CCG feel.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to tokens.highlightColor,
                        0.35f to Color.Transparent,
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Primary CTA button — amber-filled gold slab with a subtle top-highlight
 * sheen and a scale-down press spring. Drops in scale + shadow on press for
 * tactile feedback. Use for the one primary action per screen; secondary
 * actions belong in [GhostButton] or the caller's own outlined style.
 */
@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Dp = LocalClayTokens.current.corner,
    elevation: Dp = LocalClayTokens.current.elevation,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "btn-press",
    )
    val effectiveColor = if (enabled) color else color.copy(alpha = 0.35f)
    val effectiveContent = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        ClaySurface(
            color = effectiveColor,
            cornerRadius = cornerRadius,
            elevation = if (isPressed && enabled) elevation * 0.45f else elevation,
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides effectiveContent,
                ) { content() }
            }
        }
    }
}

/**
 * Low-emphasis "ghost" button — outlined transparent panel for secondary
 * actions (Cancel, Back, "Not now"). Same press-spring as [ClayButton] so
 * the interaction feels consistent, but it doesn't compete with the primary.
 */
@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = LocalClayTokens.current.corner,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ghost-press",
    )
    val tokens = LocalClayTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(Color.Transparent, shape)
            .border(1.dp, tokens.strokeColor.copy(alpha = alpha), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
        ) { content() }
    }
}

/**
 * Tiny helper: a chunky Bungee label. Kept as a one-liner utility for button
 * contents so callers don't repeat the font/weight/style triple.
 */
@Composable
fun ClayLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
}
