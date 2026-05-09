package com.example.drinkwatch.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

/**
 * Formats a total-seconds value using the provided locale-aware format string.
 * The format must have two integer arguments: %1$d = hours, %2$d = minutes.
 * Obtain the format string via stringResource(R.string.duration_format) in a composable.
 */
fun formatDuration(totalSeconds: Int, format: String): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return String.format(format, hours, minutes)
}

/** Formats an epoch-millisecond timestamp as "HH:mm:ss" in the device locale. */
fun formatTimestamp(timestampMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
