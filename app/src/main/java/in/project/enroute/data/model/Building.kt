package `in`.project.enroute.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a building within an institution.
 * Each building has multiple floors and its own boundary polygon for visibility detection.
 */
data class Building(
    @SerializedName("building_id")
    val buildingId: String,
    
    @SerializedName("building_name")
    val buildingName: String,
    
    /**
     * Available floor IDs for this building (e.g., ["floor_1", "floor_1.5", "floor_2"])
     */
    @SerializedName("available_floors")
    val availableFloors: List<String> = emptyList(),
    
    /**
     * Scale factor for rendering this building
     */
    val scale: Float = 1f,
    
    /**
     * Rotation in degrees for rendering this building
     */
    val rotation: Float = 0f,
    
    /**
     * Position where the building label should be displayed
     */
    @SerializedName("label_position")
    val labelPosition: LabelPosition? = null
)
