package com.lowspin.app.data.lyrics

import android.content.Context
import android.net.Uri
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

private data class Cached(val result: Lyrics)

/** Memoises parsed lyrics per song/version and persists user-imported and online .lrc files. */
class LyricsRepository(
    private val source: LyricsSource,
    private val appContext: Context? = null
) {
    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<Long, Cached>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Cached>?): Boolean {
                return size > 64
            }
        }
    )

    suspend fun load(song: SongEntity): Lyrics? {
        cache[song.id]?.let { return it.result }
        val result = source.load(song)
        if (result != null && result.lines.isNotEmpty()) {
            cache[song.id] = Cached(result)
        }
        return result
    }

    fun invalidate(songId: Long) {
        cache.remove(songId)
    }

    suspend fun saveLyricsText(song: SongEntity, text: String, sourceLabel: String): Lyrics? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext null
        val ctx = appContext

        val parsed = LrcParser.parse(trimmed)?.copy(sourceLabel = sourceLabel)
            ?: run {
                val plainLines = trimmed.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { LyricsLine(timeMs = -1L, text = it) }
                if (plainLines.isNotEmpty()) {
                    Lyrics(lines = plainLines, synced = false, sourceLabel = sourceLabel)
                } else null
            } ?: return@withContext null

        if (ctx != null) {
            runCatching {
                val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
                File(dir, "${song.id}.lrc").writeText(trimmed, Charsets.UTF_8)
            }
        }
        cache[song.id] = Cached(parsed)
        parsed
    }

    suspend fun fetchOnline(song: SongEntity): Lyrics? = withContext(Dispatchers.IO) {
        val content = OnlineLyricsFetcher.fetchLrc(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs
        ) ?: return@withContext null
        saveLyricsText(song, content, "Web (LRCLIB)")
    }

    suspend fun importFromUrl(song: SongEntity, url: String): Lyrics? = withContext(Dispatchers.IO) {
        val content = OnlineLyricsFetcher.fetchFromUrl(url) ?: return@withContext null
        saveLyricsText(song, content, "Web (URL)")
    }

    suspend fun importLrcFromUri(song: SongEntity, uri: Uri): Lyrics? = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext null
        val content = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@withContext null

        saveLyricsText(song, content, "Imported .lrc")
    }
}
