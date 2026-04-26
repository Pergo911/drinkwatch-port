package com.example.drinkwatch.data.serialization

import kotlinx.serialization.Serializable

@Serializable
data class SessionSnapshot(
    val version: Int = 1,
    val name: String,
    val players: List<PlayerSnapshot>,
    val drinks: List<DrinkSnapshot>,
    /** Single-char strings representing each glass group letter (e.g. ["A","B"]). */
    val glassGroups: List<String>,
    val events: List<EventSnapshot>,
)

@Serializable
data class PlayerSnapshot(
    val id: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean,
)

@Serializable
data class DrinkSnapshot(
    val id: Long,
    val name: String,
    /** Stores [com.example.drinkwatch.data.model.DrinkType.name]. */
    val type: String,
    val isDisabled: Boolean,
)

/**
 * Flat snapshot of a single event row.
 *
 * [id] is included for human readability but is NOT used during import (IDs are remapped).
 * [type] uses the EventEntity.TYPE_* string constants (ORDER, RETURN, TIMEOUT, etc.).
 * [playerId] and [drinkId] reference [PlayerSnapshot.id] / [DrinkSnapshot.id] in the same snapshot.
 */
@Serializable
data class EventSnapshot(
    val id: Long,
    val timestampMs: Long,
    val type: String,
    val playerId: Long? = null,
    val drinkId: Long? = null,
    val glassGroup: String? = null,
    val glassNumber: Int? = null,
    val durationSeconds: Int? = null,
)
