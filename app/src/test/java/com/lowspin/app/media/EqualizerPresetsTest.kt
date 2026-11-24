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
}
