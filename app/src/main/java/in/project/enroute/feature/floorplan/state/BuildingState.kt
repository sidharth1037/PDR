package `in`.project.enroute.feature.floorplan.state

import `in`.project.enroute.data.model.Building
import `in`.project.enroute.data.model.FloorPlanData

/**
 * Represents the state of a single building including its floor data and current floor selection.
 * Each building maintains its own floor state independently.
 */
data class BuildingState(
    /**
     * The building configuration
     */
    val building: Building,
    
    /**
     * Map of floor number to floor plan data for this building
     */
    val floors: Map<Float, FloorPlanData> = emptyMap(),
    
    /**
     * Sorted list of available floor numbers for this building
     */
    val availableFloorNumbers: List<Float> = emptyList(),
    
    /**
     * Currently selected floor number for this building
     */
    val currentFloorNumber: Float = 1f
) {
    /**
     * Returns all floors from bottom up to and including the current floor.
     * Used for rendering floors stacked with proper masking.
     */
    val floorsToRender: List<FloorPlanData>
        get() = availableFloorNumbers
            .filter { it <= currentFloorNumber }
            .sorted()
            .mapNotNull { floors[it] }
    
    /**
     * Returns the current floor's data.
     */
    val currentFloorData: FloorPlanData?
        get() = floors[currentFloorNumber]
    
    /**
     * Minimum floor number available.
     */
    val minFloor: Float
        get() = availableFloorNumbers.minOrNull() ?: 1f
    
    /**
     * Maximum floor number available.
     */
    val maxFloor: Float
        get() = availableFloorNumbers.maxOrNull() ?: 1f
    
    /**
     * Checks if this building has multiple floors (slider should be shown)
     */
    val hasMultipleFloors: Boolean
        get() = availableFloorNumbers.size > 1
}
