package com.lowspin.app.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_emptyString_returnsNull() {
        assertNull(LrcParser.parse(""))
        assertNull(LrcParser.parse("   \n\t  "))
    }

    @Test
    fun parse_standardLrc_correctTimestampsAndSorting() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [00:12.50]Line one
            [00:05.00]Intro line
            [01:02.123]Line three
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertEquals(3, lyrics.lines.size)

        // Verifying sorting by timestamp
        assertEquals(5000L, lyrics.lines[0].timeMs)
        assertEquals("Intro line", lyrics.lines[0].text)

        assertEquals(12500L, lyrics.lines[1].timeMs)
        assertEquals("Line one", lyrics.lines[1].text)

        assertEquals(62123L, lyrics.lines[2].timeMs)
        assertEquals("Line three", lyrics.lines[2].text)
    }

    @Test
    fun parse_multipleTimestampsPerLine_replicatesLines() {
        val lrc = """
            [00:10.00][00:20.00]Chorus line repeated
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertEquals(2, lyrics.lines.size)

        assertEquals(10000L, lyrics.lines[0].timeMs)
        assertEquals("Chorus line repeated", lyrics.lines[0].text)

        assertEquals(20000L, lyrics.lines[1].timeMs)
        assertEquals("Chorus line repeated", lyrics.lines[1].text)
    }

    @Test
    fun parse_offsetHeader_shiftsTimestampsCorrectly() {
        val lrcWithPositiveOffset = """
            [offset: 500]
            [00:10.00]Shifted forward
        """.trimIndent()

        val lyricsPos = LrcParser.parse(lrcWithPositiveOffset)
        assertNotNull(lyricsPos)
        assertEquals(10500L, lyricsPos!!.lines[0].timeMs)

        val lrcWithNegativeOffset = """
            [offset: -2000]
            [00:05.00]Shifted backward
            [00:01.00]Clamped to zero
        """.trimIndent()

        val lyricsNeg = LrcParser.parse(lrcWithNegativeOffset)
        assertNotNull(lyricsNeg)
        assertEquals(0L, lyricsNeg!!.lines[0].timeMs) // clamped coerceAtLeast(0L)
        assertEquals(3000L, lyricsNeg.lines[1].timeMs)
    }

    @Test
    fun parse_unslrc_plainTextLyrics() {
        val plainText = """
            First verse without any timestamps
            Second verse without timestamps
            Final outro line
        """.trimIndent()

        val lyrics = LrcParser.parse(plainText)
        assertNotNull(lyrics)
        assertFalse(lyrics!!.synced)
        assertEquals(3, lyrics.lines.size)
        assertEquals("First verse without any timestamps", lyrics.lines[0].text)
        assertEquals(-1L, lyrics.lines[0].timeMs)
    }

    @Test
    fun parse_twoDigitAndThreeDigitMilliseconds() {
        val lrc = """
            [01:05.8]Single digit fraction
            [01:05.85]Two digit fraction
            [01:05.854]Three digit fraction
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertEquals(65800L, lyrics!!.lines[0].timeMs)
        assertEquals(65850L, lyrics.lines[1].timeMs)
        assertEquals(65854L, lyrics.lines[2].timeMs)
    }

    @Test
    fun parse_metadataHeaders_ignoredAndNotTreatedAsLyrics() {
        val lrc = """
            [ti:Song Title]
            [ar:Artist Name]
            [al:Album Name]
            [by:LRC Maker]
            [length: 03:45]
            [tool: foobar2000]
            [re: player]
            [ve: 2.1]
            [00:05.00]First real lyric line
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertEquals(1, lyrics.lines.size)
        assertEquals("First real lyric line", lyrics.lines[0].text)
        assertEquals(5000L, lyrics.lines[0].timeMs)
    }

    @Test
    fun parse_onlyMetadataHeaders_returnsNull() {
        val lrc = """
            [ti:Song Title]
            [ar:Artist Name]
            [length: 03:45]
            [tool: foobar2000]
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNull(lyrics)
    }

    @Test
    fun parse_unicodeAndMultilingualLyrics() {
        val lrc = """
            [00:05.00]¡Hola Mundo! Canción en español
            [00:10.50]君の名は希望 🎵
            [00:15.00]Café, naïve, façade
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertEquals(3, lyrics.lines.size)
        assertEquals("¡Hola Mundo! Canción en español", lyrics.lines[0].text)
        assertEquals("君の名は希望 🎵", lyrics.lines[1].text)
        assertEquals("Café, naïve, façade", lyrics.lines[2].text)
    }

    @Test
    fun parse_extremeNegativeOffset_clampsTimestampsToZero() {
        val lrc = """
            [offset: -999999]
            [00:10.00]Will be clamped to zero
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertNotNull(lyrics)
        assertEquals(0L, lyrics!!.lines[0].timeMs)
    }
}
