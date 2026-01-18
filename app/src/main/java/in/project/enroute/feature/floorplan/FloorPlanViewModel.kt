package `in`.project.enroute.feature.floorplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.project.enroute.data.model.FloorPlanData
import `in`.project.enroute.data.repository.FloorPlanRepository
import `in`.project.enroute.data.repository.LocalFloorPlanRepository
import `in`.project.enroute.feature.floorplan.rendering.CanvasState
import `in`.project.enroute.feature.floorplan.rendering.FloorPlanDisplayConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the floor plan feature.
 */
data class FloorPlanUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allFloors: Map<Float, FloorPlanData> = emptyMap(),
    val availableFloorNumbers: List<Float> = emptyList(),
    val currentFloorNumber: Float = 1f,
    val canvasState: CanvasState = CanvasState(),
    val displayConfig: FloorPlanDisplayConfig = FloorPlanDisplayConfig()
) {
    /**
     * Returns all floors from bottom up to and including the current floor.
     * Used for rendering floors stacked with proper masking.
     */
    val floorsToRender: List<FloorPlanData>
        get() = availableFloorNumbers
            .filter { it <= currentFloorNumber }
            .sorted()
            .mapNotNull { allFloors[it] }

    /**
     * Returns the current floor's data.
     */
    val currentFloorData: FloorPlanData?
        get() = allFloors[currentFloorNumber]

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
}

/**
 * ViewModel for floor plan rendering.
 * Manages floor plan data loading and canvas state.
 * Supports multiple floors with stacked rendering.
 */
class FloorPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Repository can be swapped for remote implementation later
    private val repository: FloorPlanRepository = LocalFloorPlanRepository(application)

    private val _uiState = MutableStateFlow(FloorPlanUiState())
    val uiState: StateFlow<FloorPlanUiState> = _uiState.asStateFlow()

    /**
     * Loads all floors for a building.
     * @param buildingId The building identifier (e.g., "building_1")
     * @param floorIds List of floor IDs to load (e.g., ["floor_1", "floor_1.5", "floor_2"])
     */
    fun loadAllFloors(buildingId: String, floorIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val floorsMap = mutableMapOf<Float, FloorPlanData>()
                val floorNumbers = mutableListOf<Float>()

                for (floorId in floorIds) {
                    val floorPlanData = repository.loadFloorPlan(buildingId, floorId)
                    val floorNumber = extractFloorNumber(floorId)
                    floorsMap[floorNumber] = floorPlanData
                    floorNumbers.add(floorNumber)
                }

                val sortedFloorNumbers = floorNumbers.sorted()
                val highestFloor = sortedFloorNumbers.lastOrNull() ?: 1f

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allFloors = floorsMap,
                        availableFloorNumbers = sortedFloorNumbers,
                        currentFloorNumber = highestFloor
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load floor plans"
                    )
                }
            }
        }
    }

    /**
     * Loads floor plan data for a single floor (legacy support).
     */
    fun loadFloorPlan(buildingId: String, floorId: String) {
        loadAllFloors(buildingId, listOf(floorId))
    }

    /**
     * Extracts the floor number from floor ID (e.g., "floor_1.5" -> 1.5f)
     */
    private fun extractFloorNumber(floorId: String): Float {
        return floorId.removePrefix("floor_").toFloatOrNull() ?: 1f
    }

    /**
     * Changes the current floor to the specified floor number.
     */
    fun setCurrentFloor(floorNumber: Float) {
        val available = _uiState.value.availableFloorNumbers
        if (floorNumber in available) {
            _uiState.update { it.copy(currentFloorNumber = floorNumber) }
        }
    }

    /**
     * Moves to the next higher floor if available.
     */
    fun goToNextFloor() {
        val current = _uiState.value.currentFloorNumber
        val available = _uiState.value.availableFloorNumbers.sorted()
        val currentIndex = available.indexOf(current)
        if (currentIndex >= 0 && currentIndex < available.size - 1) {
            _uiState.update { it.copy(currentFloorNumber = available[currentIndex + 1]) }
        }
    }

    /**
     * Moves to the next lower floor if available.
     */
    fun goToPreviousFloor() {
        val current = _uiState.value.currentFloorNumber
        val available = _uiState.value.availableFloorNumbers.sorted()
        val currentIndex = available.indexOf(current)
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentFloorNumber = available[currentIndex - 1]) }
        }
    }

    /**
     * Updates canvas state from gesture input.
     */
    fun updateCanvasState(canvasState: CanvasState) {
        _uiState.update { it.copy(canvasState = canvasState) }
    }

    /**
     * Updates display configuration (toggle visibility, colors, etc.)
     */
    fun updateDisplayConfig(displayConfig: FloorPlanDisplayConfig) {
        _uiState.update { it.copy(displayConfig = displayConfig) }
    }

    // Convenience methods for toggling individual display options

    fun toggleWalls() {
        _uiState.update {
            it.copy(displayConfig = it.displayConfig.copy(showWalls = !it.displayConfig.showWalls))
        }
    }

    fun toggleStairwells() {
        _uiState.update {
            it.copy(displayConfig = it.displayConfig.copy(showStairwells = !it.displayConfig.showStairwells))
        }
    }

    fun toggleEntrances() {
        _uiState.update {
            it.copy(displayConfig = it.displayConfig.copy(showEntrances = !it.displayConfig.showEntrances))
        }
    }

    fun toggleRoomLabels() {
        _uiState.update {
            it.copy(displayConfig = it.displayConfig.copy(showRoomLabels = !it.displayConfig.showRoomLabels))
        }
    }

    /**
     * Resets canvas to initial state.
     */
    fun resetCanvas() {
        _uiState.update { it.copy(canvasState = CanvasState()) }
    }
}