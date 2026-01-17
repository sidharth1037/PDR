package `in`.project.enroute.feature.floorplan.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.abs
import `in`.project.enroute.data.model.FloorPlanData
import `in`.project.enroute.feature.floorplan.rendering.renderers.drawEntrances
import `in`.project.enroute.feature.floorplan.rendering.renderers.drawRoomLabels
import `in`.project.enroute.feature.floorplan.rendering.renderers.drawStairwells
import `in`.project.enroute.feature.floorplan.rendering.renderers.drawWalls

/**
 * Display configuration for the floor plan rendering.
 * Note: scale and rotation come from FloorPlanData.metadata
 */
data class FloorPlanDisplayConfig(
    val showWalls: Boolean = true,
    val showStairwells: Boolean = true,
    val showEntrances: Boolean = true,
    val showRoomLabels: Boolean = true,
    val wallColor: Color = Color.Black,
    val stairwellColor: Color = Color(0xFFADD8E6),
    val backgroundColor: Color = Color.White
)

/**
 * Canvas state for pan/zoom/rotate gestures.
 */
data class CanvasState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
)

/**
 * Main floor plan canvas composable.
 * Handles rendering of all floor plan elements with gesture support.
 *
 * @param floorPlanData The floor plan data to render
 * @param canvasState Current canvas transformation state
 * @param onCanvasStateChange Callback when canvas state changes (gestures)
 * @param displayConfig Configuration for what to show and how
 * @param modifier Modifier for the canvas
 */
