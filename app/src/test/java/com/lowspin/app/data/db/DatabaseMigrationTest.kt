package com.lowspin.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun migration6to7_versionNumbersCorrect() {
        assertEquals(6, LowspinDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, LowspinDatabase.MIGRATION_6_7.endVersion)
    }

    @Test
    fun migration7to8_versionNumbersCorrect_andPlayEventEntity() {
        assertEquals(7, LowspinDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, LowspinDatabase.MIGRATION_7_8.endVersion)

        val event = PlayEventEntity(
            id = 1L,
            songId = 42L,
            playedAt = 1700000000L,
            durationListenedMs = 180_000L
        )
        assertEquals(42L, event.songId)
        assertEquals(180_000L, event.durationListenedMs)
    }

    @Test
    fun songEqualizerEntity_instantiation() {
        val entity = SongEqualizerEntity(
            songId = 42L,
            presetIndex = 3,
            bassBoost = 600,
            virtualizer = 400,
            loudnessGain = 250,
            customBands = "60,120,-50,0,100",
            updatedAt = 1234567890L
        )

        assertEquals(42L, entity.songId)
        assertEquals(3, entity.presetIndex)
        assertEquals(600, entity.bassBoost)
        assertEquals(400, entity.virtualizer)
        assertEquals(250, entity.loudnessGain)
        assertEquals("60,120,-50,0,100", entity.customBands)
        assertEquals(1234567890L, entity.updatedAt)
    }

    @Test
    fun playlistEntity_customCoverFields() {
        val playlist = PlaylistEntity(
            id = 10L,
            name = "Workout Vibes",
            createdAt = 1000L,
            updatedAt = 2000L,
            customCoverUri = "content://media/external/images/123",
            gradientColorIndex = 4
        )

        assertEquals("content://media/external/images/123", playlist.customCoverUri)
        assertEquals(4, playlist.gradientColorIndex)

        val defaultPlaylist = PlaylistEntity(
            id = 11L,
            name = "Favorites",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        assertNull(defaultPlaylist.customCoverUri)
        assertNull(defaultPlaylist.gradientColorIndex)
    }

    @Test
    fun playlistSummary_customCoverFields() {
        val summary = PlaylistSummary(
            id = 1L,
            name = "Chill",
            createdAt = 1000L,
            updatedAt = 2000L,
            songCount = 12,
            artSongId = null,
            customCoverUri = "content://custom/art.png",
            gradientColorIndex = 2
        )

        assertEquals("content://custom/art.png", summary.customCoverUri)
        assertEquals(2, summary.gradientColorIndex)
    }
}
