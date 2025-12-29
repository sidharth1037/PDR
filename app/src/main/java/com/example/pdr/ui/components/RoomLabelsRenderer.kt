package com.example.pdr.ui.components

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import com.example.pdr.model.Room
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders room labels on the floor plan with reverse rotation to keep text upright.
 * Labels are drawn at room center coordinates with the room name displayed.
 * Labels only appear when zoomed in above a minimum threshold.
 */
object RoomLabelsRenderer {

    // Minimum zoom level required to display room labels
    private const val MIN_ZOOM_TO_SHOW_LABELS = 0.64f

    /**
     * Draws room labels on the canvas.
     *
     * @param rooms List of rooms to display labels for
     * @param floorPlanScale Scale factor applied to room coordinates
     * @param floorPlanRotationDegrees Rotation angle of the floor plan in degrees
     * @param scale Current canvas zoom/scale level
     * @param canvasRotation Current canvas rotation in degrees (used for reverse rotation)
     */
    fun DrawScope.drawRoomLabels(
        rooms: List<Room>,
        floorPlanScale: Float,
        floorPlanRotationDegrees: Float,
        scale: Float,
        canvasRotation: Float
    ) {
        // Don't draw labels if zoomed out beyond the threshold
        if (scale < MIN_ZOOM_TO_SHOW_LABELS) {
            return
        }

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 36f / scale
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }

            val backgroundPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }

            for (room in rooms) {
                // Only draw if room has a name
                if (room.name.isNullOrBlank()) continue

                // Apply floor plan scale and rotation
                val x = room.x * floorPlanScale
                val y = room.y * floorPlanScale
                val angleRad = Math.toRadians(floorPlanRotationDegrees.toDouble()).toFloat()
                val cos = cos(angleRad)
                val sin = sin(angleRad)
                val rotatedX = x * cos - y * sin
                val rotatedY = x * sin + y * cos

                // Split text into lines if it has more than 3 words
                val words = room.name.split(" ")
                val lines = if (words.size > 3) {
                    listOf(
                        words.take(2).joinToString(" "),
                        words.drop(2).joinToString(" ")
                    )
                } else {
                    listOf(room.name)
                }

                // Get text bounds for all lines to size the background
                val textBounds = mutableListOf<android.graphics.Rect>()
                for (line in lines) {
                    val bounds = android.graphics.Rect()
                    paint.getTextBounds(line, 0, line.length, bounds)
                    textBounds.add(bounds)
                }

                val padding = 8f / scale
                val cornerRadius = 8f / scale
                val lineHeight = 40f / scale

                // Calculate background dimensions based on all lines
                val maxWidth = textBounds.maxOf { it.width() }
                val totalHeight = textBounds.size * lineHeight

                // Draw the room label with reverse rotation to keep text upright
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(rotatedX, rotatedY)
                // Apply reverse rotation to counteract the canvas rotation
                canvas.nativeCanvas.rotate(-canvasRotation)

                // Draw white background rectangle with rounded corners
                val bgLeft = -maxWidth / 2f - padding
                val bgTop = -totalHeight / 2f - padding
                val bgRight = maxWidth / 2f + padding
                val bgBottom = totalHeight / 2f + padding
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(bgLeft, bgTop, bgRight, bgBottom),
                    cornerRadius,
                    cornerRadius,
                    backgroundPaint
                )

                // Draw text lines on top
                for ((index, line) in lines.withIndex()) {
                    val yOffset = -totalHeight / 2f + (index * lineHeight) + 32f / scale
                    canvas.nativeCanvas.drawText(line, 0f, yOffset, paint)
                }

                canvas.nativeCanvas.restore()
            }
        }
    }
}
