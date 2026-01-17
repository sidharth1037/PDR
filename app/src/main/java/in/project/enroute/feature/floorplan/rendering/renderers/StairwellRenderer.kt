package `in`.project.enroute.feature.floorplan.rendering.renderers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import `in`.project.enroute.data.model.Stairwell
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders stairwell polygons on the canvas.
 */
fun DrawScope.drawStairwells(
    stairwells: List<Stairwell>,
    scale: Float,
    rotationDegrees: Float,
    color: Color = Color(0xFFADD8E6) // Light blue
) {
    val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)

    for (stairwell in stairwells) {
        if (stairwell.points.size >= 3) {
            val transformedPoints = stairwell.points.map { (x, y) ->
                val scaledX = x * scale
                val scaledY = y * scale
                val rotatedX = scaledX * cosAngle - scaledY * sinAngle
                val rotatedY = scaledX * sinAngle + scaledY * cosAngle
                Pair(rotatedX, rotatedY)
            }

            val path = Path().apply {
                val first = transformedPoints.first()
                moveTo(first.first, first.second)
                transformedPoints.drop(1).forEach { (x, y) ->
                    lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }
    }
}
