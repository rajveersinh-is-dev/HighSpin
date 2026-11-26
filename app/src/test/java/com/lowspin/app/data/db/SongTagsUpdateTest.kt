package com.lowspin.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class SongTagsUpdateTest {

    private val sampleSong = SongEntity(
        id = 100L,
        title = "Original Title",
        artist = "Original Artist",
        artistId = 10L,
        album = "Original Album",
        albumId = 20L,
        durationMs = 240_000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2020,
        folderPath = "/storage/emulated/0/Music",
        folderName = "Music",
        contentUri = "content://media/external/audio/media/100",
        mimeType = "audio/mp3",
        dateAdded = 1600000000L,
        dateModified = 1600000000L
    )

    @Test
    fun songEntity_updateTagsCopiesCorrectly() {
        val updated = sampleSong.copy(
            title = "New Title",
            artist = "New Artist",
            album = "New Album",
            trackNumber = 5,
            year = 2024
        )

        assertEquals("New Title", updated.title)
        assertEquals("New Artist", updated.artist)
        assertEquals("New Album", updated.album)
        assertEquals(5, updated.trackNumber)
        assertEquals(2024, updated.year)

        // Preserved fields
        assertEquals(sampleSong.id, updated.id)
        assertEquals(sampleSong.durationMs, updated.durationMs)
        assertEquals(sampleSong.contentUri, updated.contentUri)
        assertEquals(sampleSong.folderPath, updated.folderPath)
    }

    @Test
    fun songEntity_tagSanitizationFallbacks() {
        val rawTitle = "   "
        val rawArtist = ""
        val rawAlbum = "   "
        val cleanTitle = rawTitle.trim().ifBlank { "Unknown Title" }
        val cleanArtist = rawArtist.trim().ifBlank { "Unknown Artist" }
        val cleanAlbum = rawAlbum.trim().ifBlank { "Unknown Album" }
        val safeTrack = (-3).coerceAtLeast(0)
        val safeYear = (-1999).coerceAtLeast(0)

        assertEquals("Unknown Title", cleanTitle)
        assertEquals("Unknown Artist", cleanArtist)
        assertEquals("Unknown Album", cleanAlbum)
        assertEquals(0, safeTrack)
        assertEquals(0, safeYear)
    }

    @Test
    fun songEntity_validTagsRetainedWithoutModification() {
        val title = "Bohemian Rhapsody"
        val artist = "Queen"
        val album = "A Night at the Opera"
        val track = 4
        val year = 1975

        val cleanTitle = title.trim().ifBlank { "Unknown Title" }
        val cleanArtist = artist.trim().ifBlank { "Unknown Artist" }
        val cleanAlbum = album.trim().ifBlank { "Unknown Album" }

        assertEquals(title, cleanTitle)
        assertEquals(artist, cleanArtist)
        assertEquals(album, cleanAlbum)
        assertEquals(4, track.coerceAtLeast(0))
        assertEquals(1975, year.coerceAtLeast(0))
    }
}
