package com.lowspin.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ReplayGainTest {

    @Test
    fun dbToMillibels_conversions() {
        assertEquals(0, ReplayGainCalculator.dbToMillibels(0.0f))
        assertEquals(300, ReplayGainCalculator.dbToMillibels(3.0f))
        assertEquals(-600, ReplayGainCalculator.dbToMillibels(-6.0f))
        assertEquals(350, ReplayGainCalculator.dbToMillibels(3.5f))
    }

    @Test
    fun millibelsToDb_conversions() {
        assertEquals(0.0f, ReplayGainCalculator.millibelsToDb(0), 0.001f)
        assertEquals(3.0f, ReplayGainCalculator.millibelsToDb(300), 0.001f)
        assertEquals(-6.0f, ReplayGainCalculator.millibelsToDb(-600), 0.001f)
        assertEquals(3.5f, ReplayGainCalculator.millibelsToDb(350), 0.001f)
    }

    @Test
    fun dbToLinearScale_unityGainAtZeroDb() {
        val unity = ReplayGainCalculator.dbToLinearScale(0.0f)
        assertEquals(1.0f, unity, 0.001f)

        val plus6Db = ReplayGainCalculator.dbToLinearScale(6.0206f)
        assertEquals(2.0f, plus6Db, 0.01f)

        val minus6Db = ReplayGainCalculator.dbToLinearScale(-6.0206f)
        assertEquals(0.5f, minus6Db, 0.01f)
    }

    @Test
    fun calculateTrackGain_atTargetLufsReturnsNearZero() {
        // -14 LUFS is approx 10^(-14/20) = 0.1995 RMS energy
        val rmsAtTarget = 0.199526f
        val gain = ReplayGainCalculator.calculateTrackGain(
            peakAmplitude = 0.8f,
            rmsEnergy = rmsAtTarget,
            targetLufs = -14.0f
        )
        assertTrue("Gain should be near 0 dB, was $gain", abs(gain) < 0.1f)
    }

    @Test
    fun calculateTrackGain_quietTrackBoostsGain() {
        // Quiet track: RMS = 0.05 (~ -26 dBFS)
        val gain = ReplayGainCalculator.calculateTrackGain(
            peakAmplitude = 0.2f,
            rmsEnergy = 0.05f,
            targetLufs = -14.0f
        )
        assertTrue("Gain should be positive for quiet track, was $gain", gain > 0f)
    }

    @Test
    fun calculateTrackGain_preventClippingClampsHeadroom() {
        // Peak is already 1.0 (0 dBFS headroom). Positive boost should be prevented.
        val gain = ReplayGainCalculator.calculateTrackGain(
            peakAmplitude = 1.0f,
            rmsEnergy = 0.05f,
            targetLufs = -14.0f,
            preventClipping = true
        )
        assertTrue("Gain must not exceed 0 dB when peak is 1.0, was $gain", gain <= 0.001f)
    }

    @Test
    fun calculateTrackGain_clampsWithinSafeRange() {
        // Extremely quiet track: should not exceed MAX_SAFE_GAIN_DB (+12 dB)
        val maxGain = ReplayGainCalculator.calculateTrackGain(
            peakAmplitude = 0.01f,
            rmsEnergy = 0.0001f,
            targetLufs = -14.0f,
            preventClipping = false
        )
        assertEquals(ReplayGainCalculator.MAX_SAFE_GAIN_DB, maxGain, 0.001f)

        // Extremely loud track: should not go below MIN_SAFE_GAIN_DB (-12 dB)
        val minGain = ReplayGainCalculator.calculateTrackGain(
            peakAmplitude = 1.0f,
            rmsEnergy = 1.0f, // 0 dBFS
            targetLufs = -14.0f
        )
        assertEquals(-12.0f, minGain, 0.001f)
    }

    @Test
    fun computeNormalizationGainMb_clampsForLoudnessEnhancer() {
        val gainMb = ReplayGainCalculator.computeNormalizationGainMb(
            peakAmplitude = 0.5f,
            rmsEnergy = 0.1f
        )
        assertTrue("LoudnessEnhancer gain must be between 0 and 1000 mB, was $gainMb", gainMb in 0..1000)
    }

    @Test
    fun computeTrackNormalization_producesValidLoudnessGains() {
        val gain1 = ReplayGainCalculator.computeTrackNormalization(songId = 1L, durationMs = 210_000L)
        val gain2 = ReplayGainCalculator.computeTrackNormalization(songId = 42L, durationMs = 185_000L)
        val gain3 = ReplayGainCalculator.computeTrackNormalization(songId = 9999L, durationMs = 340_000L)

        assertTrue("Track 1 gain in range 0..1000, was $gain1", gain1 in 0..1000)
        assertTrue("Track 2 gain in range 0..1000, was $gain2", gain2 in 0..1000)
        assertTrue("Track 3 gain in range 0..1000, was $gain3", gain3 in 0..1000)
    }
}
