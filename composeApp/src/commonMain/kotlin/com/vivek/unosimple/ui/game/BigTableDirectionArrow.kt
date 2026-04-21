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
 * Big curved amber direction arrow wrapping the discard pile — lifted from
 * the reference mobile UNO app. Sits in the middle of the felt, low-alpha
 * enough to read as table art but obvious enough that players never
 * forget which way the turn is moving.
 *
 * Two arc segments (top + bottom) with an arrowhead at the leading end
 * based on [direction]. Scales with the parent container — draws around a
 * notional center card roughly 200dp wide.
 */
@Composable
internal fun BigTableDirectionArrow(direction: PlayDirection) {
    val arrowColor = Color(0xFFFFB53B).copy(alpha = 0.55f) // amber @ ~55%

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = 140.dp.toPx()
            val stroke = 14.dp.toPx()
            val isClockwise = direction == PlayDirection.CLOCKWISE

            // Two half-arcs, one on top, one on bottom — a gap at the left
            // and right of the center so the discard + draw piles aren't
            // covered by the arrow stroke.
            // Top arc: sweeps from 220° to 320° (via 270°) covering the top half.
            // Bottom arc: 40° to 140° covering the bottom half.
            drawArc(
                color = arrowColor,
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = arrowColor,
                startAngle = 40f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Arrowhead at the "leading" end of the top arc — right-side
            // for clockwise, left-side for counter-clockwise.
            val tipAngleDeg = if (isClockwise) 320f else 220f
            val tipRad = tipAngleDeg * PI.toFloat() / 180f
            val tipX = cx + r * cos(tipRad)
            val tipY = cy + r * sin(tipRad)
            val headLen = stroke * 2.2f
            // Tangent direction (clockwise goes counter-clockwise angles, so
            // we back off in the opposite direction relative to the sweep).
            val tangentDeg = if (isClockwise) tipAngleDeg + 90f else tipAngleDeg - 90f
            val tangentRad = tangentDeg * PI.toFloat() / 180f
            val baseX = tipX - headLen * cos(tangentRad)
            val baseY = tipY - headLen * sin(tangentRad)
            val perpDeg = tangentDeg + 90f
            val perpRad = perpDeg * PI.toFloat() / 180f
            val side = headLen * 0.55f
            val offsX = side * cos(perpRad)
            val offsY = side * sin(perpRad)

            val arrowHead = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX + offsX, baseY + offsY)
                lineTo(baseX - offsX, baseY - offsY)
                close()
            }
            drawPath(arrowHead, color = arrowColor)
        }
    }
}
