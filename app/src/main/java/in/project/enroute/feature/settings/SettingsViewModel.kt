package `in`.project.enroute.feature.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val isLoading: Boolean = false,
    val currentHeight: Float = 170f,
    val isEditingHeight: Boolean = false,
    val heightInputValue: String = ""
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(currentHeight = 170f, heightInputValue = "170"))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleEditHeight() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isEditingHeight = !currentState.isEditingHeight,
            heightInputValue = if (!currentState.isEditingHeight) currentState.currentHeight.toString() else ""
        )
    }

    fun updateHeightInput(value: String) {
        _uiState.value = _uiState.value.copy(heightInputValue = value)
    }

    fun saveHeight() {
        val currentState = _uiState.value
        val newHeight = currentState.heightInputValue.toFloatOrNull() ?: currentState.currentHeight
        _uiState.value = currentState.copy(
            currentHeight = newHeight,
            isEditingHeight = false,
            heightInputValue = ""
        )
    }
}
