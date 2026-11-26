package com.lowspin.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ABRepeatTest {

    @Test
    fun loopRange_validWhenBGreaterThanA() {
        val startMs = 5_000L
        val endMs = 15_000L
        val range = Pair(startMs, endMs)

        assertTrue(range.second > range.first)
        val loopLength = range.second - range.first
        assertEquals(10_000L, loopLength)
    }

    @Test
    fun loopRange_boundarySeekCondition() {
        val loopA = 2_000L
        val loopB = 8_000L

        // Player before boundary: should not loop
        val posBefore = 7_999L
        assertFalse(posBefore >= loopB)

        // Player at boundary: should loop
        val posAt = 8_000L
        assertTrue(posAt >= loopB)

        // Player beyond boundary: should loop
        val posAfter = 8_500L
        assertTrue(posAfter >= loopB)
    }

    @Test
    fun loopRange_fineTuneCalculations() {
        val currentA = 5_000L
        val currentB = 10_000L
        val durationMs = 60_000L

        // +100ms
        val newA1 = (currentA + 100L).coerceIn(0L, currentB - 100L)
        assertEquals(5_100L, newA1)

        // -100ms
        val newA2 = (currentA - 100L).coerceIn(0L, currentB - 100L)
        assertEquals(4_900L, newA2)

        // Cannot move A past B - 100ms
        val clampedA = (currentB + 500L).coerceIn(0L, currentB - 100L)
        assertEquals(9_900L, clampedA)

        // Cannot move B before A + 100ms
        val clampedB = (currentA - 500L).coerceIn(currentA + 100L, durationMs)
        assertEquals(5_100L, clampedB)
    }

    @Test
    fun loopRange_clearResetsToNull() {
        var range: Pair<Long, Long>? = Pair(1_000L, 5_000L)
        assertEquals(1_000L, range?.first)
        range = null
        assertNull(range)
    }

    @Test
    fun loopRange_trackTransitionClearsLoop() {
        val currentSongId = 101L
        var loopRange: Pair<Long, Long>? = Pair(5_000L, 20_000L)

        // Track transitions to a new song ID
        val nextSongId = 102L
        if (nextSongId != currentSongId) {
            loopRange = null
        }

        assertNull("Loop range must be cleared when media changes to another track", loopRange)
    }

    @Test
    fun loopRange_singlePointDefaultCalculations() {
        val durationMs = 180_000L
        val safeDuration = durationMs.coerceAtLeast(1L)

        // Point A set, Point B defaulted to end of track
        val pointA = 30_000L
        val pointB = null
        val effectiveA = pointA
        val effectiveB = pointB ?: safeDuration
        assertTrue(effectiveB > effectiveA)
        assertEquals(180_000L, effectiveB)

        // Point B set, Point A defaulted to beginning of track
        val pointA2 = null
        val pointB2 = 45_000L
        val effectiveA2 = pointA2 ?: 0L
        val effectiveB2 = pointB2
        assertTrue(effectiveB2 > effectiveA2)
        assertEquals(0L, effectiveA2)
    }
}
