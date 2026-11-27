package com.lowspin.app.data.lyrics

import com.lowspin.app.data.db.SongEntity

data class LyricsLine(val timeMs: Long, val text: String)

data class Lyrics(
    val lines: List<LyricsLine>,
    val synced: Boolean,
    val sourceLabel: String = ""
)

/**
 * Pluggable lyrics provider. The current implementation reads embedded ID3
 * tags; a later SAF .lrc source can implement this without touching the UI.
 */
interface LyricsSource {
    suspend fun load(song: SongEntity): Lyrics?
}
