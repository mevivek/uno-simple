package com.vivek.unosimple.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.PlayDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Giant curved direction arrow — a single ~260° arc wrapping the discard
 * pile, with a chunky arrowhead at the leading end indicating which way
 * play is moving. Sits on the red felt at low alpha so it reads as table
 * decoration, not live UI. Lifted from the reference mobile UNO app's
 * center arrow.
 *
 * The arrow is drawn in CANVAS coordinates, where y grows DOWN. "Clockwise"
 * in gameplay terms (the natural UNO play order) corresponds to the
 * positive angular sweep direction on the canvas, so we sweep positive
 * angles for CLOCKWISE and negative for COUNTER_CLOCKWISE.
 */
@Composable
internal fun BigTableDirectionArrow(direction: PlayDirection) {
    val arrowColor = Color(0xFFFFB53B).copy(alpha = 0.55f) // amber @ ~55%
    val isClockwise = direction == PlayDirection.CLOCKWISE

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = 150.dp.toPx()
            val stroke = 16.dp.toPx()

            // One single arc with a gap at the bottom (where the draw/discard
            // row sits). Sweeps 260° around; arrowhead sits at the leading
            // end of that sweep.
            //
            // Clockwise: start at angle -150° (upper-left-ish), sweep +260°
            // ending at 110° (lower-left).
            // Counter-clockwise: mirror — start at -30° (upper-right),
            // sweep -260° ending at 70° (lower-right).
            val startDeg = if (isClockwise) -150f else -30f
            val sweepDeg = if (isClockwise) 260f else -260f

            drawArc(
                color = arrowColor,
                startAngle = startDeg,
                sweepAngle = sweepDeg,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Arrowhead — a solid triangle tangent to the circle at the
            // leading end.
            val tipDeg = startDeg + sweepDeg
            val tipRad = tipDeg * PI.toFloat() / 180f
            val tipX = cx + r * cos(tipRad)
            val tipY = cy + r * sin(tipRad)

            // Tangent direction (in the direction of motion). For positive
            // sweep (CW), tangent is the +90° rotation of the radial
            // vector; for negative sweep (CCW), -90°.
            val tangentOffsetDeg = if (isClockwise) 90f else -90f
            val tangRad = (tipDeg + tangentOffsetDeg) * PI.toFloat() / 180f
            val tUx = cos(tangRad)
            val tUy = sin(tangRad)
            // Perpendicular to the tangent — used to splay the arrowhead.
            val pUx = -tUy
            val pUy = tUx

            val headLen = stroke * 2.4f
            val halfBase = stroke * 1.4f

            val p1 = Offset(tipX + tUx * headLen * 0.6f, tipY + tUy * headLen * 0.6f)
            val baseCenter = Offset(tipX - tUx * headLen * 0.4f, tipY - tUy * headLen * 0.4f)
            val p2 = Offset(baseCenter.x + pUx * halfBase, baseCenter.y + pUy * halfBase)
            val p3 = Offset(baseCenter.x - pUx * halfBase, baseCenter.y - pUy * halfBase)

            val head = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }
            drawPath(head, color = arrowColor)
        }
    }
}
