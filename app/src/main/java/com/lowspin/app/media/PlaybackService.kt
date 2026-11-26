package com.lowspin.app.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "com.lowspin.app.playback.live.v10"
        const val FADE_DURATION_MS = 5_000L
        const val TIMER_TICK_MS = 250L

        const val SHAKE_G_FORCE_THRESHOLD = 1.65f
        const val SHAKE_DEBOUNCE_MS = 1200L
        const val FLIP_Z_THRESHOLD = -6.5f
        const val FLIP_PLANAR_MAX = 6.5f
        const val FLIP_RESET_Z_THRESHOLD = -2.0f

        fun isShakeGesture(gForce: Float): Boolean = gForce > SHAKE_G_FORCE_THRESHOLD

        fun isFaceDownOrientation(x: Float, y: Float, z: Float): Boolean {
            val planar = kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
            return z < FLIP_Z_THRESHOLD && planar < FLIP_PLANAR_MAX
        }

        fun isFaceDownReset(z: Float): Boolean = z > FLIP_RESET_Z_THRESHOLD

        fun findFrameworkMediaSession(obj: Any?): android.media.session.MediaSession? {
            if (obj == null) return null
            if (obj is android.media.session.MediaSession) return obj

            var currClass: Class<*>? = obj.javaClass
            while (currClass != null && currClass != Any::class.java) {
                for (field in currClass.declaredFields) {
                    if (android.media.session.MediaSession::class.java.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        val found = runCatching { field.get(obj) as? android.media.session.MediaSession }.getOrNull()
                        if (found != null) return found
                    }
                }
                currClass = currClass.superclass
            }

            for (nestedName in listOf("impl", "sessionCompat", "mediaSessionCompat", "mImpl", "mSessionFwk", "mSession")) {
                val nestedObj = runCatching {
                    HighSpinLiveNotificationProvider.findDeclaredField(obj.javaClass, nestedName)?.let { field ->
                        field.isAccessible = true
                        field.get(obj)
                    }
                }.getOrNull()
                if (nestedObj != null && nestedObj !== obj) {
                    val found = findFrameworkMediaSession(nestedObj)
                    if (found != null) return found
                }
            }
            return null
        }
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
    private var isFaceDown = false
    private var sensorListenerRegistered = false
    private var gestureWakeLock: android.os.PowerManager.WakeLock? = null
    private var loopRangeA: Long? = null
    private var loopRangeB: Long? = null
    private var loopJob: Job? = null
    private var lastSeekToLoopStart = 0L
    private var noisyReceiver: BroadcastReceiver? = null

    private val container: com.lowspin.app.di.AppContainer
        get() = (application as LowspinApplication).container

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Android 14+: foreground service must call startForeground() within 5 seconds.
        // Post a minimal placeholder immediately so the system doesn't kill us.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val placeholder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_vinyl)
                .setContentTitle("HighSpin")
                .setContentText("Starting playback…")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    placeholder,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, placeholder)
            }
        }

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
                syncMediaSessionMetadata()

                val songId = mediaItem?.mediaId?.toLongOrNull()
                if (songId != null) {
                    scope.launch {
                        val songEq = container.database.songEqualizerDao().get(songId)
                        if (songEq != null) {
                            container.equalizerSettings.applySongEqualizer(songEq)
                        } else {
                            container.equalizerSettings.restoreGlobalEqualizer()
                        }
                    }
                } else {
                    container.equalizerSettings.restoreGlobalEqualizer()
                }

                updateVolumeNormalization(songId, player.duration.coerceAtLeast(0L))
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val start = loopRangeA
                val end = loopRangeB
                if (start != null && end != null && newPosition.positionMs >= end) {
                    val now = System.currentTimeMillis()
                    if (now - lastSeekToLoopStart > 250L) {
                        lastSeekToLoopStart = now
                        player.seekTo(start)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persistProgress()
                syncMediaSessionMetadata()
                updateGestureState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncMediaSessionMetadata()
                updateGestureState()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                syncMediaSessionMetadata()
                updateGestureState()
            }
        })

        HighSpinLiveNotificationProvider.publishPlaybackShortcut(this)
        setMediaNotificationProvider(
            SafeNotificationProvider(
                primary = HighSpinLiveNotificationProvider(this),
                fallback = DefaultMediaNotificationProvider.Builder(this).build()
            )
        )

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

        syncMediaSessionMetadata()

        container.equalizerManager.attach(player.audioSessionId)
        startEqualizerObserver()
        startSleepTimer()
        startPersistLoop()
        startLiveNotificationObserver()
        startGestureObserver()
        registerHeadsetAndBluetoothReceiver()
        startLoopChecker()
        startLoopObserver()
    }

    private fun createNotificationChannel() {
        LowspinApplication.createNotificationChannels(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            HighSpinLiveNotificationProvider.ACTION_PLAY_PAUSE -> {
                if (::player.isInitialized) {
                    player.playWhenReady = !player.playWhenReady
                }
            }
            HighSpinLiveNotificationProvider.ACTION_NEXT -> {
                if (::player.isInitialized && player.mediaItemCount > 0) {
                    player.seekToNextMediaItem()
                }
            }
            HighSpinLiveNotificationProvider.ACTION_PREV -> {
                if (::player.isInitialized && player.mediaItemCount > 0) {
                    player.seekToPreviousMediaItem()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        persistJob?.cancel()
        loopJob?.cancel()
        unregisterHeadsetAndBluetoothReceiver()
        updateGestureWakeLock(false)
        if (sensorListenerRegistered) {
            sensorListener?.let { sensorManager?.unregisterListener(it) }
            sensorListenerRegistered = false
        }
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

    private fun startLoopObserver() {
        scope.launch {
            container.playbackController.loopRange.collect { range ->
                loopRangeA = range?.first
                loopRangeB = range?.second
            }
        }
    }

    private fun startLoopChecker() {
        loopJob = scope.launch {
            while (isActive) {
                delay(75L)
                if (::player.isInitialized && player.isPlaying) {
                    val start = loopRangeA
                    val end = loopRangeB
                    if (start != null && end != null && player.currentPosition >= end) {
                        val now = System.currentTimeMillis()
                        if (now - lastSeekToLoopStart > 250L) {
                            lastSeekToLoopStart = now
                            player.seekTo(start)
                        }
                    }
                }
            }
        }
    }

    private fun registerHeadsetAndBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        if (container.appearanceSettings.pauseOnHeadsetDisconnect.value) {
                            if (::player.isInitialized && player.isPlaying) {
                                player.playWhenReady = false
                            }
                        }
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        if (container.appearanceSettings.autoResumeBluetooth.value) {
                            if (::player.isInitialized && !player.isPlaying && player.mediaItemCount > 0) {
                                player.playWhenReady = true
                            }
                        }
                    }
                }
            }
        }
        noisyReceiver = receiver
        runCatching {
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    private fun unregisterHeadsetAndBluetoothReceiver() {
        noisyReceiver?.let {
            runCatching { unregisterReceiver(it) }
            noisyReceiver = null
        }
    }

    private fun updateVolumeNormalization(songId: Long?, durationMs: Long) {
        val enabled = container.appearanceSettings.volumeNormalization.value
        if (enabled && songId != null) {
            val gainMb = ReplayGainCalculator.computeTrackNormalization(songId, durationMs)
            container.equalizerManager.applyNormalization(enabled = true, gainMb = gainMb)
        } else {
            container.equalizerManager.applyNormalization(enabled = false)
        }
    }

    private fun startEqualizerObserver() {
        scope.launch {
            container.appearanceSettings.volumeNormalization.collect { enabled ->
                val songId = if (::player.isInitialized) player.currentMediaItem?.mediaId?.toLongOrNull() else null
                val duration = if (::player.isInitialized) player.duration.coerceAtLeast(0L) else 0L
                updateVolumeNormalization(songId, duration)
            }
        }
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

    private fun triggerHapticFeedback(durationMs: Long = 45L) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
                    ?: getSystemService(android.os.Vibrator::class.java)
            } else {
                getSystemService(android.os.Vibrator::class.java)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }
    }

    private fun updateGestureWakeLock(needed: Boolean) {
        runCatching {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (gestureWakeLock == null && pm != null) {
                gestureWakeLock = pm.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "highspin:sensor_gestures"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (needed) {
                if (gestureWakeLock?.isHeld != true) {
                    gestureWakeLock?.acquire()
                }
            } else {
                if (gestureWakeLock?.isHeld == true) {
                    gestureWakeLock?.release()
                }
            }
        }
    }

    private fun updateGestureState() {
        val appearance = container.appearanceSettings
        val gesturesEnabled = appearance.shakeToSkip.value || appearance.flipToPause.value
        val listener = sensorListener
        if (gesturesEnabled && listener != null) {
            if (!sensorListenerRegistered) {
                accelerometer?.let { sensor ->
                    sensorManager?.registerListener(
                        listener,
                        sensor,
                        android.hardware.SensorManager.SENSOR_DELAY_GAME
                    )
                    sensorListenerRegistered = true
                }
            }
        } else {
            if (sensorListenerRegistered) {
                sensorListener?.let { sensorManager?.unregisterListener(it) }
                sensorListenerRegistered = false
            }
            isFaceDown = false
        }

        val isPlaying = ::player.isInitialized && (player.isPlaying || player.playWhenReady)
        val wakeLockNeeded = gesturesEnabled && isPlaying
        updateGestureWakeLock(wakeLockNeeded)
    }

    private fun startPersistLoop() {
        persistJob = scope.launch {
            while (isActive) {
                delay(5_000)
                if (::player.isInitialized && player.isPlaying) {
                    persistProgress()
                    syncMediaSessionMetadata()
                }
            }
        }
    }

    private fun startLiveNotificationObserver() {
        val appearance = container.appearanceSettings
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
        val appearance = container.appearanceSettings
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

                    if (isShakeGesture(gForce)) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTimestamp > SHAKE_DEBOUNCE_MS) {
                            lastShakeTimestamp = now
                            if (player.mediaItemCount > 0) {
                                player.seekToNextMediaItem()
                                triggerHapticFeedback(45L)
                            }
                        }
                    }
                }

                // Flip to pause
                if (appearance.flipToPause.value) {
                    if (isFaceDownOrientation(x, y, z)) {
                        if (!isFaceDown) {
                            isFaceDown = true
                            if (player.playWhenReady && player.mediaItemCount > 0) {
                                player.playWhenReady = false
                                triggerHapticFeedback(45L)
                            }
                        }
                    } else if (isFaceDownReset(z)) {
                        isFaceDown = false
                    }
                } else {
                    isFaceDown = false
                }
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
        }

        scope.launch {
            kotlinx.coroutines.flow.combine(
                appearance.shakeToSkip,
                appearance.flipToPause
            ) { _, _ -> }.collect {
                updateGestureState()
            }
        }
        updateGestureState()
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
        private var headsetHookPressCount = 0
        private var headsetHookResetJob: Job? = null

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent
            }
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        headsetHookPressCount++
                        headsetHookResetJob?.cancel()
                        headsetHookResetJob = scope.launch {
                            delay(350L)
                            if (::player.isInitialized) {
                                when (headsetHookPressCount) {
                                    1 -> {
                                        if (player.isPlaying) player.pause() else player.play()
                                    }
                                    2 -> player.seekToNextMediaItem()
                                    3 -> player.seekToPreviousMediaItem()
                                    else -> {
                                        if (player.isPlaying) player.pause() else player.play()
                                    }
                                }
                            }
                            headsetHookPressCount = 0
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (::player.isInitialized) {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        if (::player.isInitialized) player.play()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (::player.isInitialized) player.pause()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                        if (::player.isInitialized) player.seekToNextMediaItem()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                        if (::player.isInitialized) player.seekToPreviousMediaItem()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        if (::player.isInitialized) player.stop()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        if (::player.isInitialized) {
                            val newPos = (player.currentPosition + 15_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                            player.seekTo(newPos)
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        if (::player.isInitialized) {
                            val newPos = (player.currentPosition - 15_000L).coerceAtLeast(0L)
                            player.seekTo(newPos)
                        }
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

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

    private fun syncMediaSessionMetadata() {
        val session = mediaSession ?: return
        if (!::player.isInitialized) return

        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Local Audio"
        val album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull() ?: 0L
        val duration = player.duration.coerceAtLeast(0L)
        val isPlaying = player.isPlaying

        val lyricJson = metadata.extras?.getString("lyricInfo")
            ?: currentMediaItem?.mediaMetadata?.extras?.getString("lyricInfo")
            ?: buildLyricInfoJson(title, artist, album, songId)

        val sessionExtras = android.os.Bundle().apply {
            putString("lyricInfo", lyricJson)
            putString("lyric_info", lyricJson)
            putString("oplus_lyric_info", lyricJson)
            putString("songName", title)
            putString("artist", artist)
            putString("album", album)
            putBoolean("is_live_media", true)
            putBoolean("oplus_media", true)
            putBoolean("oplus.ongoing", isPlaying)
            putBoolean("oplus_capsule", isPlaying)
            putBoolean("com.oplus.capsule", isPlaying)
            putString("oplus.capsule.type", "music")
            putString("oplus_capsule_type", "music")
            putString("com.oplus.capsule.type", "music")
            putInt("oplus_capsule_status", if (isPlaying) 1 else 0)
            putInt("oplus_capsule_state", if (isPlaying) 1 else 0)
            putInt("com.oplus.capsule.status", if (isPlaying) 1 else 0)
            putString("capsule_title", title)
            putString("capsule_content", artist)
            putString("capsule_subcontent", album)
            putBoolean("oplus.pantanal.support", true)
            putBoolean("com.oplus.pantanal.support", true)
            putBoolean("com.oplus.pantanal.capsule.support", true)
            putBoolean("oplus.capsule.support", true)
            putBoolean("com.oplus.livealert.support", true)
            putBoolean("oplus.livealert.support", true)
            putBoolean("com.oplus.fluidcloud.support", true)
            putBoolean("oplus.fluidcloud.support", true)
            putString("oplus.pantanal.type", "music")
            putInt("com.oplus.pantanal.capsule.status", if (isPlaying) 1 else 0)

            val sessionActivity = session.sessionActivity ?: PendingIntent.getActivity(
                this@PlaybackService,
                0,
                Intent(this@PlaybackService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            putParcelable("capsule_action", sessionActivity)
            putParcelable("oplus_capsule_action", sessionActivity)
            putParcelable("com.oplus.capsule.action", sessionActivity)
            val capsuleIcon = android.graphics.drawable.Icon.createWithResource(this@PlaybackService, R.drawable.ic_stat_vinyl)
            putParcelable("capsule_icon", capsuleIcon)
            putParcelable("oplus_capsule_icon", capsuleIcon)
            putParcelable("com.oplus.capsule.icon", capsuleIcon)
        }

        session.setSessionExtras(sessionExtras)

        // Directly set platform android.media.session.MediaSession metadata, playback state, and extras for OxygenOS 15 OplusMediaController
        runCatching {
            findFrameworkMediaSession(session)?.let { fwkSession ->
                val fwkMetadata = android.media.MediaMetadata.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ALBUM, album)
                    .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, duration)
                    .putString("lyricInfo", lyricJson)
                    .putString("lyric_info", lyricJson)
                    .putString("oplus_lyric_info", lyricJson)
                    .putString("songName", title)
                    .putString("artist", artist)
                    .putString("album", album)
                    .build()
                fwkSession.setMetadata(fwkMetadata)

                val stateInt = if (isPlaying) {
                    android.media.session.PlaybackState.STATE_PLAYING
                } else if (player.playbackState == Player.STATE_BUFFERING) {
                    android.media.session.PlaybackState.STATE_BUFFERING
                } else if (player.playbackState == Player.STATE_ENDED) {
                    android.media.session.PlaybackState.STATE_STOPPED
                } else {
                    android.media.session.PlaybackState.STATE_PAUSED
                }
                val actions = android.media.session.PlaybackState.ACTION_PLAY or
                    android.media.session.PlaybackState.ACTION_PAUSE or
                    android.media.session.PlaybackState.ACTION_PLAY_PAUSE or
                    android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT or
                    android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    android.media.session.PlaybackState.ACTION_SEEK_TO or
                    android.media.session.PlaybackState.ACTION_STOP

                val fwkPlaybackState = android.media.session.PlaybackState.Builder()
                    .setState(stateInt, player.currentPosition.coerceAtLeast(0L), player.playbackParameters.speed)
                    .setActions(actions)
                    .setExtras(sessionExtras)
                    .build()

                fwkSession.setPlaybackState(fwkPlaybackState)
                fwkSession.setExtras(sessionExtras)
            }
        }
    }
}
