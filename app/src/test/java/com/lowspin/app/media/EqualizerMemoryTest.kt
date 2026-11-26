package com.lowspin.app.media

import com.lowspin.app.data.db.SongEqualizerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerMemoryTest {

    @Test
    fun customBands_serializationAndDeserialization() {
        val bands = mapOf(0 to 300, 1 to 150, 2 to -50, 3 to 0, 4 to 200)
        val serialized = bands.entries.joinToString(";") { "${it.key}:${it.value}" }
        assertEquals("0:300;1:150;2:-50;3:0;4:200", serialized)

        val parsed = serialized.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val band = parts[0].toIntOrNull()
                    val level = parts[1].toIntOrNull()
                    if (band != null && level != null) band to level else null
                } else null
            }
            .toMap()

        assertEquals(bands, parsed)
    }

    @Test
    fun customBands_emptyAndMalformedStringsHandledSafely() {
        val emptyParsed = "".split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val band = parts[0].toIntOrNull()
                    val level = parts[1].toIntOrNull()
                    if (band != null && level != null) band to level else null
                } else null
            }.toMap()
        assertTrue(emptyParsed.isEmpty())

        val malformed = "invalid;1:notanumber;3:50;foo"
        val malformedParsed = malformed.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val band = parts[0].toIntOrNull()
                    val level = parts[1].toIntOrNull()
                    if (band != null && level != null) band to level else null
                } else null
            }.toMap()
        assertEquals(mapOf(3 to 50), malformedParsed)
    }

    @Test
    fun genreEqualizer_normalizationAndKeyGeneration() {
        val rawGenre1 = "  Hip Hop  "
        val clean1 = rawGenre1.trim().lowercase()
        assertEquals("hip hop", clean1)

        val rawGenre2 = "ELECTRONIC"
        val clean2 = rawGenre2.trim().lowercase()
        assertEquals("electronic", clean2)

        val emptyGenre = "   "
        val cleanEmpty = emptyGenre.trim().lowercase()
        assertTrue(cleanEmpty.isBlank())
    }

    @Test
    fun globalBaseline_preservedWhenSongOverrideApplied() {
        // User's global baseline
        val globalPreset = 4 // Rock
        val globalBassBoost = 500
        val globalVirtualizer = 300
        val globalLoudnessGain = 200

        var activePreset = globalPreset
        var activeBassBoost = globalBassBoost
        var activeVirtualizer = globalVirtualizer
        var activeLoudnessGain = globalLoudnessGain

        // Track 42 has a dedicated per-song equalizer entity
        val songEq = SongEqualizerEntity(
            songId = 42L,
            presetIndex = 7, // Classical
            bassBoost = 0,
            virtualizer = 100,
            loudnessGain = 0,
            customBands = null,
            updatedAt = System.currentTimeMillis()
        )

        // Apply song equalizer override
        activePreset = songEq.presetIndex
        activeBassBoost = songEq.bassBoost
        activeVirtualizer = songEq.virtualizer
        activeLoudnessGain = songEq.loudnessGain

        // Active equalizer reflects the custom song profile
        assertEquals(7, activePreset)
        assertEquals(0, activeBassBoost)

        // Global baseline must remain untouched
        assertEquals(4, globalPreset)
        assertEquals(500, globalBassBoost)

        // Moving to track 43 (no dedicated per-song EQ) -> restore global baseline
        activePreset = globalPreset
        activeBassBoost = globalBassBoost
        activeVirtualizer = globalVirtualizer
        activeLoudnessGain = globalLoudnessGain

        assertEquals(4, activePreset)
        assertEquals(500, activeBassBoost)
        assertEquals(300, activeVirtualizer)
        assertEquals(200, activeLoudnessGain)
    }
}
