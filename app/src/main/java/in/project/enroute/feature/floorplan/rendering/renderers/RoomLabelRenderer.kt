package `in`.project.enroute.feature.floorplan.rendering.renderers

import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import `in`.project.enroute.data.model.Room
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders room labels on the canvas.
 * Labels are drawn at room center points and counter-rotated to stay readable.
 * Labels longer than 15 characters are split across multiple lines without cutting words.
 * Labels are hidden when canvas zoom level is below 0.5.
 * Labels maintain constant size at zoom levels >= 0.8, and scale down below that threshold.
 */
fun DrawScope.drawRoomLabels(
    rooms: List<Room>,
    scale: Float,
    rotationDegrees: Float,
    canvasScale: Float,
    canvasRotation: Float,
    textColor: Int = android.graphics.Color.DKGRAY,
    textSize: Float = 30f,
    minZoomForConstantSize: Float = 0.76f
) {
    // Don't render labels if zoom level is too low
    if (canvasScale < 0.48f) {
        return
    }

    val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)

    // Calculate text size: maintain constant size at zoom >= minZoomForConstantSize
    val effectiveTextSize = if (canvasScale >= minZoomForConstantSize) {
        textSize / minZoomForConstantSize  // fixed size at higher zoom
    } else {
        textSize / canvasScale  // scale down at lower zoom
    }

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = textColor
            this.textSize = effectiveTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Paint for the white outline/border
        val outlinePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            this.textSize = effectiveTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 8f / canvasScale
        }

        // Counter-rotate text so it stays readable
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.rotate(-canvasRotation)

        for (room in rooms) {
            if (room.name != null) {
                val x = room.x * scale
                val y = room.y * scale

                val rotatedX = x * cosAngle - y * sinAngle
                val rotatedY = x * sinAngle + y * cosAngle

                // Rotate point back for text positioning
                val textAngleRad = Math.toRadians(canvasRotation.toDouble()).toFloat()
                val textCos = cos(textAngleRad)
                val textSin = sin(textAngleRad)
                val textX = rotatedX * textCos - rotatedY * textSin
                val textY = rotatedX * textSin + rotatedY * textCos

                // Build label text with number and name
                val labelText = if (room.number != null) {
                    "${room.number}: ${room.name}"
                } else {
                    room.name
                }

                // Split label into lines if necessary
                val lines = splitLabel(labelText, maxCharsPerLine = 15)
                val lineHeight = paint.descent() - paint.ascent()
                val totalHeight = lineHeight * lines.size
                val startY = textY - totalHeight / 2

                for ((index, line) in lines.withIndex()) {
                    val lineY = startY + index * lineHeight + paint.textSize / 2
                    // Draw white outline first
                    canvas.nativeCanvas.drawText(line, textX, lineY, outlinePaint)
                    // Draw text on top with original color
                    canvas.nativeCanvas.drawText(line, textX, lineY, paint)
                }
            }
        }

        canvas.nativeCanvas.restore()
    }
}

/**
 * Splits text into lines without cutting words, respecting max characters per line.
 */
private fun splitLabel(text: String, maxCharsPerLine: Int): List<String> {
    if (text.length <= maxCharsPerLine) {
        return listOf(text)
    }

    val lines = mutableListOf<String>()
    val words = text.split(" ")
    var currentLine = ""

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        
        if (testLine.length <= maxCharsPerLine) {
            currentLine = testLine
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            currentLine = word
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines
}
