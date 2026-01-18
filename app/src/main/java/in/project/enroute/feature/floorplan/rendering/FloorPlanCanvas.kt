package `in`.project.enroute.feature.floorplan.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.ClipOp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import `in`.project.enroute.data.model.FloorPlanData
import `in`.project.enroute.feature.floorplan.rendering.renderers.drawBoundary
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
    val backgroundColor: Color = Color.White,
    val boundaryColor: Color = Color(0xFFF5F5F5)
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
 * Supports multi-floor rendering by stacking floors from bottom to current.
 *
 * @param floorsToRender List of floors to render from bottom to top (for multi-floor stacking)
 * @param canvasState Current canvas transformation state
 * @param onCanvasStateChange Callback when canvas state changes (gestures)
 * @param modifier Modifier for the canvas
 * @param displayConfig Configuration for what to show and how
 */
@Composable
fun FloorPlanCanvas(
    floorsToRender: List<FloorPlanData>,
    canvasState: CanvasState,
    onCanvasStateChange: (CanvasState) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: FloorPlanDisplayConfig = FloorPlanDisplayConfig()
) {
    if (floorsToRender.isEmpty()) return

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
                    val cos = cos(angleRad)
                    val sin = sin(angleRad)
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
            // Render floors from bottom to top (stacked)
            for ((index, floorData) in floorsToRender.withIndex()) {
                val isCurrentFloor = index == floorsToRender.size - 1
                val floorPlanScale = floorData.metadata.scale
                val floorPlanRotation = floorData.metadata.rotation

                // Get clip path from all floors above this one
                val floorsAbove = floorsToRender.subList(index + 1, floorsToRender.size)
                val clipPathFromAbove = buildClipPathFromFloors(floorsAbove)

                // Draw boundary polygons to mask floors below
                drawBoundary(
                    boundaryPolygons = floorData.boundaryPolygons,
                    scale = floorPlanScale,
                    rotationDegrees = floorPlanRotation,
                    color = displayConfig.boundaryColor
                )

                // Apply clipping to hide content covered by floors above
                val drawFloorContent: () -> Unit = {
                    // Draw stairwells
                    if (displayConfig.showStairwells) {
                        drawStairwells(
                            stairwells = floorData.stairwells,
                            scale = floorPlanScale,
                            rotationDegrees = floorPlanRotation,
                            color = displayConfig.stairwellColor
                        )
                    }

                    // Draw walls
                    if (displayConfig.showWalls) {
                        drawWalls(
                            walls = floorData.walls,
                            scale = floorPlanScale,
                            rotationDegrees = floorPlanRotation,
                            color = displayConfig.wallColor
                        )
                    }

                    // Draw entrances for current floor only
                    if (isCurrentFloor && displayConfig.showEntrances) {
                        drawEntrances(
                            entrances = floorData.entrances,
                            scale = floorPlanScale,
                            rotationDegrees = floorPlanRotation,
                            canvasScale = canvasState.scale,
                            canvasRotation = canvasState.rotation
                        )
                    }

                    // Draw room labels for all floors
                    if (displayConfig.showRoomLabels) {
                        drawRoomLabels(
                            rooms = floorData.rooms,
                            scale = floorPlanScale,
                            rotationDegrees = floorPlanRotation,
                            canvasScale = canvasState.scale,
                            canvasRotation = canvasState.rotation
                        )
                    }
                }

                // If there are floors above, clip out their boundary areas
                if (clipPathFromAbove != null) {
                    clipPath(clipPathFromAbove, clipOp = ClipOp.Difference) {
                        drawFloorContent()
                    }
                } else {
                    drawFloorContent()
                }
            }
        }
    }
}

/**
 * Builds a combined clip path from all boundary polygons of the given floors.
 */
private fun buildClipPathFromFloors(floors: List<FloorPlanData>): Path? {
    if (floors.isEmpty()) return null

    val combinedPath = Path()
    
    for (floorData in floors) {
        val scale = floorData.metadata.scale
        val rotationDegrees = floorData.metadata.rotation
        val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cosAngle = cos(angleRad)
        val sinAngle = sin(angleRad)

        for (polygon in floorData.boundaryPolygons) {
            if (polygon.points.isEmpty()) continue
            
            val sortedPoints = polygon.points.sortedBy { it.id }
            val transformedPoints = sortedPoints.map { point ->
                val x = point.x * scale
                val y = point.y * scale
                val rotatedX = x * cosAngle - y * sinAngle
                val rotatedY = x * sinAngle + y * cosAngle
                Offset(rotatedX, rotatedY)
            }

            if (transformedPoints.isNotEmpty()) {
                combinedPath.moveTo(transformedPoints[0].x, transformedPoints[0].y)
                for (i in 1 until transformedPoints.size) {
                    combinedPath.lineTo(transformedPoints[i].x, transformedPoints[i].y)
                }
                combinedPath.close()
            }
        }
    }

    return if (combinedPath.isEmpty) null else combinedPath
}
