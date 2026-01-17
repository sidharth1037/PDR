package `in`.project.enroute.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a room with a center point for label display.
 */
data class Room(
    val id: Int,
    val x: Float,
    val y: Float,
    val name: String? = null,
    @SerializedName("point_ids")
    val pointIds: List<Int> = emptyList()
)
