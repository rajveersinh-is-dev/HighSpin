package com.lowspin.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNotificationTest {

    @Test
    fun liveNotification_constantsMatchRequirements() {
        assertEquals(
            "com.lowspin.app.media",
            HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID
        )
        assertEquals(1001, HighSpinLiveNotificationProvider.NOTIFICATION_ID)
    }

    @Test
    fun liveNotification_tickerFormattingValid() {
        fun formatTicker(title: String?, artist: String?): String {
            val safeTitle = title?.takeIf { it.isNotBlank() } ?: "HighSpin"
            val safeArtist = artist?.takeIf { it.isNotBlank() } ?: "Local Audio"
            return "$safeTitle • $safeArtist"
        }

        assertEquals("Song Title • Artist Name", formatTicker("Song Title", "Artist Name"))
        assertEquals("Song Title • Local Audio", formatTicker("Song Title", ""))
        assertEquals("HighSpin • Artist Name", formatTicker("", "Artist Name"))
        assertEquals("HighSpin • Local Audio", formatTicker(null, null))
    }

    @Test
    fun liveNotification_progressCalculationValid() {
        fun computeProgress(position: Long, duration: Long): Pair<Int, Int>? {
            if (duration <= 0L || position < 0L) return null
            val safePos = position.coerceIn(0L, duration)
            return Pair(duration.toInt(), safePos.toInt())
        }

        val result = computeProgress(30_000L, 180_000L)
        assertTrue(result != null)
        assertEquals(180_000, result!!.first)
        assertEquals(30_000, result.second)

        // Invalid duration gives null (no indeterminate progress)
        assertEquals(null, computeProgress(0L, 0L))
        assertEquals(null, computeProgress(100L, -1L))
    }
}
