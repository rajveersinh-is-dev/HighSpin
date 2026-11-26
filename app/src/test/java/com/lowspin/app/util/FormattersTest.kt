package com.lowspin.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatDuration_handlesZeroAndNegative() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:00", formatDuration(-1000L))
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("0:05", formatDuration(5_000L))
        assertEquals("0:45", formatDuration(45_000L))
        assertEquals("1:00", formatDuration(60_000L))
        assertEquals("3:42", formatDuration(222_000L))
        assertEquals("59:59", formatDuration(3599_000L))
    }

    @Test
    fun formatDuration_formatsHoursWhenExceeded() {
        assertEquals("1:00:00", formatDuration(3600_000L))
        assertEquals("1:05:23", formatDuration(3923_000L))
        assertEquals("10:15:30", formatDuration(36930_000L))
    }

    @Test
    fun formatDurationWithMillis_formatsMilliseconds() {
        assertEquals("0:00.0", formatDurationWithMillis(0L))
        assertEquals("0:00.0", formatDurationWithMillis(-500L))
        assertEquals("0:05.3", formatDurationWithMillis(5_340L))
        assertEquals("1:23.7", formatDurationWithMillis(83_780L))
        assertEquals("1:02:03.4", formatDurationWithMillis(3723_400L))
    }

    @Test
    fun formatTotalDuration_handlesSubMinute() {
        assertEquals("Less than a minute", formatTotalDuration(0L))
        assertEquals("Less than a minute", formatTotalDuration(30_000L))
        assertEquals("Less than a minute", formatTotalDuration(59_999L))
    }

    @Test
    fun formatTotalDuration_formatsMinutes() {
        assertEquals("1 min", formatTotalDuration(60_000L))
        assertEquals("45 min", formatTotalDuration(45 * 60_000L))
        assertEquals("59 min", formatTotalDuration(59 * 60_000L))
    }

    @Test
    fun formatTotalDuration_formatsHoursAndMinutes() {
        assertEquals("1 hr", formatTotalDuration(60 * 60_000L))
        assertEquals("1 hr 15 min", formatTotalDuration(75 * 60_000L))
        assertEquals("2 hr 30 min", formatTotalDuration(150 * 60_000L))
    }
}
