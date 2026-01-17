package `in`.project.enroute.data.model

/**
 * Represents a wall segment in the floor plan.
 * Coordinates are in the floor plan's coordinate system.
 */
data class Wall(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)
