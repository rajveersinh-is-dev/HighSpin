package com.lowspin.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val SQL_BIND_LIMIT = 500

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIdsPage(ids: List<Long>): List<SongEntity>

    /**
     * SQLite caps the number of bound variables per statement, so very large
     * queues (a whole library shuffled) are fetched in pages instead of one
     * `IN (...)` list that could exceed the limit.
     */
    suspend fun getByIds(ids: List<Long>): List<SongEntity> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(SQL_BIND_LIMIT).flatMap { getByIdsPage(it) }
    }

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE lastSeenScan != :token")
    suspend fun deleteNotSeen(token: Long)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: Long)

    @Query("UPDATE songs SET isLiked = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun recordPlay(id: Long, timestamp: Long)

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun markPlayed(id: Long, timestamp: Long)

    @Query("UPDATE songs SET dominantColorArgb = :color WHERE id = :id")
    suspend fun setDominantColor(id: Long, color: Int)

    @Query("SELECT * FROM songs WHERE metadataEnriched = 0")
    suspend fun getPendingEnrichment(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE bpm = 0 OR musicalKey = ''")
    suspend fun getPendingAudioAnalysis(): List<SongEntity>

    @Query("UPDATE songs SET bpm = :bpm, musicalKey = :musicalKey WHERE id = :id")
    suspend fun updateAudioAnalysis(id: Long, bpm: Int, musicalKey: String)

    @Query(
        """
        UPDATE songs
        SET title = :title,
            artist = :artist,
            album = :album,
            trackNumber = :trackNumber,
            discNumber = :discNumber,
            year = :year,
            metadataEnriched = 1
        WHERE id = :id
        """
    )
    suspend fun applyId3(
        id: Long,
        title: String,
        artist: String,
        album: String,
        trackNumber: Int,
        discNumber: Int,
        year: Int
    )

    @Query("UPDATE songs SET metadataEnriched = 1 WHERE id = :id")
    suspend fun markEnrichmentAttempted(id: Long)

    @Query(
        """
        UPDATE songs
        SET title = :title,
            artist = :artist,
            album = :album,
            trackNumber = :trackNumber,
            year = :year
        WHERE id = :id
        """
    )
    suspend fun updateSongTags(
        id: Long,
        title: String,
        artist: String,
        album: String,
        trackNumber: Int,
        year: Int
    )

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeLiked(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs WHERE isLiked = 1")
    fun observeLikedCount(): Flow<Int>

    @Query("SELECT * FROM songs WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs
        WHERE playCount > 0
        ORDER BY playCount DESC, title COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs
        WHERE albumId = :albumId
        ORDER BY discNumber ASC, trackNumber ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeAlbumSongs(albumId: Long): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs
        WHERE artist = :artist
        ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC
        """
    )
    fun observeArtistSongs(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun observeFolderSongs(folderPath: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE dateAdded >= :since ORDER BY dateAdded DESC LIMIT 200")
    fun observeRecentlyAdded(since: Long): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        LIMIT 300
        """
    )
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query(
        """
        SELECT albumId AS albumId,
               album AS album,
               COALESCE(MIN(artist), 'Unknown artist') AS artist,
               COUNT(*) AS songCount,
               COALESCE(SUM(durationMs), 0) AS totalDurationMs,
               COALESCE(MIN(id), 0) AS artSongId,
               COALESCE(MAX(year), 0) AS year
        FROM songs
        GROUP BY albumId, album
        ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT COALESCE(MAX(artistId), 0) AS artistId,
               artist AS artist,
               COUNT(*) AS songCount,
               COUNT(DISTINCT albumId) AS albumCount,
               COALESCE(MIN(id), 0) AS artSongId
        FROM songs
        GROUP BY artist
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT folderName AS folderName,
               folderPath AS folderPath,
               COUNT(*) AS songCount,
               COALESCE(MIN(id), 0) AS artSongId
        FROM songs
        GROUP BY folderName, folderPath
        ORDER BY folderName COLLATE NOCASE ASC
        """
    )
    fun observeFolders(): Flow<List<FolderSummary>>
}

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(ref: PlaylistSongCrossRef): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query(
        """
        UPDATE playlist_songs
        SET position = :position
        WHERE playlistId = :playlistId AND songId = :songId
        """
    )
    suspend fun updatePosition(playlistId: Long, songId: Long, position: Int)

    @Query(
        """
        SELECT songId FROM playlist_songs
        WHERE playlistId = :playlistId
        ORDER BY position ASC
        """
    )
    suspend fun getOrderedSongIds(playlistId: Long): List<Long>

    @Query(
        """
        SELECT songId FROM playlist_songs
        WHERE playlistId = :playlistId
        ORDER BY position ASC
        LIMIT :limit
        """
    )
    fun observeArtSongIds(playlistId: Long, limit: Int): Flow<List<Long>>

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """
    )
    fun observeSongs(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun observeSongCount(playlistId: Long): Flow<Int>

    @Query(
        """
        SELECT s.id FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """
    )
    suspend fun getSongIds(playlistId: Long): List<Long>

    @Query("UPDATE playlists SET customCoverUri = :customCoverUri, gradientColorIndex = :gradientColorIndex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCover(id: Long, customCoverUri: String?, gradientColorIndex: Int?, updatedAt: Long)

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               p.createdAt AS createdAt,
               p.updatedAt AS updatedAt,
               COUNT(ps.songId) AS songCount,
               (SELECT ps2.songId FROM playlist_songs ps2
                WHERE ps2.playlistId = p.id
                ORDER BY ps2.position ASC LIMIT 1) AS artSongId,
               p.customCoverUri AS customCoverUri,
               p.gradientColorIndex AS gradientColorIndex
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
        """
    )
    fun observeSummaries(): Flow<List<PlaylistSummary>>

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               p.createdAt AS createdAt,
               p.updatedAt AS updatedAt,
               COUNT(ps.songId) AS songCount,
               (SELECT ps2.songId FROM playlist_songs ps2
                WHERE ps2.playlistId = p.id
                ORDER BY ps2.position ASC LIMIT 1) AS artSongId,
               p.customCoverUri AS customCoverUri,
               p.gradientColorIndex AS gradientColorIndex
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        WHERE p.name LIKE '%' || :query || '%'
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
        LIMIT 50
        """
    )
    fun searchSummaries(query: String): Flow<List<PlaylistSummary>>
}

@Dao
interface ExcludedFolderDao {

    @Query("SELECT * FROM excluded_folders ORDER BY folderName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT folderPath FROM excluded_folders")
    suspend fun getPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun delete(folderPath: String)
}

@Dao
interface HiddenSongDao {

    @Query("SELECT * FROM hidden_songs ORDER BY hiddenAt DESC")
    fun observeAll(): Flow<List<HiddenSongEntity>>

    @Query("SELECT songId FROM hidden_songs")
    suspend fun getIds(): List<Long>

    @Query("SELECT matchKey FROM hidden_songs WHERE matchKey != ''")
    suspend fun getMatchKeys(): List<String>

    @Upsert
    suspend fun upsert(song: HiddenSongEntity)

    @Query("DELETE FROM hidden_songs WHERE songId = :songId")
    suspend fun delete(songId: Long)

    @Query("DELETE FROM hidden_songs")
    suspend fun clear()
}

@Dao
interface QueueDao {

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getAll(): List<QueueItemEntity>

    @Query("DELETE FROM queue_items")
    suspend fun clear()

    @Insert
    suspend fun insertAll(items: List<QueueItemEntity>)
}

@Dao
interface PlaybackStateDao {

    @Query("SELECT * FROM playback_state WHERE id = 1")
    suspend fun get(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlaybackStateEntity)
}

@Dao
interface RecentSearchDao {

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 10")
    fun observe(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Dao
interface SongEqualizerDao {

    @Query("SELECT * FROM song_equalizers WHERE songId = :songId")
    suspend fun get(songId: Long): SongEqualizerEntity?

    @Upsert
    suspend fun upsert(entity: SongEqualizerEntity)

    @Query("DELETE FROM song_equalizers WHERE songId = :songId")
    suspend fun delete(songId: Long)
}

@Dao
interface PlayEventDao {

    @Insert
    suspend fun insert(event: PlayEventEntity)

    @Query("SELECT SUM(durationListenedMs) FROM play_events")
    fun observeTotalListenedMs(): Flow<Long?>

    @Query("SELECT SUM(durationListenedMs) FROM play_events WHERE playedAt >= :sinceMs")
    fun observeWeeklyListenedMs(sinceMs: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM play_events")
    fun observeTotalPlayCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM play_events WHERE playedAt >= :sinceMs")
    fun observeWeeklyPlayCount(sinceMs: Long): Flow<Int>

    @Query(
        """
        SELECT s.artist AS artist,
               COALESCE(MIN(s.id), 0) AS artSongId,
               COUNT(pe.id) AS playCount,
               COALESCE(SUM(pe.durationListenedMs), 0) AS totalDurationMs
        FROM play_events pe
        INNER JOIN songs s ON pe.songId = s.id
        WHERE pe.playedAt >= :sinceMs
        GROUP BY s.artist
        ORDER BY totalDurationMs DESC, playCount DESC
        LIMIT :limit
        """
    )
    fun observeTopArtistsSince(sinceMs: Long, limit: Int = 10): Flow<List<ArtistPlayStat>>
}

@Dao
interface SmartPlaylistDao {
    @Query("SELECT * FROM smart_playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SmartPlaylistEntity>>

    @Query("SELECT * FROM smart_playlists ORDER BY createdAt DESC")
    suspend fun getAll(): List<SmartPlaylistEntity>

    @Query("SELECT * FROM smart_playlists WHERE id = :id")
    suspend fun getById(id: Long): SmartPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(smartPlaylist: SmartPlaylistEntity): Long

    @Query("DELETE FROM smart_playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY positionMs ASC")
    fun observeForSong(songId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY positionMs ASC")
    suspend fun getForSong(songId: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