@Composable
fun FloorPlanCanvas(
    floorPlanData: FloorPlanData,
    canvasState: CanvasState,
    onCanvasStateChange: (CanvasState) -> Unit,
    displayConfig: FloorPlanDisplayConfig = FloorPlanDisplayConfig(),
    modifier: Modifier = Modifier
) {
    // Get scale and rotation from metadata
    val floorPlanScale = floorPlanData.metadata.scale
    val floorPlanRotation = floorPlanData.metadata.rotation

    // Calculate content bounds for background sizing
    val contentBounds = remember(floorPlanData.walls, floorPlanScale, floorPlanRotation) {
        calculateContentBounds(floorPlanData, floorPlanScale, floorPlanRotation)
    }

    // Use rememberUpdatedState to capture latest state without restarting gesture handler
    val currentCanvasState = rememberUpdatedState(canvasState)
    val currentOnCanvasStateChange = rememberUpdatedState(onCanvasStateChange)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(displayConfig.backgroundColor)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotationChange ->
                    val state = currentCanvasState.value
                    
                    // Heuristic: if rotation is dominant, ignore zoom; otherwise ignore rotation
                    // This prevents accidental zoom while rotating and vice versa
                    val effectiveZoom: Float
                    val effectiveRotationChange: Float
                    if (abs(rotationChange) > abs(zoom - 1f) * 60) {
                        effectiveRotationChange = rotationChange
                        effectiveZoom = 1f
                    } else {
                        effectiveRotationChange = 0f
                        effectiveZoom = zoom
                    }

                    val oldScale = state.scale
                    val newScale = (state.scale * effectiveZoom).coerceIn(0.1f, 10f)
                    val actualZoom = newScale / oldScale

                    // Calculate offset relative to centroid
                    val offsetFromCentroidX = state.offsetX - centroid.x
                    val offsetFromCentroidY = state.offsetY - centroid.y

                    // Scale the offset from centroid
                    val scaledOffsetFromCentroidX = offsetFromCentroidX * actualZoom
                    val scaledOffsetFromCentroidY = offsetFromCentroidY * actualZoom

                    // Rotate the offset around centroid
                    val angleRad = Math.toRadians(effectiveRotationChange.toDouble()).toFloat()
                    val cos = kotlin.math.cos(angleRad)
                    val sin = kotlin.math.sin(angleRad)
                    val rotatedOffsetFromCentroidX = scaledOffsetFromCentroidX * cos - scaledOffsetFromCentroidY * sin
                    val rotatedOffsetFromCentroidY = scaledOffsetFromCentroidX * sin + scaledOffsetFromCentroidY * cos

                    // Apply new offset = centroid + rotated/scaled offset + pan
                    val newOffsetX = centroid.x + rotatedOffsetFromCentroidX + pan.x
                    val newOffsetY = centroid.y + rotatedOffsetFromCentroidY + pan.y
                    val newRotation = state.rotation + effectiveRotationChange

                    currentOnCanvasStateChange.value(
                        state.copy(
                            scale = newScale,
                            offsetX = newOffsetX,
                            offsetY = newOffsetY,
                            rotation = newRotation
                        )
                    )
                }
            }
            .graphicsLayer(
                scaleX = canvasState.scale,
                scaleY = canvasState.scale,
                rotationZ = canvasState.rotation,
                translationX = canvasState.offsetX,
                translationY = canvasState.offsetY,
                transformOrigin = TransformOrigin(0f, 0f)
            )
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        translate(left = centerX, top = centerY) {
            // Draw background for content area
            val padding = 500f
            drawRect(
                color = displayConfig.backgroundColor,
                topLeft = Offset(contentBounds.left - padding, contentBounds.top - padding),
                size = Size(contentBounds.width + padding * 2, contentBounds.height + padding * 2)
            )

            // Draw stairwells first (below walls)
            if (displayConfig.showStairwells) {
                drawStairwells(
                    stairwells = floorPlanData.stairwells,
                    scale = floorPlanScale,
                    rotationDegrees = floorPlanRotation,
                    color = displayConfig.stairwellColor
                )
            }

            // Draw walls
            if (displayConfig.showWalls) {
                drawWalls(
                    walls = floorPlanData.walls,
                    scale = floorPlanScale,
                    rotationDegrees = floorPlanRotation,
                    color = displayConfig.wallColor
                )
            }

            // Draw entrances
            if (displayConfig.showEntrances) {
                drawEntrances(
                    entrances = floorPlanData.entrances,
                    scale = floorPlanScale,
                    rotationDegrees = floorPlanRotation,
                    canvasScale = canvasState.scale,
                    canvasRotation = canvasState.rotation
                )
            }

            // Draw room labels
            if (displayConfig.showRoomLabels) {
                drawRoomLabels(
                    rooms = floorPlanData.rooms,
                    scale = floorPlanScale,
                    rotationDegrees = floorPlanRotation,
                    canvasScale = canvasState.scale,
                    canvasRotation = canvasState.rotation
                )
            }
        }
    }
}

/**
 * Calculates the bounding box for all floor plan content.
 */
private fun calculateContentBounds(
    floorPlanData: FloorPlanData,
    scale: Float,
    rotationDegrees: Float
): Rect {
    if (floorPlanData.walls.isEmpty()) {
        return Rect(-500f, -500f, 500f, 500f)
    }

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
    val cosAngle = kotlin.math.cos(angleRad)
    val sinAngle = kotlin.math.sin(angleRad)

    for (wall in floorPlanData.walls) {
        val x1 = wall.x1 * scale
        val y1 = wall.y1 * scale
        val x2 = wall.x2 * scale
        val y2 = wall.y2 * scale

        val rotatedX1 = x1 * cosAngle - y1 * sinAngle
        val rotatedY1 = x1 * sinAngle + y1 * cosAngle
        val rotatedX2 = x2 * cosAngle - y2 * sinAngle
        val rotatedY2 = x2 * sinAngle + y2 * cosAngle

        minX = minOf(minX, rotatedX1, rotatedX2)
        minY = minOf(minY, rotatedY1, rotatedY2)
        maxX = maxOf(maxX, rotatedX1, rotatedX2)
        maxY = maxOf(maxY, rotatedY1, rotatedY2)
    }

    return if (minX == Float.POSITIVE_INFINITY) {
        Rect(-500f, -500f, 500f, 500f)
    } else {
        Rect(minX, minY, maxX, maxY)
    }
}
