package com.lowspin.app.data.repository

import androidx.room.withTransaction
import com.lowspin.app.data.db.LowspinDatabase
import com.lowspin.app.data.db.PlaylistEntity
import com.lowspin.app.data.db.PlaylistSongCrossRef
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * All playlist mutations. Positions are kept dense (0..n-1) so the saved order
 * survives restarts and drag-reordering is a simple index update.
 */
class PlaylistRepository(
    private val database: LowspinDatabase
) {

    private val dao = database.playlistDao()

    val playlists: Flow<List<PlaylistSummary>> = dao.observeSummaries()

    fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?> = dao.observeById(playlistId)

    fun observeSongs(playlistId: Long): Flow<List<SongEntity>> = dao.observeSongs(playlistId)

    fun observeArtSongIds(playlistId: Long): Flow<List<Long>> =
        dao.observeArtSongIds(playlistId, limit = 4)

    fun search(query: String): Flow<List<PlaylistSummary>> {
        val clean = query.trim()
        return if (clean.isEmpty()) flowOf(emptyList()) else dao.searchSummaries(clean)
    }

    suspend fun createPlaylist(name: String? = null): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val finalName = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: "My Playlist #${dao.count() + 1}"
        dao.insert(PlaylistEntity(name = finalName, createdAt = now, updatedAt = now))
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        dao.rename(playlistId, clean, System.currentTimeMillis())
    }

    suspend fun updateCover(playlistId: Long, customCoverUri: String?, gradientColorIndex: Int?) {
        dao.updateCover(playlistId, customCoverUri, gradientColorIndex, System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.delete(playlistId)
    }

    /** Returns false when the song was already in the playlist. */
    suspend fun addSong(playlistId: Long, songId: Long): Boolean = database.withTransaction {
        val now = System.currentTimeMillis()
        val position = dao.nextPosition(playlistId)
        val rowId = dao.addSong(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = position,
                addedAt = now
            )
        )
        if (rowId != -1L) dao.touch(playlistId, now)
        rowId != -1L
    }

    /** Batch adds songs to a playlist, preserving order and skipping duplicates. */
    suspend fun addSongs(playlistId: Long, songIds: List<Long>): Int = database.withTransaction {
        var added = 0
        val now = System.currentTimeMillis()
        var position = dao.nextPosition(playlistId)
        for (songId in songIds) {
            val rowId = dao.addSong(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = position,
                    addedAt = now
                )
            )
            if (rowId != -1L) {
                position++
                added++
            }
        }
        if (added > 0) dao.touch(playlistId, now)
        added
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = database.withTransaction {
        dao.removeSong(playlistId, songId)
        normalizePositions(playlistId)
        dao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) = database.withTransaction {
        orderedSongIds.forEachIndexed { index, songId ->
            dao.updatePosition(playlistId, songId, index)
        }
        dao.touch(playlistId, System.currentTimeMillis())
    }

    private suspend fun normalizePositions(playlistId: Long) {
        dao.getOrderedSongIds(playlistId).forEachIndexed { index, songId ->
            dao.updatePosition(playlistId, songId, index)
        }
    }
}
