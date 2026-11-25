package com.lowspin.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(
            "android.requestPromotedOngoing",
            HighSpinLiveNotificationProvider.EXTRA_REQUEST_PROMOTED_ONGOING
        )
        assertEquals(
            "android.shortCriticalText",
            HighSpinLiveNotificationProvider.EXTRA_SHORT_CRITICAL_TEXT
        )
        assertEquals(
            "android.substName",
            HighSpinLiveNotificationProvider.EXTRA_SUBST_NAME
        )
        assertEquals(
            "android.ongoing",
            HighSpinLiveNotificationProvider.EXTRA_ONGOING
        )
        assertEquals(
            "android.showChronometer",
            HighSpinLiveNotificationProvider.EXTRA_SHOW_CHRONOMETER
        )
    }

    @Test
    fun liveNotification_tickerFormattingValid() {
        assertEquals(
            "Song Title • Artist Name",
            HighSpinLiveNotificationProvider.formatTicker("Song Title", "Artist Name")
        )
        assertEquals(
            "Song Title • Local Audio",
            HighSpinLiveNotificationProvider.formatTicker("Song Title", "")
        )
        assertEquals(
            "HighSpin • Artist Name",
            HighSpinLiveNotificationProvider.formatTicker("", "Artist Name")
        )
        assertEquals(
            "HighSpin • Local Audio",
            HighSpinLiveNotificationProvider.formatTicker(null, null)
        )
    }

    @Test
    fun liveNotification_shortCriticalFormattingValid() {
        assertEquals(
            "Bohemian Rhapsody",
            HighSpinLiveNotificationProvider.formatShortCritical("Bohemian Rhapsody")
        )
        assertEquals(
            "HighSpin",
            HighSpinLiveNotificationProvider.formatShortCritical(null)
        )
        assertEquals(
            "HighSpin",
            HighSpinLiveNotificationProvider.formatShortCritical("   ")
        )
        val veryLongTitle = "This is an extremely long song title that exceeds twenty-four characters"
        val formatted = HighSpinLiveNotificationProvider.formatShortCritical(veryLongTitle)
        assertTrue(formatted.length <= 24)
        assertEquals(veryLongTitle.take(24), formatted)
    }

    @Test
    fun liveNotification_progressCalculationValid() {
        val result = HighSpinLiveNotificationProvider.computeProgress(30_000L, 180_000L)
        assertTrue(result != null)
        assertEquals(180_000, result!!.first)
        assertEquals(30_000, result.second)

        // Invalid duration gives null (no indeterminate progress)
        assertNull(HighSpinLiveNotificationProvider.computeProgress(0L, 0L))
        assertNull(HighSpinLiveNotificationProvider.computeProgress(100L, -1L))
        assertNull(HighSpinLiveNotificationProvider.computeProgress(-10L, 100L))

        // Clamped position if position exceeds duration
        val clamped = HighSpinLiveNotificationProvider.computeProgress(200_000L, 180_000L)
        assertTrue(clamped != null)
        assertEquals(180_000, clamped!!.first)
        assertEquals(180_000, clamped.second)
    }
}
