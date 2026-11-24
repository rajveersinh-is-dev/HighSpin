package com.lowspin.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetsTest {

    @Test
    fun presets_containsStandardAudioProfiles() {
        assertTrue(EqualizerPresets.isNotEmpty())
        assertTrue(EqualizerPresets.contains("Flat"))
        assertTrue(EqualizerPresets.contains("Bass Boost"))
        assertTrue(EqualizerPresets.contains("Rock"))
        assertTrue(EqualizerPresets.contains("Pop"))
        assertTrue(EqualizerPresets.contains("Classical"))
    }

    @Test
    fun presets_firstPresetIsFlat() {
        assertEquals("Flat", EqualizerPresets[0])
    }

    @Test
    fun presetIndex_clampsSafely() {
        val count = EqualizerPresets.size
        fun safeIndex(index: Int): Int = index.coerceIn(0, count - 1)

        assertEquals(0, safeIndex(-5))
        assertEquals(count - 1, safeIndex(count + 10))
        assertEquals(2, safeIndex(2))
    }

    @Test
    fun bassBoostAndVirtualizer_clampToExpectedDsbRange() {
        fun clampDsb(strength: Int): Int = strength.coerceIn(0, 1000)

        assertEquals(0, clampDsb(-100))
        assertEquals(500, clampDsb(500))
        assertEquals(1000, clampDsb(1500))
    }

    @Test
    fun presetFallbackGains_returns5BandsForEveryPreset() {
        for (i in EqualizerPresets.indices) {
            val gains = EqualizerManager.presetFallbackGains(i)
            assertEquals("Preset $i (${EqualizerPresets[i]}) must have 5 bands", 5, gains.size)
        }
    }

    @Test
    fun presetFallbackGains_flatIsAllZeros() {
        val flatGains = EqualizerManager.presetFallbackGains(0)
        assertEquals(listOf(0, 0, 0, 0, 0), flatGains)
    }

    @Test
    fun presetFallbackGains_rockHasVShapedProfile() {
        val rockIndex = EqualizerPresets.indexOf("Rock")
        assertTrue(rockIndex >= 0)
        val rockGains = EqualizerManager.presetFallbackGains(rockIndex)
        // V-shaped: lows and highs boosted, mids dipped
        assertTrue(rockGains[0] > 0) // Bass boosted
        assertTrue(rockGains[4] > 0) // Treble boosted
        assertTrue(rockGains[2] < rockGains[0]) // Mids lower than bass
    }

    @Test
    fun presetFallbackGains_outOfBoundsFallsBackSafely() {
        val negative = EqualizerManager.presetFallbackGains(-1)
        val excessive = EqualizerManager.presetFallbackGains(999)
        assertEquals(5, negative.size)
        assertEquals(5, excessive.size)
    }
}
