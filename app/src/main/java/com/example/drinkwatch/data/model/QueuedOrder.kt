package com.example.drinkwatch.data.model

data class QueuedOrder(
    val playerId: Long,
    val drinkId: Long,
    val glassGroup: Char?,
    val glassNumber: Int?,
)
