package com.example.drinkwatch.viewmodel

data class OrderHistoryItem(
    val timestampMs: Long,
    /** Resolved drink name; "(deleted drink)" if the drink was removed after the order. */
    val drinkName: String,
    val glassGroup: Char?,
    val glassNumber: Int?,
)
