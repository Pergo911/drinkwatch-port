package com.example.drinkwatch.viewmodel

data class TakenGlassUiState(
    val glassGroup: Char,
    val glassNumber: Int,
    val playerName: String,
    val drinkName: String,
    val takenAtMs: Long,
    /** Retained for the return-glass action. */
    val playerId: Long,
)
