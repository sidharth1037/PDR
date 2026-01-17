package `in`.project.enroute.data.repository

import `in`.project.enroute.data.model.FloorPlanData

/**
 * Repository interface for loading floor plan data.
 * Abstracts the data source - can be local assets or remote backend.
 */
interface FloorPlanRepository {
    
    /**
     * Loads complete floor plan data for the specified floor.
     * @param floorId Identifier for the floor (e.g., "floor_1")
     * @return FloorPlanData containing walls, stairwells, entrances, and rooms
     */
    suspend fun loadFloorPlan(floorId: String): FloorPlanData
    
    /**
     * Gets list of available floor IDs.
     * @return List of floor identifiers
     */
    suspend fun getAvailableFloors(): List<String>
}
