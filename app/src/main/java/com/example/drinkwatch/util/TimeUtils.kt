package com.example.drinkwatch.util

import kotlin.math.max

/** Formats a millisecond countdown as "MM:SS", clamped to "00:00". */
fun formatCountdown(millisRemaining: Long): String {
    val totalSeconds = max(0L, millisRemaining / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/** Formats a total-seconds value as "Xh Ym" (e.g., "1h 23m" or "0h 5m"). */
fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}
