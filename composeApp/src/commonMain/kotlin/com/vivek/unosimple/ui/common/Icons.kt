package com.vivek.unosimple.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal Canvas-drawn icons to reduce reliance on text labels. Skia's web
 * font doesn't reliably render icon glyphs, and pulling in material-icons
 * would add ~200KB to the Wasm bundle for a handful of marks. These hand-
 * rolled icons stay crisp at any size and inherit color from
 * [LocalContentColor].
 */

/** Three horizontal lines — menu / hamburger. */
@Composable
fun MenuIcon(size: Dp = 24.dp, color: Color = LocalContentColor.current) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.5.dp.toPx()
        val pad = size.toPx() * 0.15f
        val xStart = pad
        val xEnd = this.size.width - pad
        val step = (this.size.height - pad * 2) / 2f
        for (i in 0..2) {
            val y = pad + i * step
            drawLine(
                color = color,
                start = Offset(xStart, y),
                end = Offset(xEnd, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Gear / settings cog. 8-tooth simplification. */
@Composable
fun GearIcon(size: Dp = 24.dp, color: Color = LocalContentColor.current) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outerR = this.size.minDimension / 2f * 0.95f
        val innerR = outerR * 0.62f
        val toothR = outerR
        val bodyR = outerR * 0.72f

        // Tooth path: 8 teeth alternating between toothR (outer) and bodyR (inner).
        val teeth = 8
        val path = Path()
        for (i in 0 until teeth * 2) {
            val r = if (i % 2 == 0) toothR else bodyR
            val angle = -PI / 2 + i * PI / teeth
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, join = StrokeJoin.Round),
        )
        // Center hole.
        drawCircle(
            color = color,
            radius = innerR * 0.45f,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
    }
}

/** Back arrow — left-pointing chevron. */
@Composable
fun BackIcon(size: Dp = 24.dp, color: Color = LocalContentColor.current) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.5.dp.toPx()
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f * 0.55f
        drawLine(
            color = color,
            start = Offset(cx + r * 0.4f, cy - r),
            end = Offset(cx - r * 0.7f, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(cx - r * 0.7f, cy),
            end = Offset(cx + r * 0.4f, cy + r),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Copy icon — two overlapping rounded rectangles. */
@Composable
fun CopyIcon(size: Dp = 20.dp, color: Color = LocalContentColor.current) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 1.8.dp.toPx()
        val w = this.size.width * 0.55f
        val h = this.size.height * 0.7f
        val radius = 2.dp.toPx()
        // Back rect (top-left)
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(width = stroke),
        )
        // Front rect (bottom-right), solid filled faintly
        drawRoundRect(
            color = color,
            topLeft = Offset(this.size.width - w, this.size.height - h),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(width = stroke),
        )
    }
}

/** Trophy icon — cup shape. */
@Composable
fun TrophyIcon(size: Dp = 28.dp, color: Color = LocalContentColor.current) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val cx = this.size.width / 2f
        val top = this.size.height * 0.15f
        val cupBottom = this.size.height * 0.70f
        val cupWidth = this.size.width * 0.55f
        // Cup body
        val leftX = cx - cupWidth / 2f
        val rightX = cx + cupWidth / 2f
        val cupPath = Path().apply {
            moveTo(leftX, top)
            lineTo(rightX, top)
            lineTo(rightX - cupWidth * 0.15f, cupBottom)
            lineTo(leftX + cupWidth * 0.15f, cupBottom)
            close()
        }
        drawPath(cupPath, color = color)
        // Side handles (small arcs)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(leftX - cupWidth * 0.25f, top),
            size = androidx.compose.ui.geometry.Size(cupWidth * 0.3f, cupWidth * 0.5f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(rightX - cupWidth * 0.05f, top),
            size = androidx.compose.ui.geometry.Size(cupWidth * 0.3f, cupWidth * 0.5f),
            style = Stroke(width = stroke),
        )
        // Base
        val baseY = this.size.height * 0.88f
        drawLine(
            color = color,
            start = Offset(cx, cupBottom),
            end = Offset(cx, baseY),
            strokeWidth = stroke * 1.5f,
        )
        drawLine(
            color = color,
            start = Offset(cx - cupWidth * 0.25f, this.size.height),
            end = Offset(cx + cupWidth * 0.25f, this.size.height),
            strokeWidth = stroke * 1.5f,
            cap = StrokeCap.Round,
        )
    }
}
