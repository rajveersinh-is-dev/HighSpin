package com.lowspin.app.data.lyrics

import com.lowspin.app.data.db.SongEntity

class CompositeLyricsSource(
    private val sources: List<LyricsSource>
) : LyricsSource {

    override suspend fun load(song: SongEntity): Lyrics? {
        for (source in sources) {
            val lyrics = source.load(song)
            if (lyrics != null && lyrics.lines.isNotEmpty()) {
                return lyrics
            }
        }
        return null
    }
}
