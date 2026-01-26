package `in`.project.enroute.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.core.graphics.toColorInt

/**
 * Horizontal floor slider for switching between building floors.
 * Shows building name at top, slider in middle, and floor numbers at bottom.
 * Visibility is controlled by the isVisible parameter with animations.
 *
 * @param buildingName Name of the building to display
 * @param availableFloors List of available floor numbers (sorted ascending)
 * @param currentFloor Current floor number
 * @param onFloorChange Callback when floor is changed
 * @param isVisible Whether the slider should be visible
 * @param modifier Modifier for the component
 */
@Composable
fun FloorSlider(
    buildingName: String,
    availableFloors: List<Float>,
    currentFloor: Float,
    onFloorChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible && availableFloors.size > 1,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        FloorSliderContent(
            buildingName = buildingName,
            availableFloors = availableFloors,
            currentFloor = currentFloor,
            onFloorChange = onFloorChange
        )
    }
}

/**
 * Internal content of the floor slider.
 */
@Composable
private fun FloorSliderContent(
    buildingName: String,
    availableFloors: List<Float>,
    currentFloor: Float,
    onFloorChange: (Float) -> Unit
) {
    // Use safe defaults if list is empty (can happen briefly during exit animation)
    val safeFloors = if (availableFloors.size >= 2) availableFloors else listOf(0f, 1f)
    val safeCurrentFloor = if (availableFloors.contains(currentFloor)) currentFloor else safeFloors.first()
    
    val minFloor = safeFloors.minOrNull() ?: 0f
    val maxFloor = safeFloors.maxOrNull() ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Building name at top
        if (buildingName.isNotEmpty()) {
            val floorDisplay = if (safeCurrentFloor == safeCurrentFloor.toInt().toFloat()) {
                safeCurrentFloor.toInt().toString()
            } else {
                safeCurrentFloor.toString()
            }
            Text(
                text = "$buildingName : Floor $floorDisplay",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Custom compact slider
        CompactFloorSlider(
            availableFloors = safeFloors,
            currentFloor = safeCurrentFloor,
            minFloor = minFloor,
            maxFloor = maxFloor,
            onFloorChange = onFloorChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

/**
 * Custom compact slider for floor selection.
 * Draws a track with dots for each floor and supports tap and drag gestures.
 */
@Composable
private fun CompactFloorSlider(
    availableFloors: List<Float>,
    currentFloor: Float,
    minFloor: Float,
    maxFloor: Float,
    onFloorChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val outlineColor = MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier
            .pointerInput(availableFloors) {
                detectTapGestures { offset ->
                    // Handle tap - find nearest floor based on x position
                    val thumbRadius = 12.dp.toPx()
                    val trackWidth = size.width - (thumbRadius * 2)
                    val tappedProgress = ((offset.x - thumbRadius) / trackWidth).coerceIn(0f, 1f)
                    val tappedValue = minFloor + (maxFloor - minFloor) * tappedProgress
                    
                    val nearestFloor = availableFloors.minByOrNull { 
                        abs(it - tappedValue) 
                    } ?: currentFloor
                    
                    onFloorChange(nearestFloor)
                }
            }
            .pointerInput(availableFloors) {
                detectDragGestures { change, _ ->
                    change.consume()
                    
                    // Handle drag - find nearest floor based on current position
                    val thumbRadius = 12.dp.toPx()
                    val trackWidth = size.width - (thumbRadius * 2)
                    val dragProgress = ((change.position.x - thumbRadius) / trackWidth).coerceIn(0f, 1f)
                    val dragValue = minFloor + (maxFloor - minFloor) * dragProgress
                    
                    val nearestFloor = availableFloors.minByOrNull { 
                        abs(it - dragValue) 
                    } ?: currentFloor
                    
                    onFloorChange(nearestFloor)
                }
            }
    ) {
        val thumbRadius = 12.dp.toPx()
        val trackWidth = size.width - (thumbRadius * 2)
        val centerY = size.height / 2
        
        val trackStart = Offset(thumbRadius, centerY)
        val trackEnd = Offset(size.width - thumbRadius, centerY)
        
        // Draw background track
        drawLine(
            color = inactiveColor,
            start = trackStart,
            end = trackEnd,
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Calculate current floor position
        val currentProgress = if (maxFloor > minFloor) {
            (currentFloor - minFloor) / (maxFloor - minFloor)
        } else {
            0f
        }
        val currentX = thumbRadius + trackWidth * currentProgress
        
        // Draw active track (from start to current floor)
        drawLine(
            color = primaryColor,
            start = trackStart,
            end = Offset(currentX, centerY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Draw pill background behind current floor number FIRST
        val pillWidth = 44.dp.toPx()
        val pillHeight = 26.dp.toPx()
        val pillLeft = currentX - (pillWidth / 2)
        val pillTop = centerY - (pillHeight / 2)
        
        // Draw pill fill
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(pillLeft, pillTop),
            size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx(), 13.dp.toPx())
        )
        
        // Draw pill border
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(pillLeft, pillTop),
            size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx(), 13.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        
        // Draw floor numbers on the track
        for (floor in availableFloors) {
            val progress = if (maxFloor > minFloor) {
                (floor - minFloor) / (maxFloor - minFloor)
            } else {
                0f
            }
            val x = thumbRadius + trackWidth * progress
            val isCurrentFloor = floor == currentFloor
            
            val fontSize = 18.sp.toPx()
            
            val textPaint = android.graphics.Paint().apply {
                color = "#ffffff".toColorInt()
                textSize = fontSize
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = if (isCurrentFloor)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                else
                    android.graphics.Typeface.DEFAULT
            }
            
            val outlinePaint = android.graphics.Paint().apply {
                color = outlineColor.toArgb()
                textSize = fontSize
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = textPaint.typeface
                strokeWidth = 10f
                style = android.graphics.Paint.Style.STROKE
            }
            
            val floorText = if (floor == floor.toInt().toFloat()) {
                floor.toInt().toString()
            } else {
                floor.toString()
            }
            
            val textY = centerY + (fontSize / 3)
            
            drawIntoCanvas { canvas ->
                // Draw outline
                canvas.nativeCanvas.drawText(floorText, x, textY, outlinePaint)
                // Draw text on top
                canvas.nativeCanvas.drawText(floorText, x, textY, textPaint)
            }
        }
    }
}
