package com.example.pdr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pdr.viewmodel.FloorPlanViewModel
import com.example.pdr.viewmodel.StepViewModel

/**
 * The settings screen, which allows the user to configure the PDR algorithm and clear the path.
 */
@Composable
fun SettingsScreen(stepViewModel: StepViewModel, floorPlanViewModel: FloorPlanViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // Text field for the user to input their height.
        OutlinedTextField(
            value = stepViewModel.height,
            onValueChange = { stepViewModel.height = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(0.8f)
        )

        // --- UI Control --
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Show Floor Plan", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = floorPlanViewModel.showFloorPlan,
                onCheckedChange = { floorPlanViewModel.showFloorPlan = it },
                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Show Point Numbers", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = floorPlanViewModel.showPointNumbers,
                onCheckedChange = { floorPlanViewModel.showPointNumbers = it },
                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Show Entrances", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = floorPlanViewModel.showEntrances,
                onCheckedChange = { floorPlanViewModel.showEntrances = it },
                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Show Room Labels", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = floorPlanViewModel.showRoomLabels,
                onCheckedChange = { floorPlanViewModel.showRoomLabels = it },
                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
            )
        }

        OutlinedTextField(
            value = floorPlanViewModel.floorPlanScale,
            onValueChange = { floorPlanViewModel.floorPlanScale = it },
            label = { Text("Floor Plan Scale") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(0.8f)
        )

        OutlinedTextField(
            value = floorPlanViewModel.floorPlanRotation,
            onValueChange = { floorPlanViewModel.floorPlanRotation = it },
            label = { Text("Floor Plan Rotation (°)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(0.8f)
        )

        // --- Stride Calculation Parameters --
        Text("K (Frequency Factor): ${"%.2f".format(stepViewModel.kValue)}", style = MaterialTheme.typography.labelSmall)
        Text("Controls how much stride length increases with speed.", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.kValue,
            onValueChange = { stepViewModel.kValue = it },
            valueRange = 0.1f..1.0f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Text("C (Base Stride Factor): ${"%.2f".format(stepViewModel.cValue)}", style = MaterialTheme.typography.labelSmall)
        Text("Determines base stride length as a percent of height.", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.cValue,
            onValueChange = { stepViewModel.cValue = it },
            valueRange = 0.05f..0.5f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        // --- Step Detection Parameters --
        Text("Threshold: ${"%.1f".format(stepViewModel.threshold)}", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.threshold,
            onValueChange = { stepViewModel.threshold = it },
            valueRange = 5f..20f,
            steps = ((20f - 5f) / 0.2f - 1).toInt(),
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Text("Window Size: ${stepViewModel.windowSize.toInt()}", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.windowSize,
            onValueChange = { stepViewModel.windowSize = it },
            valueRange = 1f..20f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Text("Debounce (ms): ${stepViewModel.debounce.toInt()}", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.debounce,
            onValueChange = { stepViewModel.debounce = it },
            valueRange = 100f..600f,
            steps = ((600f - 100f) / 20f - 1).toInt(),
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Text("Cadence Average Size: ${stepViewModel.cadenceAverageSize.toInt()}", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = stepViewModel.cadenceAverageSize,
            onValueChange = { stepViewModel.cadenceAverageSize = it },
            valueRange = 1f..20f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}