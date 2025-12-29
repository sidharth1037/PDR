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
 */
object RoomLabelsRenderer {

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

                // Get text bounds to size the background
                val textBounds = android.graphics.Rect()
                paint.getTextBounds(room.name, 0, room.name.length, textBounds)
                val padding = 8f / scale
                val cornerRadius = 8f / scale

                // Draw the room label with reverse rotation to keep text upright
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(rotatedX, rotatedY)
                // Apply reverse rotation to counteract the canvas rotation
                canvas.nativeCanvas.rotate(-canvasRotation)

                // Draw white background rectangle with rounded corners
                val bgLeft = -textBounds.width() / 2f - padding
                val bgTop = -textBounds.height() / 2f - padding
                val bgRight = textBounds.width() / 2f + padding
                val bgBottom = textBounds.height() / 2f + padding
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(bgLeft, bgTop, bgRight, bgBottom),
                    cornerRadius,
                    cornerRadius,
                    backgroundPaint
                )

                // Draw text on top (moved down slightly for better centering)
                canvas.nativeCanvas.drawText(room.name, 0f, 6f / scale, paint)
                canvas.nativeCanvas.restore()
            }
        }
    }
}
