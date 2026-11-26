package com.lowspin.app.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.data.lyrics.Lyrics
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.AudioVisualizer
import com.lowspin.app.ui.components.TurntableVinyl
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onMoreClick: () -> Unit,
    showLyrics: Boolean,
    lyrics: Lyrics?,
    onToggleLyrics: () -> Unit,
    sleepLabel: String,
    onSleepClick: () -> Unit,
    onSpeedClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSongInfoClick: () -> Unit = {}
) {
    val song = state.currentSong ?: return
    val appearance = LocalAppearance.current
    val context = LocalContext.current
    val visualizerEnabled by appearance.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by appearance.visualizerStyle.collectAsStateWithLifecycle()
    val pureBlack by appearance.pureBlack.collectAsStateWithLifecycle()
    val turntableMode by appearance.turntableMode.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()
    val ambientGlow by appearance.ambientGlow.collectAsStateWithLifecycle()
    val keepScreenOn by appearance.keepScreenOn.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val dominant = remember(state.dominantColor, song.id) {
        state.dominantColor
            .takeIf { it != 0 }
            ?.let { Color(it) }
            ?: Color(GradientPlaceholder.primaryColor(song.album.ifBlank { song.title }))
    }

    val baseBg = if (pureBlack) Color(0xFF000000) else MaterialTheme.colorScheme.background
    val top = remember(dominant, pureBlack) {
        if (pureBlack) lerp(dominant, Color(0xFF000000), 0.65f)
        else lerp(dominant, baseBg, 0.20f)
    }
    val middle = remember(dominant, pureBlack) {
        if (pureBlack) lerp(dominant, Color(0xFF000000), 0.88f)
        else lerp(dominant, baseBg, 0.70f)
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, middle, baseBg)))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 160f) onDismiss()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    }
                )
            }
            .offset { IntOffset(0, dragOffset.roundToInt()) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = LowspinTextPrimary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = state.sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = LowspinTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Turntable Vinyl toggle button
                IconButton(onClick = { appearance.setTurntableMode(!turntableMode) }) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = if (turntableMode) "Switch to Album Art" else "Switch to Turntable Vinyl",
                        tint = if (turntableMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isLiked) "Remove from Liked Songs" else "Like song",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleLyrics) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = if (showLyrics) "Hide lyrics" else "Show lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    runCatching {
                        val intent = Intent(context, com.lowspin.app.BubbleActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        }
                        context.startActivity(intent)
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Floating Mini Player Bubble",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Center Artwork / Vinyl / Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Ambient dynamic back-glow reflecting album palette
                if (ambientGlow) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val glowRadius = minOf(maxWidth, maxHeight) * 0.64f
                        Box(
                            modifier = Modifier
                                .size(glowRadius * 2)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            dominant.copy(alpha = if (state.isPlaying) 0.30f else 0.12f),
                                            dominant.copy(alpha = if (state.isPlaying) 0.12f else 0.04f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                if (showLyrics && lyrics != null) {
                    LyricsCard(
                        lyrics = lyrics,
                        positionMs = state.positionMs,
                        isPlaying = state.isPlaying,
                        onSeek = onSeek
                    )
                } else if (turntableMode) {
                    TurntableVinyl(
                        songId = song.id,
                        albumSeed = song.album.ifBlank { song.title },
                        isPlaying = state.isPlaying,
                        dominantColor = dominant,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.fillMaxSize(0.95f)
                    )
                } else {
                    BoxWithConstraints {
                        val side = minOf(maxWidth, maxHeight) * 0.94f
                        Box(
                            modifier = Modifier
                                .size(side)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    spotColor = dominant,
                                    ambientColor = dominant
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                        ) {
                            AlbumArtImage(
                                songId = song.id,
                                seed = song.album.ifBlank { song.title },
                                modifier = Modifier.fillMaxSize(),
                                sizePx = 800,
                                shape = RoundedCornerShape(18.dp),
                                showRotatingVinyl = true,
                                isPlaying = state.isPlaying
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title and Artist with Format Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = LowspinTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Audio Info & DSP Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val codec = remember(song.mimeType, song.folderPath) {
                        val ext = song.folderPath.substringAfterLast('.', "").uppercase()
                        when {
                            ext in listOf("FLAC", "WAV", "OGG", "OPUS", "AAC", "MP3", "M4A", "ALAC", "AIFF") -> ext
                            song.mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                            song.mimeType.contains("wav", ignoreCase = true) -> "WAV"
                            song.mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                            song.mimeType.contains("opus", ignoreCase = true) -> "OPUS"
                            song.mimeType.contains("aac", ignoreCase = true) -> "AAC"
                            song.mimeType.contains("alac", ignoreCase = true) -> "ALAC"
                            song.mimeType.contains("mp4", ignoreCase = true) || song.mimeType.contains("m4a", ignoreCase = true) -> "M4A"
                            song.mimeType.contains("mpeg", ignoreCase = true) || song.mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                            else -> song.mimeType.substringAfterLast('/').uppercase().take(4)
                        }
                    }
                    val isLossless = codec in listOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isLossless) Color(0xFFD4AF37).copy(alpha = 0.20f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isLossless) Color(0xFFFFD700).copy(alpha = 0.55f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(onClick = onSongInfoClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLossless) {
                                Text(
                                    text = "HI-RES ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = codec,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLossless) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sleep Timer Quick Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onSleepClick)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sleepLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-Time Audio Visualizer (Smooth animated bars / waveform)
            if (visualizerEnabled) {
                Spacer(Modifier.height(8.dp))
                AudioVisualizer(
                    isPlaying = state.isPlaying,
                    style = visualizerStyle,
                    accentColor = MaterialTheme.colorScheme.primary,
                    reduceMotion = reduceMotion,
                    height = 36.dp
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Seek Bar
            SeekBar(state = state, onSeek = onSeek)

            // Playback Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = if (state.shuffleEnabled) "Disable shuffle" else "Enable shuffle",
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = LowspinTextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = LowspinOnAccent
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = LowspinTextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat mode",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Utility bar: Speed/Pitch Pill, Equalizer, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed/pitch pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onSpeedClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Playback Speed & Pitch",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.playbackPitch != 1f) {
                            "${state.playbackSpeed}x • ${state.playbackPitch}x pitch"
                        } else {
                            "${state.playbackSpeed}x"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEqualizerClick) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onQueueClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Playback Queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LyricsCard(
    lyrics: Lyrics,
    positionMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    val appearance = LocalAppearance.current
    val context = LocalContext.current
    val fontSize by appearance.lyricsFontSize.collectAsStateWithLifecycle()
    val syncOffset by appearance.lyricsSyncOffset.collectAsStateWithLifecycle()

    val adjustedPosition = positionMs + syncOffset
    val activeIndex = if (lyrics.synced) {
        lyrics.lines.indexOfLast { it.timeMs <= adjustedPosition }
    } else {
        -1
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && isPlaying) {
            listState.animateScrollToItem(activeIndex, scrollOffset = 0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.96f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Lyrics Header Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (lyrics.synced) "SYNCED LYRICS" else "LYRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (lyrics.synced && syncOffset != 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (syncOffset > 0) "+${syncOffset}ms" else "${syncOffset}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lyrics.synced) {
                        Text(
                            text = "-0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .clickable { appearance.setLyricsSyncOffset(syncOffset - 500) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "+0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .clickable { appearance.setLyricsSyncOffset(syncOffset + 500) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "A-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .clickable { appearance.setLyricsFontSize(fontSize - 1.5f) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "A+",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .clickable { appearance.setLyricsFontSize(fontSize + 1.5f) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            val fullText = lyrics.lines.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Lyrics", fullText))
                            Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Lyrics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (lyrics.synced) {
                    LazyColumn(state = listState) {
                        itemsIndexed(lyrics.lines) { index, line ->
                            val isActive = index == activeIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (line.timeMs >= 0) {
                                            onSeek(line.timeMs)
                                        }
                                    }
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = line.text.ifBlank { "♪" },
                                    style = if (isActive) {
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (fontSize + 3f).sp
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = fontSize.sp
                                        )
                                    },
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (line.timeMs > 0) {
                                    Text(
                                        text = formatDuration(line.timeMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        lyrics.lines.forEach { line ->
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(state: PlayerUiState, onSeek: (Long) -> Unit) {
    val duration = state.durationMs.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.positionMs, dragging) {
        if (!dragging) {
            dragValue = state.positionMs.coerceIn(0L, duration).toFloat()
        }
    }

    val currentRatio = (dragValue / duration.toFloat()).coerceIn(0f, 1f)
    val songSeed = state.currentSong?.id ?: 42L
    val barHeights = remember(songSeed) {
        val random = java.util.Random(songSeed)
        List(40) { index ->
            val base = kotlin.math.sin(index * 0.22).toFloat() * 0.35f + 0.65f
            (base * (0.35f + random.nextFloat() * 0.65f)).coerceIn(0.2f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEachIndexed { i, heightRatio ->
                val progressAtBar = i.toFloat() / barHeights.size
                val isPassed = progressAtBar <= currentRatio
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(heightRatio)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isPassed) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                        )
                )
            }
        }

        Slider(
            value = dragValue.coerceIn(0f, duration.toFloat()),
            onValueChange = { value ->
                dragging = true
                dragValue = value
            },
            onValueChangeFinished = {
                onSeek(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                inactiveTrackColor = Color.Transparent
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(if (dragging) dragValue.toLong() else state.positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDuration(state.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
