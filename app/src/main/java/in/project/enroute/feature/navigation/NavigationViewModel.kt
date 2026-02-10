package `in`.project.enroute.feature.navigation

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.project.enroute.data.model.Entrance
import `in`.project.enroute.data.model.FloorPlanData
import `in`.project.enroute.data.model.Room
import `in`.project.enroute.data.model.Wall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * UI state for the navigation / pathfinding feature.
 *
 * @param path The computed A* path as world-coordinate waypoints. Empty when no path is active.
 * @param isCalculating True while the pathfinding coroutine is running.
 * @param targetRoom The room the user requested directions to.
 * @param targetEntrance The entrance matched for the target room.
 * @param error Human-readable error message, or null.
 */
data class NavigationUiState(
    val path: List<Offset> = emptyList(),
    val isCalculating: Boolean = false,
    val targetRoom: Room? = null,
    val targetEntrance: Entrance? = null,
    val error: String? = null
)

/**
 * ViewModel for A* pathfinding between the user's PDR position and a room entrance.
 *
 * Lifecycle:
 *  1. [supplyFloorData] is called once floors are loaded so the VM has walls & entrances.
 *  2. When the user taps "Directions" on a pinned room, [requestDirections] is called.
 *  3. The path result is emitted via [uiState].
 *  4. [clearPath] resets the state.
 */
class NavigationViewModel : ViewModel() {

    companion object {
        private const val TAG = "NavigationViewModel"
    }

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    /**
     * Cached floor data keyed by floorId. Populated by [supplyFloorData].
     * Contains walls and entrances needed for pathfinding.
     */
    private val floorDataMap = mutableMapOf<String, FloorPlanData>()

    /**
     * Lazily-created NavigationRepository per floor (expensive init: distance transform).
     * Keyed by floorId.
     */
    private val repositoryCache = mutableMapOf<String, NavigationRepository>()

    /** Running pathfinding job – cancelled if a new request comes in. */
    private var pathfindingJob: Job? = null

    // ──────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────

    /**
     * Provides floor plan data so pathfinding can work.
     * Call this whenever new floors are loaded (e.g. from FloorPlanViewModel).
     */
    fun supplyFloorData(floors: List<FloorPlanData>) {
        floors.forEach { floor ->
            floorDataMap[floor.floorId] = floor
        }
    }

    /**
     * Requests a path from the user's current position to the entrance of [room].
     *
     * @param room The destination room (must have name or number to match an entrance).
     * @param userPosition The user's current world-coordinate position (from PDR).
     * @param currentFloor The floorId the user is currently on (e.g. "floor_1").
     */
    fun requestDirections(
        room: Room,
        userPosition: Offset,
        currentFloor: String
    ) {
        // Cancel any in-flight computation
        pathfindingJob?.cancel()

        val floorData = floorDataMap[currentFloor]
        if (floorData == null) {
            _uiState.update {
                it.copy(
                    error = "Floor data not loaded for $currentFloor",
                    isCalculating = false,
                    targetRoom = room
                )
            }
            Log.e(TAG, "Floor data missing for $currentFloor")
            return
        }

        // Find the entrance that matches this room
        val entrance = findEntranceForRoom(room, floorData.entrances)
        if (entrance == null) {
            _uiState.update {
                it.copy(
                    error = "No entrance found for room ${room.name ?: room.number}",
                    isCalculating = false,
                    targetRoom = room
                )
            }
            Log.e(TAG, "No entrance matched for room: name=${room.name}, number=${room.number}")
            return
        }

        Log.d(TAG, "Matched entrance id=${entrance.id} at (${entrance.x}, ${entrance.y}) for room ${room.name ?: room.number}")

        _uiState.update {
            it.copy(
                isCalculating = true,
                error = null,
                targetRoom = room,
                targetEntrance = entrance,
                path = emptyList()
            )
        }

        pathfindingJob = viewModelScope.launch {
            val metadata = floorData.metadata

            // PDR positions are in metadata-transformed space (scale + rotate).
            // NavigationRepository works in raw floor plan coordinates.
            // Convert user position → raw for pathfinding.
            val rawStart = metadataToRaw(userPosition, metadata.scale, metadata.rotation)
            val rawGoal = Offset(entrance.x, entrance.y) // already raw

            Log.d(TAG, "Coordinate transform: metadata(${userPosition.x}, ${userPosition.y}) → raw(${rawStart.x}, ${rawStart.y})")

            val rawPath = computePath(rawStart, rawGoal, floorData.walls, currentFloor)

            // Convert path from raw space → metadata-transformed space for rendering
            val path = rawPath.map { rawToMetadata(it, metadata.scale, metadata.rotation) }

            _uiState.update {
                if (path.isEmpty()) {
                    it.copy(
                        isCalculating = false,
                        error = "Could not find a path"
                    )
                } else {
                    it.copy(
                        isCalculating = false,
                        path = path
                    )
                }
            }
        }
    }

    /**
     * Clears the current path and resets navigation state.
     */
    fun clearPath() {
        pathfindingJob?.cancel()
        _uiState.update { NavigationUiState() }
    }

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    /**
     * Runs A* on a background dispatcher. The NavigationRepository for the
     * floor is created lazily and cached (distance transform is expensive).
     */
    private suspend fun computePath(
        start: Offset,
        goal: Offset,
        walls: List<Wall>,
        floorId: String
    ): List<Offset> = withContext(Dispatchers.Default) {
        val repo = repositoryCache.getOrPut(floorId) {
            Log.d(TAG, "Building distance transform for $floorId (${walls.size} walls)…")
            NavigationRepository(walls)
        }
        repo.findPath(start, goal)
    }

    /**
     * Finds the entrance that corresponds to [room] on the same floor.
     * Matching priority:
     *  1. roomNo matches room.number (both converted to string)
     *  2. name matches room.name (case-insensitive)
     */
    private fun findEntranceForRoom(room: Room, entrances: List<Entrance>): Entrance? {
        // Try matching by room number first (most reliable)
        if (room.number != null) {
            val byNumber = entrances.firstOrNull { entrance ->
                entrance.roomNo != null && entrance.roomNo == room.number.toString()
            }
            if (byNumber != null) return byNumber
        }

        // Fall back to name matching (case-insensitive)
        if (room.name != null) {
            val byName = entrances.firstOrNull { entrance ->
                entrance.name != null && entrance.name.equals(room.name, ignoreCase = true)
            }
            if (byName != null) return byName
        }

        return null
    }

    // ──────────────────────────────────────────────
    // Coordinate space transforms
    // ──────────────────────────────────────────────

    /**
     * Transforms a point from raw floor plan coordinates to metadata-transformed
     * coordinates (the space renderers draw in). Forward: scale → rotate.
     */
    private fun rawToMetadata(point: Offset, scale: Float, rotationDegrees: Float): Offset {
        val x = point.x * scale
        val y = point.y * scale
        val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Offset(x * cosA - y * sinA, x * sinA + y * cosA)
    }

    /**
     * Transforms a point from metadata-transformed coordinates back to raw
     * floor plan coordinates. Inverse: undo rotation → undo scale.
     */
    private fun metadataToRaw(point: Offset, scale: Float, rotationDegrees: Float): Offset {
        val angleRad = Math.toRadians(-rotationDegrees.toDouble()).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val unrotatedX = point.x * cosA - point.y * sinA
        val unrotatedY = point.x * sinA + point.y * cosA
        return Offset(unrotatedX / scale, unrotatedY / scale)
    }

    override fun onCleared() {
        super.onCleared()
        pathfindingJob?.cancel()
    }
}
