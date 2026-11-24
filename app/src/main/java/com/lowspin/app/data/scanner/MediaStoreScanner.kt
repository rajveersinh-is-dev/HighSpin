package com.lowspin.app.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.lowspin.app.data.db.SongDao
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val songCount: Int,
    val filteredCount: Int
)

/**
 * Reads the device audio library in chunks and merges it into Room.
 *
 * The merge deliberately preserves user state (likes, play counts, last played,
 * cached colours) and previously enriched ID3 values for unchanged files.
 * Recorder folders and call/voice-note files are rejected by [TrackFilter] and
 * by the user's excluded-folder list.
 */
@Suppress("DEPRECATION")
class MediaStoreScanner(
    private val context: Context,
    private val songDao: SongDao
) {

    suspend fun scan(
        minDurationMs: Long,
        excludedFolders: Set<String>,
        hiddenSongIds: Set<Long>,
        hiddenMatchKeys: Set<String>,
        onProgress: (scanned: Int, total: Int) -> Unit
    ): ScanResult = withContext(Dispatchers.IO) {
        val token = System.currentTimeMillis()
        val previous = songDao.getAll().associateBy { it.id }
        val excluded = excludedFolders.mapTo(HashSet()) { TrackFilter.normalizePath(it) }
        val seen = HashSet<Long>()
        var filtered = 0
        var queryRows = 0
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // Some OEMs mark music files as IS_MUSIC=0, so accept known
        // audio extensions as well. Durations still filter out short clips.
        val extensionClauses = AUDIO_EXTENSIONS.joinToString(" OR ") { extension ->
            "LOWER(${MediaStore.Audio.Media.DISPLAY_NAME}) LIKE '%$extension'"
        }
        val selection =
            "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR $extensionClauses) " +
                "AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())

        resolver.query(
            collection,
            buildProjection(),
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val total = cursor.count
            queryRows = total
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            var processed = 0
            val batch = ArrayList<SongEntity>(BATCH_SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val displayName = cursor.stringOrNull(MediaStore.Audio.Media.DISPLAY_NAME).orEmpty()
                val rawTitle = cursor.stringOrNull(MediaStore.Audio.Media.TITLE).orEmpty()
                val folderPath = readFolderPath(cursor)

                val key = TrackFilter.matchKey(folderPath, displayName)
                if (id in hiddenSongIds || key in hiddenMatchKeys ||
                    isExcludedFolder(folderPath, excluded) ||
                    TrackFilter.isRecording(folderPath, displayName, rawTitle)
                ) {
                    filtered++
                } else {
                    val song = readSong(cursor, id, token, previous[id], folderPath, displayName)
                    seen.add(id)
                    batch.add(song)
                }
                processed++

                if (batch.size >= BATCH_SIZE) {
                    songDao.upsertAll(batch)
                    batch.clear()
                    onProgress(processed, total)
                } else if (processed % PROGRESS_EVERY == 0) {
                    onProgress(processed, total)
                }
            }

            if (batch.isNotEmpty()) {
                songDao.upsertAll(batch)
            }
            onProgress(processed, total)
        }

        // Prune only when the MediaStore query itself succeeded and returned rows.
        // An empty or failed query must never wipe the local library.
        if (queryRows > 0) {
            songDao.deleteNotSeen(token)
        }
        ScanResult(songCount = seen.size, filteredCount = filtered)
    }

    private fun isExcludedFolder(folderPath: String, excluded: Set<String>): Boolean {
        if (excluded.isEmpty()) return false
        val normalized = TrackFilter.normalizePath(folderPath)
        if (normalized.isEmpty()) return false
        if (normalized in excluded) return true
        return excluded.any { root -> root.isNotEmpty() && normalized.startsWith("$root/") }
    }

    private fun readSong(
        cursor: Cursor,
        id: Long,
        token: Long,
        previous: SongEntity?,
        folderPath: String,
        displayName: String
    ): SongEntity {
        val rawTitle = cursor.stringOrNull(MediaStore.Audio.Media.TITLE)
        val mediaTitle = rawTitle
            ?.takeIf { it.isNotBlank() && !it.equals(UNKNOWN, ignoreCase = true) }
            ?: displayName.substringBeforeLast('.').ifBlank { "Unknown track" }
        val mediaArtist = cursor.stringOrNull(MediaStore.Audio.Media.ARTIST)
            ?.takeIf { it.isNotBlank() && !it.equals(UNKNOWN, ignoreCase = true) }
            ?: UNKNOWN_ARTIST
        val mediaAlbum = cursor.stringOrNull(MediaStore.Audio.Media.ALBUM)
            ?.takeIf { it.isNotBlank() && !it.equals(UNKNOWN, ignoreCase = true) }
            ?: UNKNOWN_ALBUM

        val dateModified = cursor.longOrNull(MediaStore.Audio.Media.DATE_MODIFIED) ?: 0L
        val folderName = folderPath.substringAfterLast('/').ifBlank { "Unknown folder" }
        val sameFile = previous != null && previous.dateModified == dateModified
        val enriched = previous?.takeIf { sameFile && it.metadataEnriched }

        return SongEntity(
            id = id,
            title = enriched?.title ?: mediaTitle,
            artist = enriched?.artist ?: mediaArtist,
            artistId = cursor.longOrNull(MediaStore.Audio.Media.ARTIST_ID) ?: 0L,
            album = enriched?.album ?: mediaAlbum,
            albumId = cursor.longOrNull(MediaStore.Audio.Media.ALBUM_ID) ?: 0L,
            durationMs = cursor.longOrNull(MediaStore.Audio.Media.DURATION) ?: 0L,
            trackNumber = enriched?.trackNumber
                ?: cursor.intOrNull(MediaStore.Audio.Media.TRACK) ?: 0,
            discNumber = enriched?.discNumber ?: 0,
            year = enriched?.year ?: cursor.intOrNull(MediaStore.Audio.Media.YEAR) ?: 0,
            folderPath = folderPath,
            folderName = folderName,
            contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            ).toString(),
            mimeType = cursor.stringOrNull(MediaStore.Audio.Media.MIME_TYPE) ?: "audio/mpeg",
            dateAdded = cursor.longOrNull(MediaStore.Audio.Media.DATE_ADDED) ?: 0L,
            dateModified = dateModified,
            isLiked = previous?.isLiked ?: false,
            playCount = previous?.playCount ?: 0,
            lastPlayedAt = previous?.lastPlayedAt ?: 0L,
            dominantColorArgb = previous?.dominantColorArgb ?: 0,
            metadataEnriched = enriched?.metadataEnriched ?: false,
            lastSeenScan = token,
            matchKey = TrackFilter.matchKey(folderPath, displayName)
        )
    }

    private fun readFolderPath(cursor: Cursor): String {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH).orEmpty()
        } else {
            val data = cursor.stringOrNull(MediaStore.Audio.Media.DATA).orEmpty()
            data.substringBeforeLast('/', "")
        }
        return raw.replace('\\', '/').trim('/')
    }

    private fun buildProjection(): Array<String> {
        val columns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            columns.add(MediaStore.Audio.Media.DATA)
        }
        return columns.toTypedArray()
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getString(index)
    }

    private fun Cursor.longOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getLong(index)
    }

    private fun Cursor.intOrNull(column: String): Int? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getInt(index)
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val PROGRESS_EVERY = 50
        val AUDIO_EXTENSIONS = listOf(
            ".mp3", ".flac", ".wav", ".wave", ".m4a", ".aac", ".ogg", ".opus", ".aif", ".aiff"
        )
        const val UNKNOWN = "<unknown>"
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}
