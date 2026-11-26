package com.lowspin.app.util

import java.util.Locale

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatDurationWithMillis(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00.0"
    val totalSeconds = durationMs / 1000L
    val tenths = (durationMs % 1000L) / 100L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.US, "%d:%02d.%d", minutes, seconds, tenths)
    }
}

fun formatTotalDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000L
    return when {
        totalMinutes < 1L -> "Less than a minute"
        totalMinutes < 60L -> "$totalMinutes min"
        else -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes == 0L) "$hours hr" else "$hours hr $minutes min"
        }
    }
}
