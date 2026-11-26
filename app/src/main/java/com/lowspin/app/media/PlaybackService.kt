package com.lowspin.app.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lowspin.app.MainActivity
import com.lowspin.app.LowspinApplication
import com.lowspin.app.R
import com.lowspin.app.data.db.PlaybackStateEntity
import com.lowspin.app.ui.settings.AppearanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID
        const val FADE_DURATION_MS = 5_000L
        const val TIMER_TICK_MS = 250L
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val smartShuffle = SmartShuffleManager()
    private var persistJob: Job? = null
    private var sensorManager: android.hardware.SensorManager? = null
    private var accelerometer: android.hardware.Sensor? = null
    private var sensorListener: android.hardware.SensorEventListener? = null
    private var lastShakeTimestamp = 0L

    private val container: com.lowspin.app.di.AppContainer
        get() = (application as LowspinApplication).container

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (smartShuffle.shouldSkipImmediateRepeat(
                        nextIndex = index,
                        reason = reason,
                        shuffleEnabled = player.shuffleModeEnabled,
                        repeatMode = player.repeatMode,
                        queueSize = player.mediaItemCount
                    )
                ) {
                    player.seekToNextMediaItem()
                    return
                }
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    container.sleepTimer.consumeTrackEnd()
                ) {
                    player.playWhenReady = false
                }
                smartShuffle.onPlayed(index)
                persistProgress()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persistProgress()
            }
        })

        createNotificationChannel()
        HighSpinLiveNotificationProvider.publishPlaybackShortcut(this)
        val notificationProvider = HighSpinLiveNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()

        container.equalizerManager.attach(player.audioSessionId)
        startEqualizerObserver()
        startSleepTimer()
        startPersistLoop()
        startLiveNotificationObserver()
        startGestureObserver()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Clean up legacy and previous channels that were stuck on lower importance
        for (oldChannelId in HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS) {
            runCatching {
                manager.deleteNotificationChannel(oldChannelId)
            }
        }

        // If the current channel exists with importance lower than IMPORTANCE_HIGH, delete it so it can be recreated
        val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null && existing.importance < NotificationManager.IMPORTANCE_HIGH) {
            runCatching {
                manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
            }
        }

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "HighSpin live playback controls, status-bar updates, and notification bubbles"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(true)
            }
        }
        manager.createNotificationChannel(channel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val convChannelId = HighSpinLiveNotificationProvider.CONVERSATION_CHANNEL_ID
            val existingConv = manager.getNotificationChannel(convChannelId)
            if (existingConv != null && (existingConv.importance < NotificationManager.IMPORTANCE_HIGH || !existingConv.canBubble())) {
                runCatching {
                    manager.deleteNotificationChannel(convChannelId)
                }
            }

            val conversationChannel = NotificationChannel(
                convChannelId,
                "Now Playing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "HighSpin playback bubble conversation"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
                setAllowBubbles(true)
                setConversationId(NOTIFICATION_CHANNEL_ID, HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID)
            }
            manager.createNotificationChannel(conversationChannel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistJob?.cancel()
        sensorListener?.let { sensorManager?.unregisterListener(it) }
        runCatching {
            getSystemService(NotificationManager::class.java)
                ?.cancel(HighSpinLiveNotificationProvider.BUBBLE_NOTIFICATION_ID)
        }
        runCatching { runBlocking(Dispatchers.IO) { saveProgress() } }
        container.equalizerManager.release()
        mediaSession?.release()
        mediaSession = null
        if (::player.isInitialized) player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun startEqualizerObserver() {
        scope.launch {
            container.equalizerSettings.enabled.collect {
                container.equalizerManager.applyAll()
            }
        }
        scope.launch {
            container.equalizerSettings.preset.collect { preset ->
                container.equalizerManager.applyEqualizer(
                    container.equalizerSettings.enabled.value,
                    preset
                )
            }
        }
        scope.launch {
            container.equalizerSettings.bassBoost.collect { bass ->
                container.equalizerManager.applyBassBoost(
                    container.equalizerSettings.enabled.value,
                    bass
                )
            }
        }
        scope.launch {
            container.equalizerSettings.virtualizer.collect { virt ->
                container.equalizerManager.applyVirtualizer(
                    container.equalizerSettings.enabled.value,
                    virt
                )
            }
        }
        scope.launch {
            container.equalizerSettings.loudnessGain.collect { gain ->
                container.equalizerManager.applyLoudness(
                    container.equalizerSettings.enabled.value,
                    gain
                )
            }
        }
    }

    private fun startSleepTimer() {
        scope.launch {
            container.sleepTimer.endsAt.collect { endsAt ->
                if (endsAt <= 0L) {
                    if (::player.isInitialized) player.setVolume(1f)
                    return@collect
                }
                while (isActive && container.sleepTimer.endsAt.value == endsAt) {
                    val remaining = endsAt - System.currentTimeMillis()
                    if (remaining <= 0L) {
                        container.sleepTimer.cancel()
                        if (::player.isInitialized) {
                            player.setVolume(1f)
                            player.playWhenReady = false
                        }
                        break
                    }
                    if (::player.isInitialized) {
                        val fade = remaining.coerceAtMost(FADE_DURATION_MS)
                        player.setVolume(fade.toFloat() / FADE_DURATION_MS)
                    }
                    delay(TIMER_TICK_MS)
                }
            }
        }
    }

    private fun startPersistLoop() {
        persistJob = scope.launch {
            while (isActive) {
                delay(5_000)
                if (::player.isInitialized && player.isPlaying) persistProgress()
            }
        }
    }

    private fun startLiveNotificationObserver() {
        val appearance = AppearanceSettings(this)
        scope.launch {
            appearance.liveNotifications.collect { enabled ->
                if (!enabled) {
                    runCatching {
                        getSystemService(NotificationManager::class.java)
                            ?.cancel(HighSpinLiveNotificationProvider.BUBBLE_NOTIFICATION_ID)
                    }
                }
                if (::player.isInitialized && player.mediaItemCount > 0) {
                    runCatching {
                        setMediaNotificationProvider(HighSpinLiveNotificationProvider(this@PlaybackService))
                    }
                }
            }
        }
    }

    private fun startGestureObserver() {
        val appearance = AppearanceSettings(this)
        sensorManager = getSystemService(android.hardware.SensorManager::class.java)
        accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)

        sensorListener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event == null || !::player.isInitialized) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Shake to skip
                if (appearance.shakeToSkip.value) {
                    val gX = x / android.hardware.SensorManager.GRAVITY_EARTH
                    val gY = y / android.hardware.SensorManager.GRAVITY_EARTH
                    val gZ = z / android.hardware.SensorManager.GRAVITY_EARTH
                    val gForce = kotlin.math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                    if (gForce > 2.6f) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTimestamp > 1400L) {
                            lastShakeTimestamp = now
                            if (player.mediaItemCount > 0) {
                                player.seekToNextMediaItem()
                                runCatching {
                                    val vibrator = getSystemService(android.os.Vibrator::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator?.vibrate(45)
                                    }
                                }
                            }
                        }
                    }
                }

                // Flip to pause
                if (appearance.flipToPause.value && z < -8.5f && kotlin.math.abs(x) < 3.5f && kotlin.math.abs(y) < 3.5f) {
                    if (player.playWhenReady && player.mediaItemCount > 0) {
                        player.playWhenReady = false
                    }
                }
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
        }

        scope.launch {
            kotlinx.coroutines.flow.combine(
                appearance.shakeToSkip,
                appearance.flipToPause
            ) { shake, flip -> shake || flip }.collect { needed ->
                if (needed) {
                    accelerometer?.let { sensor ->
                        sensorManager?.registerListener(
                            sensorListener,
                            sensor,
                            android.hardware.SensorManager.SENSOR_DELAY_UI
                        )
                    }
                } else {
                    sensorListener?.let { sensorManager?.unregisterListener(it) }
                }
            }
        }
    }

    private fun persistProgress() {
        scope.launch { saveProgress() }
    }

    private suspend fun saveProgress() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val index = player.currentMediaItemIndex
        if (index < 0 || player.currentMediaItem == null) return

        val position = player.currentPosition
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        val speed = player.playbackParameters.speed
        val pitch = player.playbackParameters.pitch

        val db = container.database
        val existing = db.playbackStateDao().get() ?: PlaybackStateEntity()
        db.playbackStateDao().upsert(
            existing.copy(
                currentQueuePosition = index,
                positionMs = position,
                shuffleEnabled = shuffle,
                repeatMode = repeat,
                playbackSpeed = speed,
                playbackPitch = pitch,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun buildResumption(): MediaSession.MediaItemsWithStartPosition {
        val db = container.database
        val queue = db.queueDao().getAll()
        check(queue.isNotEmpty()) { "No saved queue" }

        val songs = db.songDao().getByIds(queue.map { it.songId }).associateBy { it.id }
        val ordered = queue.mapNotNull { songs[it.songId] }
        check(ordered.isNotEmpty()) { "Saved queue has no playable songs" }

        val saved = db.playbackStateDao().get() ?: PlaybackStateEntity()
        return MediaSession.MediaItemsWithStartPosition(
            ordered.map { it.toMediaItem() },
            saved.currentQueuePosition.coerceIn(0, ordered.lastIndex),
            saved.positionMs.coerceAtLeast(0L)
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                runCatching { buildResumption() }
                    .onSuccess { future.set(it) }
                    .onFailure { future.setException(it) }
            }
            return future
        }
    }
}
