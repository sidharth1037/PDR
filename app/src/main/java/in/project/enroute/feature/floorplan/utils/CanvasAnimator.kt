package `in`.project.enroute.feature.floorplan.utils

import `in`.project.enroute.feature.floorplan.rendering.CanvasState
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Configuration for canvas animation behavior.
 * Allows customization of animation duration and easing.
 *
 * @param durationMs Total duration of the animation in milliseconds
 * @param frameDelayMs Delay between animation frames in milliseconds (affects smoothness)
 */
data class CanvasAnimationConfig(
    val durationMs: Long = 500L,
    val frameDelayMs: Long = 16L // ~60fps
)

/**
 * Represents a target position and zoom for the canvas.
 *
 * @param x The x coordinate in canvas/world space to center on
 * @param y The y coordinate in canvas/world space to center on
 * @param scale The target zoom scale
 */
data class CanvasTarget(
    val x: Float,
    val y: Float,
    val scale: Float
)

/**
 * Utility class for animating canvas transformations.
 * Provides smooth animated transitions for centering on coordinates with zoom.
 */
object CanvasAnimator {
    
    /**
     * Calculates the canvas offset needed to center a world coordinate on screen.
     * Takes both canvas rotation and floor plan transformations into account.
     *
     * The FloorPlanCanvas applies transformations in this order:
     * 1. Raw floor plan coordinates are scaled by floorPlanScale
     * 2. Rotated by floorPlanRotation
     * 3. Drawn at (transformed_x + centerX, transformed_y + centerY) due to inner translate
     * 4. Canvas graphicsLayer applies: scale -> rotate -> translate around origin (0,0)
     *
     * @param targetX The x coordinate in raw floor plan space to center
     * @param targetY The y coordinate in raw floor plan space to center
     * @param canvasScale The canvas zoom scale
     * @param canvasRotation The canvas rotation in degrees
     * @param floorPlanScale The floor plan's metadata scale factor
     * @param floorPlanRotation The floor plan's metadata rotation in degrees
     * @param screenWidth The screen width in pixels
     * @param screenHeight The screen height in pixels
     * @return Pair of (offsetX, offsetY) needed to center the target
     */
    fun calculateCenterOffset(
        targetX: Float,
        targetY: Float,
        canvasScale: Float,
        canvasRotation: Float,
        floorPlanScale: Float,
        floorPlanRotation: Float,
        screenWidth: Float,
        screenHeight: Float
    ): Pair<Float, Float> {
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        
        // Step 1: Apply floor plan transformations (scale then rotate)
        val fpScaledX = targetX * floorPlanScale
        val fpScaledY = targetY * floorPlanScale
        
        val fpRotationRad = Math.toRadians(floorPlanRotation.toDouble())
        val fpCos = cos(fpRotationRad).toFloat()
        val fpSin = sin(fpRotationRad).toFloat()
        
        val fpTransformedX = fpScaledX * fpCos - fpScaledY * fpSin
        val fpTransformedY = fpScaledX * fpSin + fpScaledY * fpCos
        
        // Step 2: Add inner translate offset (drawing origin shift to center)
        val localX = fpTransformedX + centerX
        val localY = fpTransformedY + centerY
        
        // Step 3: Apply canvas graphicsLayer transforms (scale -> rotate -> translate)
        val canvasRotationRad = Math.toRadians(canvasRotation.toDouble())
        val canvasCos = cos(canvasRotationRad).toFloat()
        val canvasSin = sin(canvasRotationRad).toFloat()
        
        // Canvas scale around origin (0, 0)
        val canvasScaledX = localX * canvasScale
        val canvasScaledY = localY * canvasScale
        
        // Canvas rotation around origin (0, 0)
        val canvasRotatedX = canvasScaledX * canvasCos - canvasScaledY * canvasSin
        val canvasRotatedY = canvasScaledX * canvasSin + canvasScaledY * canvasCos
        
        // Step 4: Calculate offset to center the point on screen
        val offsetX = centerX - canvasRotatedX
        val offsetY = centerY - canvasRotatedY
        
        return Pair(offsetX, offsetY)
    }
    
    /**
     * Generates interpolated canvas states for smooth animation.
     * Uses ease-out cubic easing for natural deceleration.
     *
     * @param startState The starting canvas state
     * @param targetState The target canvas state
     * @param progress Animation progress from 0.0 to 1.0
     * @return Interpolated CanvasState
     */
    fun interpolate(
        startState: CanvasState,
        targetState: CanvasState,
        progress: Float
    ): CanvasState {
        // Ease-out cubic for smooth deceleration
        val easedProgress = easeOutCubic(progress)
        
        return CanvasState(
            scale = lerp(startState.scale, targetState.scale, easedProgress),
            offsetX = lerp(startState.offsetX, targetState.offsetX, easedProgress),
            offsetY = lerp(startState.offsetY, targetState.offsetY, easedProgress),
            rotation = lerp(startState.rotation, targetState.rotation, easedProgress)
        )
    }
    
    /**
     * Animates the canvas from current state to center on a target coordinate.
     * This is a suspend function that should be called from a coroutine.
     *
     * @param currentState The current canvas state
     * @param target The target position and scale
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @param config Animation configuration
     * @param onStateUpdate Callback for each animation frame with the new state
     */
    suspend fun animateToTarget(
        currentState: CanvasState,
        target: CanvasTarget,
        floorPlanScale: Float,
        floorPlanRotation: Float,
        screenWidth: Float,
        screenHeight: Float,
        config: CanvasAnimationConfig = CanvasAnimationConfig(),
        onStateUpdate: (CanvasState) -> Unit
    ) {
        val (targetOffsetX, targetOffsetY) = calculateCenterOffset(
            targetX = target.x,
            targetY = target.y,
            canvasScale = target.scale,
            canvasRotation = currentState.rotation,
            floorPlanScale = floorPlanScale,
            floorPlanRotation = floorPlanRotation,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        
        val targetState = CanvasState(
            scale = target.scale,
            offsetX = targetOffsetX,
            offsetY = targetOffsetY,
            rotation = currentState.rotation // Keep current rotation
        )
        
        val startTime = System.currentTimeMillis()
        var elapsed = 0L
        
        while (elapsed < config.durationMs) {
            val progress = (elapsed.toFloat() / config.durationMs).coerceIn(0f, 1f)
            val interpolatedState = interpolate(currentState, targetState, progress)
            onStateUpdate(interpolatedState)
            
            delay(config.frameDelayMs)
            elapsed = System.currentTimeMillis() - startTime
        }
        
        // Ensure we end exactly at the target
        onStateUpdate(targetState)
    }
    
    /**
     * Linear interpolation between two values.
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
    
    /**
     * Ease-out cubic easing function.
     * Starts fast and decelerates towards the end.
     */
    private fun easeOutCubic(t: Float): Float {
        val t1 = t - 1f
        return t1 * t1 * t1 + 1f
    }
    
    /**
     * Ease-in-out cubic easing function.
     * Smooth acceleration and deceleration.
     */
    @Suppress("unused")
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            val t1 = -2f * t + 2f
            1f - (t1 * t1 * t1) / 2f
        }
    }
}
