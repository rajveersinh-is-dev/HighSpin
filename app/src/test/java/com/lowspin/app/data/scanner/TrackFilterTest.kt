package com.lowspin.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackFilterTest {

    @Test
    fun isRecording_detectsExcludedFolderSegments() {
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Recordings/Call", "song.mp3", "Call"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/WhatsApp/WhatsApp Audio", "audio.opus", "Audio"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Voice Recorder/", "memo.m4a", "Voice memo"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Telegram/Telegram Voice", "voice.ogg", "Voice"))
    }

    @Test
    fun isRecording_detectsExcludedFilePrefixes() {
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "call_20261124_120000.mp3", "Conversation"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Download", "rec_meeting.m4a", "Team sync"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "ptt-20261124-WA0001.opus", "Voice note"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "screenrec_test.mp4", "Screen Recording"))
    }

    @Test
    fun isRecording_detectsExcludedTitlePrefixes() {
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "track1.mp3", "Call recording from 555-0199"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "track2.mp3", "Voice recording #42"))
        assertTrue(TrackFilter.isRecording("/storage/emulated/0/Music", "track3.mp3", "screencast session 1"))
    }

    @Test
    fun isRecording_allowsLegitimateMusicTracks() {
        assertFalse(TrackFilter.isRecording("/storage/emulated/0/Music/Pink Floyd/The Wall", "01 In the Flesh.flac", "In the Flesh?"))
        assertFalse(TrackFilter.isRecording("/storage/emulated/0/Music/Daft Punk", "Get Lucky.mp3", "Get Lucky"))
        assertFalse(TrackFilter.isRecording("/storage/emulated/0/Download", "Song_Title_Calling_You.mp3", "Calling You"))
        assertFalse(TrackFilter.isRecording("/storage/emulated/0/Music", "Recorder_Concerto.flac", "Concerto for Recorder in C Major"))
    }

    @Test
    fun normalizePath_convertsBackslashesAndTrims() {
        assertEquals("storage/emulated/0/music", TrackFilter.normalizePath("\\storage\\emulated\\0\\music\\"))
        assertEquals("storage/emulated/0/music", TrackFilter.normalizePath("  /storage/emulated/0/music/  "))
    }

    @Test
    fun matchKey_combinesFolderAndFileNameNormalized() {
        val key = TrackFilter.matchKey("\\storage\\emulated\\0\\Music\\", "Song.mp3")
        assertEquals("storage/emulated/0/music/song.mp3", key)
    }
}
