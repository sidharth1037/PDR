package `in`.project.enroute.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents an entrance point in the floor plan.
 * Can be a regular room entrance or a stairwell entry/exit.
 */
data class Entrance(
    val id: Int,
    val x: Float,
    val y: Float,
    val name: String? = null,
    @SerializedName("room_no")
    val roomNo: String? = null,
    val stairs: Boolean = false,
    val available: Boolean = true,
    /** Floor this entrance belongs to (e.g. "floor_1"). Set after loading, not from JSON. */
    @Transient
    val floorId: String? = null
)
