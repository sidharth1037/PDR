package `in`.project.enroute.feature.floorplan.rendering.renderers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import `in`.project.enroute.data.model.Wall
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders wall segments on the canvas.
 */
fun DrawScope.drawWalls(
    walls: List<Wall>,
    scale: Float,
    rotationDegrees: Float,
    strokeWidth: Float = 3f,
    color: Color = Color.Black
) {
    val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)

    for (wall in walls) {
        val x1 = wall.x1 * scale
        val y1 = wall.y1 * scale
        val x2 = wall.x2 * scale
        val y2 = wall.y2 * scale

        // Apply rotation
        val rotatedX1 = x1 * cosAngle - y1 * sinAngle
        val rotatedY1 = x1 * sinAngle + y1 * cosAngle
        val rotatedX2 = x2 * cosAngle - y2 * sinAngle
        val rotatedY2 = x2 * sinAngle + y2 * cosAngle

        drawLine(
            color = color,
            start = Offset(rotatedX1, rotatedY1),
            end = Offset(rotatedX2, rotatedY2),
            strokeWidth = strokeWidth
        )
    }
}
