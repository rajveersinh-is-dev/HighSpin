package com.lowspin.app.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.withTransaction
import com.lowspin.app.data.art.AlbumArtRepository
import com.lowspin.app.data.db.LowspinDatabase
import com.lowspin.app.data.db.PlaybackStateEntity
import com.lowspin.app.data.db.QueueItemEntity
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerUiState(
    val connected: Boolean = false,
    val currentSong: SongEntity? = null,
    val currentMediaItemIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val dominantColor: Int = 0,
    val queueSize: Int = 0,
    val sourceLabel: String = "Your library"
)

/**
 * Single app-wide bridge to the MediaSessionService. Lives in AppContainer so
 * playback survives activity recreation and continues in the background.
 */
class PlaybackController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: LowspinDatabase,
    private val artRepository: AlbumArtRepository
) {

    private var controller: MediaController? = null
    private val songCache = LinkedHashMap<Long, SongEntity>()
    private val requestedColors = HashSet<Long>()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var playStatsJob: Job? = null
    private var lastRecordedSongId = -1L
    private var currentSourceLabel = "Your library"
    private var pendingAction: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncState()
            onTrackStarted(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            if (isPlaying) {
                startTicker()
            } else {
                stopTicker()
                persistProgress()
            }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val mediaController = runCatching { future.get() }.getOrNull()
            if (mediaController == null) {
                _state.update { it.copy(connected = false) }
                return@addListener
            }
            controller = mediaController
            mediaController.addListener(playerListener)
            _state.update { it.copy(connected = true) }
            syncState()
            if (mediaController.isPlaying) startTicker()
            pendingAction?.let { action ->
                pendingAction = null
                action()
            }
            scope.launch { restoreIfNeeded(mediaController) }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun restoreIfNeeded(mediaController: MediaController) {
        if (mediaController.mediaItemCount > 0) return

        val queue = database.queueDao().getAll()
        if (queue.isEmpty()) return
        val songs = database.songDao().getByIds(queue.map { it.songId })
        val songMap = songs.associateBy { it.id }
        val ordered = queue.mapNotNull { songMap[it.songId] }
        if (ordered.isEmpty()) return

        val saved = database.playbackStateDao().get() ?: PlaybackStateEntity()
        currentSourceLabel = saved.sourceLabel
        ordered.forEach { songCache[it.id] = it }
        val index = saved.currentQueuePosition.coerceIn(0, ordered.lastIndex)

        withContext(Dispatchers.Main) {
            mediaController.setMediaItems(
                ordered.map { it.toMediaItem() },
                index,
                saved.positionMs.coerceAtLeast(0L)
            )
            mediaController.shuffleModeEnabled = saved.shuffleEnabled
            mediaController.repeatMode = saved.repeatMode
            mediaController.setPlaybackSpeed(saved.playbackSpeed)
            mediaController.prepare()
        }
        syncState()
    }

    private fun syncState() {
        val mediaController = controller ?: return
        val count = mediaController.mediaItemCount
        val missing = ArrayList<Long>()

        for (index in 0 until count) {
            val id = mediaController.getMediaItemAt(index).mediaId.toLongOrNull() ?: continue
            if (!songCache.containsKey(id) && id !in missing) missing.add(id)
        }

        if (missing.isEmpty()) {
            publishState(mediaController)
        } else {
            scope.launch {
                database.songDao().getByIds(missing).forEach { songCache[it.id] = it }
                publishState(mediaController)
            }
        }
    }

    private fun publishState(mediaController: MediaController) {
        val current = currentSong(mediaController)
        val dominant = current?.let { songCache[it.id]?.dominantColorArgb } ?: 0

        _state.update { old ->
            old.copy(
                connected = true,
                currentSong = current,
                currentMediaItemIndex = mediaController.currentMediaItemIndex,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
                positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                durationMs = current?.durationMs?.takeIf { it > 0 }
                    ?: mediaController.duration.coerceAtLeast(0L),
                shuffleEnabled = mediaController.shuffleModeEnabled,
                repeatMode = mediaController.repeatMode,
                playbackSpeed = mediaController.playbackParameters.speed,
                playbackPitch = mediaController.playbackParameters.pitch,
                dominantColor = dominant,
                queueSize = mediaController.mediaItemCount,
                sourceLabel = currentSourceLabel
            )
        }

        if (current != null && current.dominantColorArgb == 0 && requestedColors.add(current.id)) {
            scope.launch {
                val color = artRepository.dominantColor(current)
                songCache[current.id] = current.copy(dominantColorArgb = color)
                if (currentSong(mediaController)?.id == current.id) {
                    _state.update { it.copy(dominantColor = color) }
                }
            }
        }
    }

    private fun currentSong(mediaController: MediaController? = controller): SongEntity? {
        val media = mediaController ?: return null
        if (media.mediaItemCount == 0) return null
        val id = media.currentMediaItem?.mediaId?.toLongOrNull() ?: return null
        return songCache[id]
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val mediaController = controller ?: break
                val song = currentSong(mediaController)
                _state.update {
                    it.copy(
                        positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                        durationMs = song?.durationMs?.takeIf { duration -> duration > 0 }
                            ?: mediaController.duration.coerceAtLeast(0L),
                        isBuffering = mediaController.playbackState == Player.STATE_BUFFERING
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun onTrackStarted(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId?.toLongOrNull() ?: return
        if (id == lastRecordedSongId) return
        lastRecordedSongId = id

        playStatsJob?.cancel()
        playStatsJob = scope.launch {
            delay(3_000)
            if (!isCurrent(id)) return@launch
            val now = System.currentTimeMillis()
            database.songDao().markPlayed(id, now)
            songCache[id]?.let { songCache[id] = it.copy(lastPlayedAt = now) }

            delay(7_000)
            if (isCurrent(id) && controller?.isPlaying == true) {
                database.songDao().recordPlay(id, System.currentTimeMillis())
                songCache[id]?.let { songCache[id] = it.copy(playCount = it.playCount + 1) }
            }
        }
    }

    private fun isCurrent(songId: Long): Boolean =
        controller?.currentMediaItem?.mediaId?.toLongOrNull() == songId

    fun playQueue(songs: List<SongEntity>, startIndex: Int, sourceLabel: String) {
        val mediaController = controller
        if (mediaController == null) {
            // A tap that lands before the service binds should still start playback.
            pendingAction = { playQueue(songs, startIndex, sourceLabel) }
            return
        }
        if (songs.isEmpty()) return

        val index = startIndex.coerceIn(0, songs.lastIndex)
        songs.forEach { songCache[it.id] = it }
        currentSourceLabel = sourceLabel
        lastRecordedSongId = -1L

        mediaController.setMediaItems(songs.map { it.toMediaItem() }, index, 0L)
        mediaController.prepare()
        mediaController.play()

        scope.launch {
            database.withTransaction {
                database.queueDao().clear()
                database.queueDao().insertAll(
                    songs.mapIndexed { position, song ->
                        QueueItemEntity(
                            songId = song.id,
                            position = position,
                            originalIndex = position,
                            addedAt = System.currentTimeMillis()
                        )
                    }
                )
            }
            val existing = database.playbackStateDao().get() ?: PlaybackStateEntity()
            database.playbackStateDao().upsert(
                existing.copy(
                    sourceLabel = sourceLabel,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        syncState()
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            if (mediaController.playbackState == Player.STATE_IDLE) {
                mediaController.prepare()
            }
            mediaController.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        controller?.seekTo(safePosition)
        _state.update { it.copy(positionMs = safePosition) }
    }

    fun setShuffle(enabled: Boolean) {
        val mediaController = controller ?: return
        mediaController.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffleEnabled = enabled) }
        persistModes()
    }

    fun cycleRepeat() {
        val mediaController = controller ?: return
        mediaController.repeatMode = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _state.update { it.copy(repeatMode = mediaController.repeatMode) }
        persistModes()
    }

    fun setSpeed(speed: Float) {
        val mediaController = controller ?: return
        val currentPitch = mediaController.playbackParameters.pitch
        val safeSpeed = speed.coerceIn(0.5f, 2.5f)
        mediaController.playbackParameters = androidx.media3.common.PlaybackParameters(safeSpeed, currentPitch)
        _state.update { it.copy(playbackSpeed = safeSpeed) }
        persistModes()
    }

    fun setPlaybackSpeed(speed: Float) = setSpeed(speed)

    fun setPitch(pitch: Float) {
        val mediaController = controller ?: return
        val currentSpeed = mediaController.playbackParameters.speed
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        mediaController.playbackParameters = androidx.media3.common.PlaybackParameters(currentSpeed, safePitch)
        _state.update { it.copy(playbackPitch = safePitch) }
    }

    fun setPlaybackPitch(pitch: Float) = setPitch(pitch)

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        val mediaController = controller ?: return
        val safeSpeed = speed.coerceIn(0.5f, 2.5f)
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        mediaController.playbackParameters = androidx.media3.common.PlaybackParameters(safeSpeed, safePitch)
        _state.update { it.copy(playbackSpeed = safeSpeed, playbackPitch = safePitch) }
        persistModes()
    }

    fun addToQueue(song: SongEntity) {
        val mediaController = controller ?: return
        songCache[song.id] = song
        mediaController.addMediaItem(song.toMediaItem())
        persistQueueFromPlayer()
    }

    fun playNext(song: SongEntity) {
        val mediaController = controller ?: return
        songCache[song.id] = song
        val insertIndex = (mediaController.currentMediaItemIndex + 1)
            .coerceIn(0, mediaController.mediaItemCount)
        mediaController.addMediaItem(insertIndex, song.toMediaItem())
        persistQueueFromPlayer()
    }

    fun removeQueueItem(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.removeMediaItem(index)
            persistQueueFromPlayer()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val mediaController = controller ?: return
        val count = mediaController.mediaItemCount
        if (fromIndex in 0 until count && toIndex in 0 until count && fromIndex != toIndex) {
            mediaController.moveMediaItem(fromIndex, toIndex)
            persistQueueFromPlayer()
        }
    }

    fun clearUpcomingQueue() {
        val mediaController = controller ?: return
        val currentIndex = mediaController.currentMediaItemIndex
        val total = mediaController.mediaItemCount
        if (currentIndex in 0 until total - 1) {
            mediaController.removeMediaItems(currentIndex + 1, total)
            persistQueueFromPlayer()
        }
    }

    fun getQueueSongs(): List<SongEntity> {
        val mediaController = controller ?: return emptyList()
        val count = mediaController.mediaItemCount
        return (0 until count).mapNotNull { index ->
            val id = mediaController.getMediaItemAt(index).mediaId.toLongOrNull()
            if (id != null) songCache[id] else null
        }
    }

    fun toggleLike(songId: Long, liked: Boolean) {
        scope.launch { database.songDao().setLiked(songId, liked) }
    }

    private fun persistQueueFromPlayer() {
        val mediaController = controller ?: return
        val ids = (0 until mediaController.mediaItemCount).mapNotNull {
            mediaController.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        scope.launch {
            database.withTransaction {
                database.queueDao().clear()
                database.queueDao().insertAll(
                    ids.mapIndexed { position, songId ->
                        QueueItemEntity(
                            songId = songId,
                            position = position,
                            originalIndex = position,
                            addedAt = System.currentTimeMillis()
                        )
                    }
                )
            }
        }
    }

    private fun persistModes() {
        val mediaController = controller ?: return
        val shuffle = mediaController.shuffleModeEnabled
        val repeat = mediaController.repeatMode
        val speed = mediaController.playbackParameters.speed
        scope.launch {
            val existing = database.playbackStateDao().get() ?: PlaybackStateEntity()
            database.playbackStateDao().upsert(
                existing.copy(
                    shuffleEnabled = shuffle,
                    repeatMode = repeat,
                    playbackSpeed = speed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun persistProgress() {
        val mediaController = controller ?: return
        if (mediaController.mediaItemCount == 0) return
        val index = mediaController.currentMediaItemIndex
        val position = mediaController.currentPosition
        val shuffle = mediaController.shuffleModeEnabled
        val repeat = mediaController.repeatMode
        val speed = mediaController.playbackParameters.speed

        scope.launch {
            val existing = database.playbackStateDao().get() ?: PlaybackStateEntity()
            database.playbackStateDao().upsert(
                existing.copy(
                    currentQueuePosition = index,
                    positionMs = position,
                    shuffleEnabled = shuffle,
                    repeatMode = repeat,
                    playbackSpeed = speed,
                    sourceLabel = currentSourceLabel,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
