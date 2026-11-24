package com.lowspin.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.AlbumSummary
import com.lowspin.app.data.db.ArtistSummary
import com.lowspin.app.data.db.FolderSummary
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.data.repository.PlaylistRepository
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlaybackController
import com.lowspin.app.media.PlayerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val player: PlaybackController
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = musicRepository.songs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<PlaylistSummary>> = playlistRepository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val albums: StateFlow<List<AlbumSummary>> = musicRepository.albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val artists: StateFlow<List<ArtistSummary>> = musicRepository.artists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val folders: StateFlow<List<FolderSummary>> = musicRepository.folders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<SongEntity>> = musicRepository.recentlyPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val mostPlayed: StateFlow<List<SongEntity>> = musicRepository.mostPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val scanState: StateFlow<ScanState> = musicRepository.scanState
    val playerState: StateFlow<PlayerUiState> = player.state

    fun onLibraryAccessGranted() {
        viewModelScope.launch { musicRepository.ensureLibraryLoaded() }
    }

    fun refreshLibrary() {
        musicRepository.refreshLibrary()
    }

    fun play(song: SongEntity) {
        val list = songs.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.playQueue(list, index, "Your library")
    }

    fun playQueue(list: List<SongEntity>, startIndex: Int, label: String) {
        if (list.isEmpty()) return
        player.playQueue(list, startIndex.coerceIn(0, list.lastIndex), label)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun next() = player.next()

    fun previous() = player.previous()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun setShuffle(enabled: Boolean) = player.setShuffle(enabled)

    fun cycleRepeat() = player.cycleRepeat()

    fun toggleLike(songId: Long, liked: Boolean) {
        viewModelScope.launch { musicRepository.setLiked(songId, liked) }
    }

    fun playNext(song: SongEntity) = player.playNext(song)

    fun addToQueue(song: SongEntity) = player.addToQueue(song)

    fun createPlaylist(name: String?, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(playlistRepository.createPlaylist(name))
        }
    }

    fun addToPlaylist(playlistId: Long, songId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(playlistRepository.addSong(playlistId, songId))
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    musicRepository = container.musicRepository,
                    playlistRepository = container.playlistRepository,
                    player = container.playbackController
                )
            }
        }
    }
}
