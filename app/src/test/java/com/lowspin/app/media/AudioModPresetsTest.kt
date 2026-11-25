package com.lowspin.app.media

import com.lowspin.app.ui.components.AUDIO_MOD_PRESETS
import com.lowspin.app.ui.components.AVAILABLE_SPEEDS
import com.lowspin.app.ui.settings.HighSpinThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioModPresetsTest {

    @Test
    fun audioModPresets_allSpeedsAndPitchesWithinSafeBounds() {
        assertTrue(AUDIO_MOD_PRESETS.isNotEmpty())
        for (preset in AUDIO_MOD_PRESETS) {
            assertTrue(
                "Speed ${preset.speed} for ${preset.name} out of bounds",
                preset.speed in 0.5f..2.5f
            )
            assertTrue(
                "Pitch ${preset.pitch} for ${preset.name} out of bounds",
                preset.pitch in 0.5f..2.0f
            )
        }
    }

    @Test
    fun availableSpeeds_sortedAndDistinct() {
        assertTrue(AVAILABLE_SPEEDS.contains(1.0f))
        val sorted = AVAILABLE_SPEEDS.sorted()
        assertEquals(sorted, AVAILABLE_SPEEDS)
        assertEquals(AVAILABLE_SPEEDS.toSet().size, AVAILABLE_SPEEDS.size)
    }

    @Test
    fun highSpinThemes_validAndUnique() {
        assertTrue(HighSpinThemes.size >= 5)
        val ids = HighSpinThemes.map { it.id }
        assertEquals("Theme IDs must be unique", ids.toSet().size, ids.size)

        for (theme in HighSpinThemes) {
            assertTrue("Theme name must not be blank", theme.name.isNotBlank())
            assertTrue("Theme primary alpha must be 1.0", theme.primary.alpha > 0.99f)
            assertTrue("Theme background alpha must be 1.0", theme.background.alpha > 0.99f)
        }
    }
}
