package com.example.drinkwatch.data.model

data class PlayerDerivedState(
    val player: Player,
    val activeDrinkCount: Int,
    val isUnderTimeout: Boolean,
    val timeoutEndsAtMs: Long?,     // null when not under timeout
    val totalDrinks: Int,
    val totalTimeoutSeconds: Int,
    val unreturnedGlassCount: Int,
)

data class TakenGlass(
    val glassGroup: Char,
    val glassNumber: Int,
    val playerId: Long,
    val drinkId: Long,
    val takenAtMs: Long,
)
