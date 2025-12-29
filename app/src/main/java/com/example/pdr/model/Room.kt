package com.example.pdr.model

/**
 * Represents a room on the floor plan.
 * @param id Unique identifier for the room
 * @param x X-coordinate of the room's center
 * @param y Y-coordinate of the room's center
 * @param name Display name of the room (may be null for unnamed rooms)
 * @param pointIds List of wall endpoint IDs that form the room boundary
 */
data class Room(
    val id: Int,
    val x: Float,
    val y: Float,
    val name: String?,
    val pointIds: List<Int>
)
