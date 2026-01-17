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
    val floorPlanData: FloorPlanData? = null,
    val canvasState: CanvasState = CanvasState(),
    val displayConfig: FloorPlanDisplayConfig = FloorPlanDisplayConfig()
)

/**
 * ViewModel for floor plan rendering.
 * Manages floor plan data loading and canvas state.
 */
class FloorPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Repository can be swapped for remote implementation later
    private val repository: FloorPlanRepository = LocalFloorPlanRepository(application)

    private val _uiState = MutableStateFlow(FloorPlanUiState())
    val uiState: StateFlow<FloorPlanUiState> = _uiState.asStateFlow()

    /**
     * Loads floor plan data for the specified floor.
     */
    fun loadFloorPlan(floorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val floorPlanData = repository.loadFloorPlan(floorId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        floorPlanData = floorPlanData
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load floor plan"
                    )
                }
            }
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