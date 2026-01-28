package `in`.project.enroute.data.model

/**
 * Represents a complete stairwell polygon, built from grouped StairLines.
 * Points are ordered for proper polygon filling.
 */
data class Stairwell(
    val polygonId: Int,
    val points: List<Pair<Float, Float>>,
    val floorsConnected: List<Float> = emptyList(),
    // Distinct positions present on the stair lines (e.g. "top", "bottom")
    val positions: List<String> = emptyList()
    ,
    // Original stair lines that formed this polygon. Keeping these allows renderers
    // to detect which edges are marked "top" / "bottom" and which are side edges.
    val lines: List<StairLine> = emptyList()
)
