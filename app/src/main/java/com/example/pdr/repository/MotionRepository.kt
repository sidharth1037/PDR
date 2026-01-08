package com.example.pdr.repository

import android.app.Application
import com.example.pdr.model.MotionClassifier
import com.example.pdr.model.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Repository for motion classification using ML model.
 * Handles sensor data buffering and inference.
 * Emits results via StateFlow.
 *
 * STATEFLOW CONCEPTS:
 * - Using StateFlow for asynchronous events (ML inference happens on background thread)
 * - .launch(Dispatchers.Main) ensures StateFlow updates happen on main thread
 * - This is safe because Compose only reads on main thread anyway
 */
class MotionRepository(application: Application) {

    private val motionClassifier = MotionClassifier(application)
    private val sensorDataBuffer = mutableListOf<FloatArray>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // StateFlow for motion events
    private val _motionEvents = MutableStateFlow<MotionEvent?>(null)
    val motionEvents: StateFlow<MotionEvent?> = _motionEvents.asStateFlow()

    // ===== SMOOTHING / NOISE REDUCTION =====
    // SMOOTHING DISABLED - showing raw model output directly
    // // Keep recent predictions to smooth out noisy model output
    // // Majority voting prevents false positives (e.g., 1 second of misclassification in 10 second walk)
    // private val recentPredictions = mutableListOf<String>()
    // private val smoothingWindowSize = 3  // Use last 5 predictions for majority vote
    
    private var lastEmittedMotionType: String? = null
    private var lastEmittedConfidence = 0f

    /**
     * Processes incoming accelerometer data for motion classification.
     * 
     * // SMOOTHING DISABLED - now emitting raw model output directly
     * // Previously used SMOOTHING STRATEGY:
     * // - Each inference produces a raw classification
     * // - We added it to a sliding window of recent predictions
     * // - We emitted the MAJORITY VOTE of recent predictions
     * // - This reduced noise while maintaining responsive transitions (~1-2 seconds)
     * 
     * THREAD SAFETY & STATEFLOW:
     * - Background thread: collects sensor data and runs ML inference
     * - Main thread: emits to StateFlow (Compose only reads on main thread)
     * - This is the correct pattern for background work + UI updates
     */
    fun onSensorDataReceived(accX: Float, accY: Float, accZ: Float) {
        val accMag = sqrt(accX * accX + accY * accY + accZ * accZ)
        val accelerometerData = floatArrayOf(accX, accY, accZ, accMag)

        sensorDataBuffer.add(accelerometerData)

        // Check if buffer is full - trigger inference
        if (sensorDataBuffer.size >= motionClassifier.meta.windowSize) {
            val bufferArray = sensorDataBuffer.toTypedArray()
            
            // Run inference on background thread
            scope.launch {
                val prediction = motionClassifier.predict(bufferArray)
                
                // Find the class with highest confidence
                val predictedIndex = prediction.indices.maxByOrNull { prediction[it] } ?: -1
                
                if (predictedIndex != -1) {
                    val motionTypeString = motionClassifier.meta.classNames[predictedIndex]
                    
                    // Use original class names directly: "walking", "upstairs", "downstairs", "idle"
                    val rawMotionType = motionTypeString.lowercase()
                    
                    // Direct raw output
                    val rawConfidence = prediction[predictedIndex]
                    
                    // Emit directly without smoothing
                    // Reduce UI flicker by only emitting on significant changes
                    if (rawMotionType != lastEmittedMotionType || 
                        (abs(rawConfidence - lastEmittedConfidence) > 0.05f)) {
                        
                        scope.launch(Dispatchers.Main) {
                            val event = MotionEvent(
                                classificationName = rawMotionType,
                                confidence = rawConfidence
                            )
                            _motionEvents.value = event
                            lastEmittedMotionType = rawMotionType
                            lastEmittedConfidence = rawConfidence
                        }
                    }
                }
            }

            // Implement sliding window: remove oldest data points
            for (i in 0 until motionClassifier.meta.stepSize) {
                if (sensorDataBuffer.isNotEmpty()) {
                    sensorDataBuffer.removeAt(0)
                }
            }
        }
    }

    /**
     * Gets the most common motion type in the recent predictions window.
     * This smooths out temporary misclassifications.
     * 
     * DISABLED - smoothing is turned off
     * 
     * Example: [WALKING, WALKING, IDLE, WALKING, WALKING] → WALKING
     * The single IDLE misclassification is ignored.
     */
    // private fun getMajorityVoteMotionType(): MotionType {
    //     if (recentPredictions.isEmpty()) return MotionType.UNKNOWN
    //     
    //     // Count occurrences of each motion type
    //     val counts = recentPredictions.groupingBy { it }.eachCount()
    //     
    //     // Return the motion type with highest count
    //     return counts.maxByOrNull { it.value }?.key ?: MotionType.UNKNOWN
    // }

    /**
     * Computes weighted average confidence for the smoothed motion type.
     * Recent predictions are weighted more heavily than older ones.
     * 
     * DISABLED - smoothing is turned off
     * 
     * This gives higher confidence when the model is consistently confident,
     * and lower confidence during transitions when model is uncertain.
     */
    // private fun getWeightedAverageConfidence(prediction: FloatArray): Float {
    //     if (recentPredictions.isEmpty()) return 0f
    //     
    //     // Get the smoothed motion type
    //     val smoothedType = getMajorityVoteMotionType()
    //     
    //     // Find index of smoothed type in model's class names
    //     val typeIndex = when (smoothedType) {
    //         MotionType.WALKING -> motionClassifier.meta.classNames.indexOf("walking")
    //         MotionType.STAIR_ASCENT -> motionClassifier.meta.classNames.indexOf("upstairs")
    //         MotionType.STAIR_DESCENT -> motionClassifier.meta.classNames.indexOf("downstairs")
    //         MotionType.STATIONARY -> motionClassifier.meta.classNames.indexOf("idle")
    //         MotionType.UNKNOWN -> -1
    //     }
    //     
    //     if (typeIndex == -1 || typeIndex >= prediction.size) return 0f
    //     
    //     // Use the model's confidence for this type from latest prediction
    //     return prediction[typeIndex].coerceIn(0f, 1f)
    // }

    /**
     * Adjusts smoothing sensitivity. 
     * DISABLED - smoothing is turned off
     * 
     * Larger = smoother but slower to respond to transitions
     * Smaller = faster response but noisier output
     * 
     * Recommended: 3-7 (3=responsive, 7=smooth)
     */

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        motionClassifier.close()
    }
}

