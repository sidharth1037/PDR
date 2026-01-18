package `in`.project.enroute.data.model

/**
 * Represents a boundary polygon that defines a section of the floor plan boundary.
 * A floor can have multiple boundary polygons (e.g., separate building sections).
 */
data class BoundaryPolygon(
    val name: String,
    val points: List<BoundaryPoint>
)
