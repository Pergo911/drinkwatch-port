package com.example.drinkwatch.viewmodel

data class OrderHistoryItem(
    val timestampMs: Long,
    /** Resolved drink name; "(deleted drink)" if the drink was removed after the order. */
    val drinkName: String,
    val glassGroup: Char?,
    val glassNumber: Int?,
    /**
     * Whether the assigned glass has been returned.
     * null  = no glass was assigned to this order.
     * true  = glass was returned.
     * false = glass is still unreturned.
     */
    val glassReturned: Boolean?,
)
