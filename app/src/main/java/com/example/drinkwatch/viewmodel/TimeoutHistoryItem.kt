package com.example.drinkwatch.viewmodel

data class TimeoutHistoryItem(
    val timestampMs: Long,
    /** Duration of the timeout in seconds; 0 means the timeout was cancelled. */
    val durationSeconds: Int,
)
