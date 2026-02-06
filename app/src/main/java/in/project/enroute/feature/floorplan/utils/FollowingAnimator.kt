package `in`.project.enroute.feature.floorplan.utils

import androidx.compose.ui.geometry.Offset
import `in`.project.enroute.feature.floorplan.rendering.CanvasState
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Configuration for following mode behavior.
 *
 * @param scale The zoom level to use in following mode
 * @param animationDurationMs Duration for smooth animation transitions
 * @param frameDelayMs Delay between animation frames (~60fps)
 */
data class FollowingConfig(
    val scale: Float = 0.7f,
    val animationDurationMs: Long = 650L,
    val frameDelayMs: Long = 12L
)

/**
 * Utility for "follow me" following mode, similar to Google Maps navigation.
 * 
 * In following mode:
 * - The view centers on the user's current position
 * - The canvas rotates so the user's heading always points up (north = top of screen)
 * - Updates smoothly as the user walks
 *
 * Key difference from CanvasAnimator: This works with PDR path coordinates
 * which are in "inner canvas space" (after the center translate but before
 * graphicsLayer transforms). Floor plan metadata transforms are NOT applied.
 */
object FollowingAnimator {
    
    /**
     * Calculates the canvas state to center on a world position with heading pointing up.
     *
     * The coordinate system:
     * - PDR path points are in "world space" relative to the canvas center
     * - graphicsLayer applies: scale -> rotate -> translate (with TransformOrigin(0,0))
     * - Inner translate(centerX, centerY) shifts drawing origin to screen center
     *
     * To center a world point on screen with heading pointing up:
     * 1. Calculate where the point ends up after all transforms
     * 2. Set offset so that position equals screen center
     * 3. Set rotation so heading (radians, 0=north, clockwise) points up
     *
     * @param worldPosition The position to center on (in world/PDR coordinates)
     * @param headingRadians The heading direction in radians (0 = north, positive = clockwise)
     * @param scale The target zoom scale
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return CanvasState that centers the position with heading pointing up
     */
    fun calculateFollowingState(
        worldPosition: Offset,
        headingRadians: Float,
        scale: Float,
        screenWidth: Float,
        screenHeight: Float
    ): CanvasState {
        // Convert heading to canvas rotation (negative to make heading point up)
        // Heading: 0 = north (up), positive = clockwise
        // Canvas rotation: positive = clockwise
        // To make heading point up, rotate canvas by -heading
        val rotationDegrees = -Math.toDegrees(headingRadians.toDouble()).toFloat()
        
        // Calculate offset to center the world position on screen
        val (offsetX, offsetY) = calculateCenterOffset(
            worldX = worldPosition.x,
            worldY = worldPosition.y,
            scale = scale,
            rotationDegrees = rotationDegrees,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        
        return CanvasState(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotationDegrees
        )
    }
    
    /**
     * Calculates the canvas state to center on a world position without changing rotation.
     * Used when aim button is pressed but rotation following is not enabled.
     *
     * @param worldPosition The position to center on (in world/PDR coordinates)
     * @param currentRotation The current canvas rotation to maintain
     * @param scale The target zoom scale
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return CanvasState that centers the position
     */
    fun calculateCenterState(
        worldPosition: Offset,
        currentRotation: Float,
        scale: Float,
        screenWidth: Float,
        screenHeight: Float
    ): CanvasState {
        val (offsetX, offsetY) = calculateCenterOffset(
            worldX = worldPosition.x,
            worldY = worldPosition.y,
            scale = scale,
            rotationDegrees = currentRotation,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        
        return CanvasState(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = currentRotation
        )
    }
    
    /**
     * Calculates the offset needed to center a world coordinate on screen.
     *
     * Transform order in graphicsLayer (TransformOrigin(0,0)):
     * 1. Scale around (0,0)
     * 2. Rotate around (0,0)  
     * 3. Translate by (offsetX, offsetY)
     *
     * Plus the inner translate(centerX, centerY) before drawing.
     *
     * @param worldX World X coordinate (relative to canvas center)
     * @param worldY World Y coordinate (relative to canvas center)
     * @param scale Canvas zoom scale
     * @param rotationDegrees Canvas rotation in degrees
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return Pair of (offsetX, offsetY)
     */
    private fun calculateCenterOffset(
        worldX: Float,
        worldY: Float,
        scale: Float,
        rotationDegrees: Float,
        screenWidth: Float,
        screenHeight: Float
    ): Pair<Float, Float> {
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        
        // Step 1: Apply inner translate (drawing at world coords + center)
        val localX = worldX + centerX
        val localY = worldY + centerY
        
        // Step 2: Apply scale (around origin 0,0)
        val scaledX = localX * scale
        val scaledY = localY * scale
        
        // Step 3: Apply rotation (around origin 0,0)
        val rotationRad = Math.toRadians(rotationDegrees.toDouble())
        val cosR = cos(rotationRad).toFloat()
        val sinR = sin(rotationRad).toFloat()
        
        val rotatedX = scaledX * cosR - scaledY * sinR
        val rotatedY = scaledX * sinR + scaledY * cosR
        
        // Step 4: Calculate offset so point ends up at screen center
        // Final position = rotated + offset = center
        // offset = center - rotated
        val offsetX = centerX - rotatedX
        val offsetY = centerY - rotatedY
        
        return Pair(offsetX, offsetY)
    }
    
    /**
     * Smoothly animates from current state to a target state.
     * Uses ease-out interpolation for natural deceleration.
     *
     * @param currentState Starting canvas state
     * @param targetState Target canvas state
     * @param config Following configuration
     * @param onStateUpdate Callback for each animation frame
     */
    suspend fun animateToState(
        currentState: CanvasState,
        targetState: CanvasState,
        config: FollowingConfig = FollowingConfig(),
        onStateUpdate: (CanvasState) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        var elapsed = 0L
        
        while (elapsed < config.animationDurationMs) {
            val progress = (elapsed.toFloat() / config.animationDurationMs).coerceIn(0f, 1f)
            val easedProgress = easeInOutCubic(progress)
            
            val interpolatedState = interpolate(currentState, targetState, easedProgress)
            onStateUpdate(interpolatedState)
            
            delay(config.frameDelayMs)
            elapsed = System.currentTimeMillis() - startTime
        }
        
        // Ensure we end exactly at target
        onStateUpdate(targetState)
    }
    
    /**
     * Interpolates between two canvas states.
     */
    private fun interpolate(
        start: CanvasState,
        end: CanvasState,
        progress: Float
    ): CanvasState {
        return CanvasState(
            scale = lerp(start.scale, end.scale, progress),
            offsetX = lerp(start.offsetX, end.offsetX, progress),
            offsetY = lerp(start.offsetY, end.offsetY, progress),
            rotation = lerpAngle(start.rotation, end.rotation, progress)
        )
    }
    
    /**
     * Linear interpolation.
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
    
    /**
     * Interpolates angles, taking the shortest path around the circle.
     */
    private fun lerpAngle(start: Float, end: Float, fraction: Float): Float {
        // Normalize difference to [-180, 180]
        var diff = end - start
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return start + diff * fraction
    }

    /**
     * Ease-in-out cubic easing function.
     * Smooth acceleration and deceleration.
     */
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            val t1 = -2f * t + 2f
            1f - (t1 * t1 * t1) / 2f
        }
    }
}
