package com.lowspin.app.ui.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ArtistAlbumItem(
    val albumId: Long,
    val albumTitle: String,
    val coverSongId: Long,
    val year: Int
)

class ArtistViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
    val artistName: String
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = musicRepository.getArtistSongs(artistName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val topSongs: StateFlow<List<SongEntity>> = songs.map { list ->
        list.sortedWith(
            compareByDescending<SongEntity> { it.playCount }
                .thenByDescending { it.lastPlayedAt }
                .thenBy { it.title }
        ).take(5)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val albums: StateFlow<List<ArtistAlbumItem>> = songs.map { list ->
        list.groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                ArtistAlbumItem(
                    albumId = albumId,
                    albumTitle = first.album.ifBlank { "Unknown Album" },
                    coverSongId = first.id,
                    year = albumSongs.map { it.year }.filter { it > 0 }.minOrNull() ?: 0
                )
            }
            .sortedByDescending { it.year }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun play(startIndex: Int = 0) {
        val list = songs.value
        if (list.isEmpty()) return
        playbackController.playQueue(list, startIndex, artistName)
    }

    fun playSong(song: SongEntity) {
        val list = songs.value
        val index = list.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        playbackController.playQueue(list, index, artistName)
    }

    fun shufflePlay() {
        val list = songs.value.shuffled()
        if (list.isEmpty()) return
        playbackController.setShuffle(true)
        playbackController.playQueue(list, 0, artistName)
    }

    companion object {
        fun factory(container: AppContainer, artistName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ArtistViewModel(
                    musicRepository = container.musicRepository,
                    playbackController = container.playbackController,
                    artistName = artistName
                )
            }
        }
    }
}
