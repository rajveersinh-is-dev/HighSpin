package com.lowspin.app.data.repository

import com.lowspin.app.data.db.AlbumSummary
import com.lowspin.app.data.db.ArtistSummary
import com.lowspin.app.data.db.ExcludedFolderEntity
import com.lowspin.app.data.db.FolderSummary
import com.lowspin.app.data.db.HiddenSongEntity
import com.lowspin.app.data.db.LowspinDatabase
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.RecentSearchEntity
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.Id3Enricher
import com.lowspin.app.data.scanner.MediaStoreScanner
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.data.scanner.TrackFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MusicRepository(
    private val database: LowspinDatabase,
    private val scanner: MediaStoreScanner,
    private val enricher: Id3Enricher,
    private val scope: CoroutineScope
) {

    private val songDao = database.songDao()
    private val excludedFolderDao = database.excludedFolderDao()
    private val hiddenSongDao = database.hiddenSongDao()
    private val recentSearchDao = database.recentSearchDao()

    val songs: Flow<List<SongEntity>> = songDao.observeAll()
    val albums: Flow<List<AlbumSummary>> = songDao.observeAlbums()
    val artists: Flow<List<ArtistSummary>> = songDao.observeArtists()
    val folders: Flow<List<FolderSummary>> = songDao.observeFolders()
    val likedSongs: Flow<List<SongEntity>> = songDao.observeLiked()
    val likedCount: Flow<Int> = songDao.observeLikedCount()
    val recentlyPlayed: Flow<List<SongEntity>> = songDao.observeRecentlyPlayed(20)
    val mostPlayed: Flow<List<SongEntity>> = songDao.observeMostPlayed(20)
    val excludedFolders: Flow<List<ExcludedFolderEntity>> = excludedFolderDao.observeAll()
    val hiddenSongs: Flow<List<HiddenSongEntity>> = hiddenSongDao.observeAll()
    val recentSearches: Flow<List<RecentSearchEntity>> = recentSearchDao.observe()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanMutex = Mutex()
    private var scanJob: Job? = null
    private var pendingRescan = false

    fun refreshLibrary(minDurationMs: Long = MIN_TRACK_DURATION_MS) {
        if (scanJob?.isActive == true) {
            pendingRescan = true
            return
        }
        scanJob = scope.launch {
            do {
                pendingRescan = false
                scanMutex.withLock { runScan(minDurationMs) }
            } while (pendingRescan)
        }
    }

    private suspend fun runScan(minDurationMs: Long) {
        _scanState.value = ScanState.Scanning(0, 0)
        runCatching {
            val excluded = excludedFolderDao.getPaths().toSet()
            val hiddenIds = hiddenSongDao.getIds().toSet()
            val hiddenKeys = hiddenSongDao.getMatchKeys().toSet()
            val result = scanner.scan(
                minDurationMs = minDurationMs,
                excludedFolders = excluded,
                hiddenSongIds = hiddenIds,
                hiddenMatchKeys = hiddenKeys,
                onProgress = { scanned, total ->
                    _scanState.value = ScanState.Scanning(scanned, total)
                }
            )
            enricher.enrich { done, total ->
                _scanState.value = ScanState.Enriching(done, total)
            }
            _scanState.value = ScanState.Complete(
                songCount = result.songCount,
                filteredCount = result.filteredCount,
                finishedAt = System.currentTimeMillis()
            )
        }.onFailure { error ->
            _scanState.value = ScanState.Failed(
                error.message ?: "Could not read the local music library"
            )
        }
    }

    suspend fun ensureLibraryLoaded() {
        if (songDao.count() == 0) {
            refreshLibrary()
        }
    }

    suspend fun setLiked(songId: Long, liked: Boolean) {
        songDao.setLiked(songId, liked)
    }

    /** Hides a song from the library; the audio file itself is never touched. */
    suspend fun hideSong(song: SongEntity) {
        hiddenSongDao.upsert(
            HiddenSongEntity(
                songId = song.id,
                matchKey = song.matchKey,
                title = song.title,
                artist = song.artist,
                folderPath = song.folderPath,
                hiddenAt = System.currentTimeMillis()
            )
        )
        songDao.deleteById(song.id)
        refreshLibrary()
    }

    /** Restores a hidden song and brings it back on the next scan. */
    suspend fun unhideSong(song: HiddenSongEntity) {
        hiddenSongDao.delete(song.songId)
        refreshLibrary()
    }

    suspend fun restoreAllHidden() {
        hiddenSongDao.clear()
        refreshLibrary()
    }

    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity> {
        if (ids.isEmpty()) return emptyList()
        return songDao.getByIds(ids)
    }

    /** Hide or unhide a folder, then rescan so the change is reflected immediately. */
    suspend fun setFolderExcluded(
        folderPath: String,
        folderName: String,
        songCount: Int,
        excluded: Boolean
    ) {
        val normalized = TrackFilter.normalizePath(folderPath)
        if (excluded) {
            excludedFolderDao.upsert(
                ExcludedFolderEntity(
                    folderPath = normalized,
                    folderName = folderName,
                    songCount = songCount,
                    excludedAt = System.currentTimeMillis()
                )
            )
        } else {
            excludedFolderDao.delete(normalized)
        }
        refreshLibrary()
    }

    fun searchSongs(query: String): Flow<List<SongEntity>> {
        val clean = query.trim()
        return if (clean.isEmpty()) flowOf(emptyList()) else songDao.searchSongs(clean)
    }

    fun getAlbumSongs(albumId: Long): Flow<List<SongEntity>> = songDao.observeAlbumSongs(albumId)

    fun getArtistSongs(artist: String): Flow<List<SongEntity>> = songDao.observeArtistSongs(artist)

    fun getFolderSongs(folderPath: String): Flow<List<SongEntity>> = songDao.observeFolderSongs(folderPath)

    fun getRecentlyAdded(since: Long = System.currentTimeMillis() - 30L * 86_400_000L): Flow<List<SongEntity>> =
        songDao.observeRecentlyAdded(since)

    suspend fun saveRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        recentSearchDao.insert(RecentSearchEntity(clean, System.currentTimeMillis()))
    }

    suspend fun removeRecentSearch(query: String) {
        recentSearchDao.delete(query)
    }

    suspend fun clearRecentSearches() {
        recentSearchDao.clear()
    }

    private companion object {
        const val MIN_TRACK_DURATION_MS = 10_000L
    }
}
