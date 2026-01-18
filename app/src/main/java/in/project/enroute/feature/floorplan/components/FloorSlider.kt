package `in`.project.enroute.feature.floorplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Horizontal floor slider for switching between building floors.
 * Shows slider at top with floor numbers displayed below.
 *
 * @param availableFloors List of available floor numbers (sorted ascending)
 * @param currentFloor Current floor number
 * @param onFloorChange Callback when floor is changed
 * @param modifier Modifier for the component
 */
@Composable
fun FloorSlider(
    availableFloors: List<Float>,
    currentFloor: Float,
    onFloorChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableFloors.size <= 1) return

    val minFloor = availableFloors.minOrNull() ?: 1f
    val maxFloor = availableFloors.maxOrNull() ?: 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Horizontal slider
        Slider(
            value = currentFloor,
            onValueChange = { newValue ->
                // Snap to nearest available floor
                val nearestFloor = availableFloors.minByOrNull { 
                    kotlin.math.abs(it - newValue) 
                } ?: currentFloor
                onFloorChange(nearestFloor)
            },
            valueRange = minFloor..maxFloor,
            steps = availableFloors.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Floor labels below the slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (floor in availableFloors) {
                Text(
                    text = formatFloorLabel(floor),
                    fontSize = 12.sp,
                    fontWeight = if (floor == currentFloor) FontWeight.Bold else FontWeight.Normal,
                    color = if (floor == currentFloor) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Formats floor number for display.
 * Shows "F1", "F1.5", "F2", etc.
 */
private fun formatFloorLabel(floorNumber: Float): String {
    return if (floorNumber == floorNumber.toInt().toFloat()) {
        "F${floorNumber.toInt()}"
    } else {
        "F$floorNumber"
    }
}
