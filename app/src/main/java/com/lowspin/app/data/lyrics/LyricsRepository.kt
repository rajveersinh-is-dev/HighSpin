package com.lowspin.app.data.lyrics

import android.content.Context
import android.net.Uri
import android.util.LruCache
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class Cached(val result: Lyrics)

/** Memoises parsed lyrics per song/version and persists user-imported .lrc files. */
class LyricsRepository(
    private val source: LyricsSource,
    private val appContext: Context? = null
) {
    private val cache = object : LruCache<Long, Cached>(64) {}

    suspend fun load(song: SongEntity): Lyrics? {
        cache.get(song.id)?.let { return it.result }
        val result = source.load(song)
        if (result != null && result.lines.isNotEmpty()) {
            cache.put(song.id, Cached(result))
        }
        return result
    }

    fun invalidate(songId: Long) {
        cache.remove(songId)
    }

    suspend fun importLrcFromUri(song: SongEntity, uri: Uri): Lyrics? = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext null
        val content = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@withContext null

        val parsed = LrcParser.parse(content)?.copy(sourceLabel = "Imported .lrc") ?: return@withContext null
        runCatching {
            val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
            File(dir, "${song.id}.lrc").writeText(content, Charsets.UTF_8)
        }
        cache.put(song.id, Cached(parsed))
        parsed
    }
}
