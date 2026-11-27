package com.lowspin.app.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.compose.material.icons.rounded.BookmarkAdded
import com.lowspin.app.data.db.BookmarkEntity
import com.lowspin.app.ui.components.BookmarksSheet
import com.lowspin.app.ui.components.PitchShiftSheet
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.data.lyrics.Lyrics
import com.lowspin.app.media.EqualizerManager
import com.lowspin.app.media.EqualizerSettings
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.AudioVisualizer
import com.lowspin.app.ui.components.BassBoostDial
import com.lowspin.app.ui.components.FluidMeshBackground
import com.lowspin.app.ui.components.LikeParticleButton
import com.lowspin.app.ui.components.TurntableVinyl
import com.lowspin.app.ui.components.WaveformSeekBar
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.settings.SwipeAction
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
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
    lyricsLoading: Boolean = false,
    onToggleLyrics: () -> Unit,
    onImportLrc: (Uri) -> Unit = {},
    sleepLabel: String,
    onSleepClick: () -> Unit,
    onSpeedClick: () -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSongInfoClick: () -> Unit = {},
    loopRange: Pair<Long, Long>? = null,
    onABRepeatClick: () -> Unit = {},
    onCarModeClick: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onPitchSemitonesChanged: (Int) -> Unit = {},
    bookmarks: List<BookmarkEntity> = emptyList(),
    onAddBookmark: (Long) -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    equalizerSettings: EqualizerSettings? = null,
    equalizerManager: EqualizerManager? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val song = state.currentSong ?: return
    val appearance = LocalAppearance.current
    val context = LocalContext.current
    val visualizerEnabled by appearance.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by appearance.visualizerStyle.collectAsStateWithLifecycle()
    val spectrumWallpaper by appearance.spectrumWallpaper.collectAsStateWithLifecycle()
    val pureBlack by appearance.pureBlack.collectAsStateWithLifecycle()
    val turntableMode by appearance.turntableMode.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()
    val ambientGlow by appearance.ambientGlow.collectAsStateWithLifecycle()
    val keepScreenOn by appearance.keepScreenOn.collectAsStateWithLifecycle()
    val waveformSeekbar by appearance.waveformSeekbar.collectAsStateWithLifecycle()
    val fluidMeshLyrics by appearance.fluidMeshLyrics.collectAsStateWithLifecycle()
    val swipeLeft by appearance.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRight by appearance.swipeRightAction.collectAsStateWithLifecycle()
    val swipeUp by appearance.swipeUpAction.collectAsStateWithLifecycle()
    val swipeDown by appearance.swipeDownAction.collectAsStateWithLifecycle()

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

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var showBassDial by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showArtFullscreen by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var doubleTapFlashSide by remember { mutableIntStateOf(0) } // -1 = prev, 1 = next
    var pinchSpeedHud by remember { mutableStateOf<Float?>(null) }

    val lrcPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportLrc(uri)
    }

    LaunchedEffect(doubleTapFlashSide) {
        if (doubleTapFlashSide != 0) {
            delay(420L)
            doubleTapFlashSide = 0
        }
    }

    LaunchedEffect(pinchSpeedHud) {
        if (pinchSpeedHud != null) {
            delay(1000L)
            pinchSpeedHud = null
        }
    }

    fun executeSwipeAction(action: SwipeAction) {
        when (action) {
            SwipeAction.NONE -> Unit
            SwipeAction.DISMISS -> onDismiss()
            SwipeAction.NEXT -> onNext()
            SwipeAction.PREVIOUS -> onPrevious()
            SwipeAction.SEEK_FORWARD_10 -> onSeek((state.positionMs + 10_000L).coerceAtMost(state.durationMs.coerceAtLeast(0L)))
            SwipeAction.SEEK_BACK_10 -> onSeek((state.positionMs - 10_000L).coerceAtLeast(0L))
            SwipeAction.TOGGLE_LIKE -> onToggleLike()
            SwipeAction.CYCLE_SPEED -> {
                val nextSpeed = when {
                    state.playbackSpeed < 1.15f -> 1.25f
                    state.playbackSpeed < 1.40f -> 1.5f
                    else -> 1.0f
                }
                onSpeedChanged(nextSpeed)
                pinchSpeedHud = nextSpeed
            }
            SwipeAction.TOGGLE_LYRICS -> onToggleLyrics()
            SwipeAction.OPEN_QUEUE -> onQueueClick()
        }
    }

    val verticalVisualOffset = if (swipeDown == SwipeAction.DISMISS && dragY > 0f && abs(dragY) >= abs(dragX)) {
        dragY.roundToInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, middle, baseBg)))
            .pointerInput(swipeLeft, swipeRight, swipeUp, swipeDown, state.positionMs, state.playbackSpeed, isLiked, showLyrics) {
                if (showLyrics) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        val threshold = 135f
                        if (abs(dragX) > abs(dragY)) {
                            if (dragX < -threshold) executeSwipeAction(swipeLeft)
                            else if (dragX > threshold) executeSwipeAction(swipeRight)
                        } else {
                            if (dragY < -threshold) executeSwipeAction(swipeUp)
                            else if (dragY > threshold) executeSwipeAction(swipeDown)
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { _, amount ->
                        dragX += amount.x
                        dragY += amount.y
                    }
                )
            }
            .offset { IntOffset(0, verticalVisualOffset) }
    ) {
        // Spectrum Wallpaper (only on normal player backdrop when enabled, or FluidMesh takes full screen in lyrics mode)
        if (spectrumWallpaper && !showLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !reduceMotion) {
                            Modifier.blur(26.dp)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                AudioVisualizer(
                    isPlaying = state.isPlaying,
                    barCount = 34,
                    style = visualizerStyle,
                    accentColor = dominant,
                    reduceMotion = reduceMotion,
                    height = 520.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBg.copy(alpha = 0.62f))
                )
            }
        }

        if (showLyrics) {
            // FULL-SCREEN SMOOTH & BOUNCY CAPTIONS EXPERIENCE
            FullScreenLyricsView(
                song = song,
                state = state,
                dominant = dominant,
                fluidMeshLyrics = fluidMeshLyrics,
                reduceMotion = reduceMotion,
                lyrics = lyrics,
                lyricsLoading = lyricsLoading,
                onCloseLyrics = onToggleLyrics,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onImportLrc = { lrcPicker.launch(arrayOf("*/*")) }
            )
        } else {
            // CLEAN, MINIMAL NORMAL NOW PLAYING SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                // Clean 4-item Top Bar
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

                    IconButton(onClick = onToggleLyrics) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Show full-screen captions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More tools & options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (turntableMode) "Album Art Mode" else "Turntable Vinyl Mode",
                                        color = LowspinTextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Album,
                                        contentDescription = null,
                                        tint = if (turntableMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    appearance.setTurntableMode(!turntableMode)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Start Smart Radio", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMoreMenu = false
                                    onStartRadio()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (loopRange != null) "A-B Loop (Active)" else "A-B Loop Section",
                                        color = LowspinTextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.AllInclusive,
                                        contentDescription = null,
                                        tint = if (loopRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onABRepeatClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer ($sleepLabel)", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    onSleepClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Car Mode", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    onCarModeClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Floating Mini Player", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    runCatching {
                                        val intent = Intent(context, com.lowspin.app.BubbleActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            putExtra(com.lowspin.app.BubbleActivity.EXTRA_START_PIP, true)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pitch & Key Shift", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    showPitchSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Bookmark @ ${formatDuration(state.positionMs)}", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    onAddBookmark(state.positionMs)
                                    android.widget.Toast.makeText(context, "Bookmark saved", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Bookmarks (${bookmarks.size})", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    showBookmarksSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Track Info & Specs", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    onSongInfoClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("All Song Options", color = LowspinTextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    showMoreMenu = false
                                    onMoreClick()
                                }
                            )
                        }
                    }
                }

                // Spacious Center Artwork / Turntable + Double-tap Skip + Pinch-to-Speed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(song.id) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2f) {
                                        doubleTapFlashSide = -1
                                        onPrevious()
                                    } else {
                                        doubleTapFlashSide = 1
                                        onNext()
                                    }
                                }
                            )
                        }
                        .pointerInput(state.playbackSpeed) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var workingSpeed = state.playbackSpeed
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2) {
                                        val zoom = event.calculateZoom()
                                        if (abs(zoom - 1f) > 0.012f) {
                                            workingSpeed = (workingSpeed * zoom).coerceIn(0.5f, 2.5f)
                                            val snapped = (workingSpeed * 20f).roundToInt() / 20f
                                            onSpeedChanged(snapped)
                                            pinchSpeedHud = snapped
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
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

                    if (turntableMode) {
                        TurntableVinyl(
                            songId = song.id,
                            albumSeed = song.album.ifBlank { song.title },
                            isPlaying = state.isPlaying,
                            dominantColor = dominant,
                            reduceMotion = reduceMotion,
                            playbackSpeed = state.playbackSpeed,
                            modifier = Modifier.fillMaxSize(0.95f)
                        )
                    } else {
                        BoxWithConstraints {
                            val side = minOf(maxWidth, maxHeight) * 0.94f
                            val sharedMod = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        state = rememberSharedContentState(key = "album-art-${song.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier

                            Box(
                                modifier = Modifier
                                    .size(side)
                                    .then(sharedMod)
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
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { showArtFullscreen = true }
                            ) {
                                androidx.compose.animation.Crossfade(
                                    targetState = song.id,
                                    animationSpec = tween(if (reduceMotion) 0 else 280),
                                    label = "albumArtCrossfade"
                                ) { currentSongId ->
                                    AlbumArtImage(
                                        songId = currentSongId,
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

                    // Double-tap skip visual flash indicator
                    androidx.compose.animation.AnimatedVisibility(
                        visible = doubleTapFlashSide != 0,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(
                            if (doubleTapFlashSide < 0) Alignment.CenterStart else Alignment.CenterEnd
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (doubleTapFlashSide < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (doubleTapFlashSide < 0) "Previous" else "Next",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Pinch-to-speed HUD
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pinchSpeedHud != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        pinchSpeedHud?.let { spd ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Speed: ${String.format(java.util.Locale.US, "%.2fx", spd)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Clean Title, Artist, Subtle Metadata Subtitle + Particle Like Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = song,
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                (slideInVertically(tween(220)) { height -> height / 3 } + fadeIn(tween(220)))
                                    .togetherWith(slideOutVertically(tween(180)) { height -> -height / 3 } + fadeOut(tween(180)))
                            }
                        },
                        label = "songTitleMorph",
                        modifier = Modifier.weight(1f)
                    ) { currentTrack ->
                        Column {
                            Text(
                                text = currentTrack.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = LowspinTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = currentTrack.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val codec = remember(currentTrack.mimeType, currentTrack.folderPath) {
                                val ext = currentTrack.folderPath.substringAfterLast('.', "").uppercase()
                                when {
                                    ext in listOf("FLAC", "WAV", "OGG", "OPUS", "AAC", "MP3", "M4A", "ALAC", "AIFF") -> ext
                                    currentTrack.mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                                    currentTrack.mimeType.contains("wav", ignoreCase = true) -> "WAV"
                                    currentTrack.mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                                    currentTrack.mimeType.contains("opus", ignoreCase = true) -> "OPUS"
                                    currentTrack.mimeType.contains("aac", ignoreCase = true) -> "AAC"
                                    currentTrack.mimeType.contains("alac", ignoreCase = true) -> "ALAC"
                                    currentTrack.mimeType.contains("mp4", ignoreCase = true) || currentTrack.mimeType.contains("m4a", ignoreCase = true) -> "M4A"
                                    currentTrack.mimeType.contains("mpeg", ignoreCase = true) || currentTrack.mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                                    else -> currentTrack.mimeType.substringAfterLast('/').uppercase().take(4)
                                }
                            }
                            val metaSubtitle = buildString {
                                append(codec)
                                if (currentTrack.bpm > 0) append(" · ${currentTrack.bpm} BPM")
                                if (currentTrack.musicalKey.isNotBlank() && currentTrack.musicalKey != "Unknown") append(" · ${currentTrack.musicalKey}")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = metaSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onSongInfoClick)
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    LikeParticleButton(
                        isLiked = isLiked,
                        onToggle = onToggleLike,
                        reduceMotion = reduceMotion
                    )
                }

                // Live Audio Visualizer only while actively playing (avoids static dashed stub when paused)
                if (visualizerEnabled && state.isPlaying && !spectrumWallpaper) {
                    Spacer(Modifier.height(8.dp))
                    AudioVisualizer(
                        isPlaying = true,
                        style = visualizerStyle,
                        accentColor = MaterialTheme.colorScheme.primary,
                        reduceMotion = reduceMotion,
                        height = 28.dp
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                // Seek Bar
                if (waveformSeekbar) {
                    WaveformSeekBar(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        songId = song.id,
                        loopRange = loopRange,
                        onSeek = onSeek
                    )
                } else {
                    SeekBar(state = state, onSeek = onSeek)
                }

                Spacer(Modifier.height(6.dp))

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

                // Balanced 4-item Bottom Utility Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                            .clickable(onClick = onSpeedClick)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Playback Speed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${state.playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (equalizerSettings != null) {
                        val bassVal by equalizerSettings.bassBoost.collectAsStateWithLifecycle()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (showBassDial || bassVal > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                )
                                .clickable { showBassDial = !showBassDial }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Bass Dial",
                                tint = if (showBassDial || bassVal > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Bass ${bassVal / 10}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showBassDial || bassVal > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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

                Spacer(Modifier.height(14.dp))
            }
        }

        // Dedicated Circular Bass Boost Dial Overlay
        if (showBassDial && equalizerSettings != null) {
            val bass by equalizerSettings.bassBoost.collectAsStateWithLifecycle()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showBassDial = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick Bass Control",
                        style = MaterialTheme.typography.titleMedium,
                        color = LowspinTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Drag around the dial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    BassBoostDial(
                        strength = bass,
                        onStrengthChange = { newVal ->
                            if (newVal > 0 && !equalizerSettings.enabled.value) {
                                equalizerSettings.setEnabled(true)
                            }
                            equalizerSettings.setBassBoost(newVal)
                            equalizerManager?.applyBassBoost(equalizerSettings.enabled.value || newVal > 0, newVal)
                        },
                        size = 180.dp
                    )
                }
            }
        }

        // Fullscreen Album Art Viewer
        androidx.compose.animation.AnimatedVisibility(
            visible = showArtFullscreen,
            enter = fadeIn(tween(if (reduceMotion) 0 else 240)),
            exit = fadeOut(tween(if (reduceMotion) 0 else 200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f))
                    .clickable { showArtFullscreen = false }
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .shadow(32.dp, RoundedCornerShape(20.dp), spotColor = dominant, ambientColor = dominant)
                    ) {
                        AlbumArtImage(
                            songId = song.id,
                            seed = song.album.ifBlank { song.title },
                            modifier = Modifier.fillMaxSize(),
                            sizePx = 1024,
                            shape = RoundedCornerShape(20.dp),
                            showRotatingVinyl = false
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LowspinTextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Tap anywhere to dismiss",
                        style = MaterialTheme.typography.labelMedium,
                        color = LowspinTextMuted
                    )
                }
            }
        }

        val pitchSemitones by appearance.pitchSemitones.collectAsStateWithLifecycle()
        if (showPitchSheet) {
            PitchShiftSheet(
                currentSemitones = pitchSemitones,
                onSemitonesChange = { semitones ->
                    onPitchSemitonesChanged(semitones)
                },
                onDismiss = { showPitchSheet = false }
            )
        }

        if (showBookmarksSheet) {
            BookmarksSheet(
                bookmarks = bookmarks,
                currentPositionMs = state.positionMs,
                onAddBookmark = onAddBookmark,
                onSeekTo = onSeek,
                onDeleteBookmark = onDeleteBookmark,
                onDismiss = { showBookmarksSheet = false }
            )
        }
    }
}

@Composable
private fun FullScreenLyricsView(
    song: com.lowspin.app.data.db.SongEntity,
    state: PlayerUiState,
    dominant: Color,
    fluidMeshLyrics: Boolean,
    reduceMotion: Boolean,
    lyrics: Lyrics?,
    lyricsLoading: Boolean,
    onCloseLyrics: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onImportLrc: () -> Unit
) {
    val appearance = LocalAppearance.current
    val context = LocalContext.current
    val fontSize by appearance.lyricsFontSize.collectAsStateWithLifecycle()
    val syncOffset by appearance.lyricsSyncOffset.collectAsStateWithLifecycle()
    var showTools by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (fluidMeshLyrics) {
            FluidMeshBackground(
                dominantColor = dominant,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Minimal Compact Full-Screen Captions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    songId = song.id,
                    seed = song.album.ifBlank { song.title },
                    modifier = Modifier.size(42.dp),
                    sizePx = 128,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showTools = !showTools }) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Caption settings",
                        tint = if (showTools) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f)
                    )
                }
                IconButton(onClick = onCloseLyrics) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close captions",
                        tint = Color.White
                    )
                }
            }

            // Optional collapsible single-line scrollable caption controls (never wraps vertically)
            AnimatedVisibility(visible = showTools && lyrics != null) {
                if (lyrics != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lyrics.synced) {
                            CaptionToolChip("-0.5s") { appearance.setLyricsSyncOffset(syncOffset - 500) }
                            val offsetText = when {
                                syncOffset > 0 -> "+${syncOffset / 1000.0}s"
                                syncOffset < 0 -> "${syncOffset / 1000.0}s"
                                else -> "Sync 0s"
                            }
                            Text(
                                text = offsetText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                softWrap = false,
                                maxLines = 1
                            )
                            CaptionToolChip("+0.5s") { appearance.setLyricsSyncOffset(syncOffset + 500) }
                        }
                        CaptionToolChip("A-") { appearance.setLyricsFontSize(fontSize - 2f) }
                        CaptionToolChip("A+") { appearance.setLyricsFontSize(fontSize + 2f) }
                        CaptionToolChip("Import .LRC") { onImportLrc() }
                        CaptionToolChip("Copy") {
                            val fullText = lyrics.lines.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Lyrics", fullText))
                            Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Full-Screen Smooth, Large-Typography, Bouncy Captions Canvas
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    lyricsLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Loading captions…",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    lyrics == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Lyrics,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "No captions found",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Checked embedded MP3, FLAC, M4A & OGG tags and local .lrc files.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = onImportLrc,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Import .lrc file", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        val adjustedPos = state.positionMs + syncOffset
                        val activeIndex = if (lyrics.synced) {
                            lyrics.lines.indexOfLast { it.timeMs <= adjustedPos }
                        } else -1

                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0) {
                                listState.animateScrollToItem(activeIndex, scrollOffset = -180)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            itemsIndexed(
                                items = lyrics.lines,
                                key = { idx, line -> "${idx}_${line.timeMs}" }
                            ) { index, line ->
                                BouncyCaptionLine(
                                    text = line.text.ifBlank { "♪" },
                                    isActive = lyrics.synced && index == activeIndex,
                                    isSynced = lyrics.synced,
                                    baseFontSize = fontSize,
                                    reduceMotion = reduceMotion,
                                    onClick = {
                                        if (lyrics.synced && line.timeMs >= 0) {
                                            onSeek(line.timeMs)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Sleek Minimal Bottom Progress & Transport Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                val dur = state.durationMs.coerceAtLeast(1L)
                val ratio = (state.positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BouncyCaptionLine(
    text: String,
    isActive: Boolean,
    isSynced: Boolean,
    baseFontSize: Float,
    reduceMotion: Boolean,
    onClick: () -> Unit
) {
    // Spring overshoot bounce whenever a line enters active state
    val scale = remember { Animatable(if (isActive) 1.04f else 0.96f) }
    val bounceOffsetY = remember { Animatable(0f) }

    LaunchedEffect(isActive, reduceMotion) {
        if (reduceMotion) {
            scale.snapTo(if (isActive) 1.02f else 1.0f)
            bounceOffsetY.snapTo(0f)
            return@LaunchedEffect
        }
        if (isActive) {
            // Energetic spring pop on the active line
            bounceOffsetY.snapTo(8f)
            scale.snapTo(0.95f)
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(
                    dampingRatio = 0.44f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            scale.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(durationMillis = 260)
            )
        }
    }

    LaunchedEffect(isActive, reduceMotion) {
        if (isActive && !reduceMotion) {
            bounceOffsetY.animateTo(
                targetValue = -3f,
                animationSpec = spring(
                    dampingRatio = 0.42f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else if (!reduceMotion) {
            bounceOffsetY.animateTo(0f, tween(240))
        }
    }

    val textAlpha by animateFloatAsState(
        targetValue = when {
            !isSynced -> 0.92f
            isActive -> 1.0f
            else -> 0.36f
        },
        animationSpec = tween(durationMillis = 280),
        label = "captionAlpha"
    )

    val resolvedSize = if (isActive || !isSynced) {
        (baseFontSize + 5f).sp
    } else {
        baseFontSize.sp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isSynced, onClick = onClick)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationY = bounceOffsetY.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = resolvedSize,
                lineHeight = (resolvedSize.value + 10f).sp,
                fontWeight = if (isActive || !isSynced) FontWeight.ExtraBold else FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White,
            modifier = Modifier.alpha(textAlpha)
        )
    }
}

@Composable
private fun CaptionToolChip(
    label: String,
    onClick: () -> Unit
) {
    Text(
        text = label,
        softWrap = false,
        maxLines = 1,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
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
