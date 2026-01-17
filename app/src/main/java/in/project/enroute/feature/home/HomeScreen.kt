package `in`.project.enroute.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.project.enroute.feature.floorplan.FloorPlanViewModel
import `in`.project.enroute.feature.floorplan.rendering.FloorPlanCanvas

@Composable
fun HomeScreen(
    floorPlanViewModel: FloorPlanViewModel = viewModel()
) {
    val uiState by floorPlanViewModel.uiState.collectAsState()

    // Load floor plan on first composition
    LaunchedEffect(Unit) {
        floorPlanViewModel.loadFloorPlan("floor_1")
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                Text(text = uiState.error ?: "Unknown error")
            }
            uiState.floorPlanData != null -> {
                FloorPlanCanvas(
                    floorPlanData = uiState.floorPlanData!!,
                    canvasState = uiState.canvasState,
                    onCanvasStateChange = { floorPlanViewModel.updateCanvasState(it) },
                    displayConfig = uiState.displayConfig,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
