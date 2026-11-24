package com.lowspin.app.ui.screens.folderdetail

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
import kotlinx.coroutines.flow.stateIn

class FolderDetailViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
    val folderPath: String
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = musicRepository.getFolderSongs(folderPath)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun play(startIndex: Int = 0) {
        val list = songs.value
        if (list.isEmpty()) return
        val folderName = folderPath.substringAfterLast('/').ifBlank { "Folder" }
        playbackController.playQueue(list, startIndex, folderName)
    }

    fun shufflePlay() {
        val list = songs.value.shuffled()
        if (list.isEmpty()) return
        val folderName = folderPath.substringAfterLast('/').ifBlank { "Folder" }
        playbackController.setShuffle(true)
        playbackController.playQueue(list, 0, folderName)
    }

    companion object {
        fun factory(container: AppContainer, folderPath: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FolderDetailViewModel(
                    musicRepository = container.musicRepository,
                    playbackController = container.playbackController,
                    folderPath = folderPath
                )
            }
        }
    }
}
