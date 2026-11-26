package com.lowspin.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.LocalAlbumArtRepository
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.VinylGlyph
import com.lowspin.app.ui.settings.AppearanceSettings
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.ui.theme.LowspinTheme
import com.lowspin.app.util.formatDuration

/**
 * Dedicated, lightweight floating activity for Android 11+ Notification Bubbles and
 * Picture-in-Picture (PiP) floating mini players.
 *
 * Runs in its own separate task (documentLaunchMode="always") so that:
 * 1. MainActivity's task, view tree, and navigation backstack are never hijacked or reparented.
 * 2. Multi-window, notification bubble, and Picture-in-Picture floating criteria are satisfied
 *    on Android 8 through 15 (and OnePlus OxygenOS 15) without introducing blank screen regressions.
 * 3. Provides clean, instant, on-device playback transport controls and artwork inside the bubble.
 */
class BubbleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_PIP = "EXTRA_START_PIP"
        const val ACTION_PIP_PREV = "com.lowspin.app.ACTION_PIP_PREV"
        const val ACTION_PIP_PLAY_PAUSE = "com.lowspin.app.ACTION_PIP_PLAY_PAUSE"
        const val ACTION_PIP_NEXT = "com.lowspin.app.ACTION_PIP_NEXT"
    }

    private val isPipMode = mutableStateOf(false)
    private var isReceiverRegistered = false

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val container = (application as? LowspinApplication)?.container ?: return
            when (intent?.action) {
                ACTION_PIP_PREV -> container.playbackController.previous()
                ACTION_PIP_PLAY_PAUSE -> container.playbackController.togglePlayPause()
                ACTION_PIP_NEXT -> container.playbackController.next()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LowspinApplication).container
        val appearance = AppearanceSettings(applicationContext)

        registerPipReceiver()
        updatePipParams(container.playbackController.state.value.isPlaying)

        setContent {
            val textScale by appearance.textScale.collectAsStateWithLifecycle()
            val density = LocalDensity.current
            val isInPip by isPipMode
            val playerState: PlayerUiState by container.playbackController.state.collectAsStateWithLifecycle()

            LaunchedEffect(playerState.isPlaying) {
                updatePipParams(playerState.isPlaying)
            }

            CompositionLocalProvider(
                LocalAppearance provides appearance,
                LocalAlbumArtRepository provides container.albumArtRepository
            ) {
                LowspinTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = density.density,
                            fontScale = density.fontScale * textScale
                        )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (isInPip) {
                                BubblePipCompactPlayer(
                                    song = playerState.currentSong,
                                    isPlaying = playerState.isPlaying
                                )
                            } else {
                                BubblePlayerScreen(
                                    container = container,
                                    onFloatPip = { enterBubblePip() },
                                    onOpenMainApp = {
                                        val intent = Intent(this@BubbleActivity, MainActivity::class.java).apply {
                                            action = Intent.ACTION_MAIN
                                            addCategory(Intent.CATEGORY_LAUNCHER)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                        }
                                        startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (intent?.getBooleanExtra(EXTRA_START_PIP, false) == true) {
            window.decorView.post {
                enterBubblePip()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_START_PIP, false)) {
            window.decorView.post {
                enterBubblePip()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterBubblePip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode.value = isInPictureInPictureMode
        val container = (application as? LowspinApplication)?.container
        if (container != null) {
            updatePipParams(container.playbackController.state.value.isPlaying)
        }
    }

    fun enterBubblePip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val container = (application as? LowspinApplication)?.container
                val isPlaying = container?.playbackController?.state?.value?.isPlaying ?: false
                val params = buildPipParams(isPlaying)
                enterPictureInPictureMode(params)
            }
        }
    }

    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        val actions = ArrayList<RemoteAction>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Previous action
            val prevIntent = PendingIntent.getBroadcast(
                this, 101,
                Intent(ACTION_PIP_PREV).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_stat_vinyl),
                    "Previous", "Previous Track", prevIntent
                )
            )

            // Play/Pause action
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 102,
                Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_stat_vinyl),
                    if (isPlaying) "Pause" else "Play",
                    if (isPlaying) "Pause" else "Play",
                    playPauseIntent
                )
            )

            // Next action
            val nextIntent = PendingIntent.getBroadcast(
                this, 103,
                Intent(ACTION_PIP_NEXT).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_stat_vinyl),
                    "Next", "Next Track", nextIntent
                )
            )

            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(1, 1))
                .setActions(actions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
                builder.setSeamlessResizeEnabled(true)
            }

            return builder.build()
        }

        throw UnsupportedOperationException("Picture-in-picture requires API 26+")
    }

    fun updatePipParams(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                setPictureInPictureParams(buildPipParams(isPlaying))
            }
        }
    }

    private fun registerPipReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PIP_PREV)
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_NEXT)
            }
            ContextCompat.registerReceiver(
                this,
                pipReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    private fun unregisterPipReceiver() {
        if (isReceiverRegistered) {
            runCatching { unregisterReceiver(pipReceiver) }
            isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        unregisterPipReceiver()
        super.onDestroy()
    }
}

/**
 * Ultra-compact floating mini-player shown when in Picture-in-Picture mode.
 */
@Composable
private fun BubblePipCompactPlayer(
    song: SongEntity?,
    isPlaying: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (song != null) {
            AlbumArtImage(
                songId = song.id,
                seed = song.album.ifBlank { song.title },
                modifier = Modifier.fillMaxSize(),
                sizePx = 384,
                shape = RoundedCornerShape(0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VinylGlyph(modifier = Modifier.size(16.dp), rotating = isPlaying)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFF00E676) else Color(0xFFFF5252))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                VinylGlyph(modifier = Modifier.size(24.dp), rotating = false)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "HighSpin",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BubblePlayerScreen(
    container: AppContainer,
    onFloatPip: () -> Unit,
    onOpenMainApp: () -> Unit
) {
    val playerState: PlayerUiState by container.playbackController.state.collectAsStateWithLifecycle()
    val songs: List<SongEntity> by container.musicRepository.songs.collectAsStateWithLifecycle(emptyList())
    val song = playerState.currentSong
    val isLiked = remember(songs, song?.id) {
        songs.firstOrNull { it.id == song?.id }?.isLiked == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VinylGlyph(
                    modifier = Modifier.size(20.dp),
                    rotating = playerState.isPlaying
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "HighSpin Bubble",
                    style = MaterialTheme.typography.titleSmall,
                    color = LowspinTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onFloatPip,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Float Mini Player"
                    )
                }
                IconButton(
                    onClick = onOpenMainApp,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open HighSpin"
                    )
                }
            }
        }

        if (song != null) {
            // Artwork & Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AlbumArtImage(
                    songId = song.id,
                    seed = song.album.ifBlank { song.title },
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    sizePx = 256,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LowspinTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .basicMarquee()
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Progress & Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var isSeeking by remember { mutableStateOf(false) }
                var seekPos by remember { mutableFloatStateOf(0f) }

                val duration = playerState.durationMs.coerceAtLeast(1L)
                val currentPos = if (isSeeking) seekPos.toLong() else playerState.positionMs

                Slider(
                    value = currentPos.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = {
                        isSeeking = true
                        seekPos = it
                    },
                    onValueChangeFinished = {
                        container.playbackController.seekTo(seekPos.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPos),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { container.playbackController.setShuffle(!playerState.shuffleEnabled) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { container.playbackController.previous() }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = LowspinTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = { container.playbackController.togglePlayPause() },
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = LowspinOnAccent
                        )
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { container.playbackController.next() }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = LowspinTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            song.let {
                                container.playbackController.toggleLike(it.id, !isLiked)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No track playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Open HighSpin to select a song or playlist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenMainApp,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = LowspinOnAccent
                    )
                ) {
                    Text("Open HighSpin")
                }
            }
        }
    }
}
