package com.lowspin.app.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.lyrics.Lyrics
import com.lowspin.app.di.AppContainer
import com.lowspin.app.ui.components.ABRepeatSheet
import com.lowspin.app.ui.components.CreatePlaylistDialog
import com.lowspin.app.ui.components.EditTagsSheet
import com.lowspin.app.ui.components.MiniPlayer
import com.lowspin.app.ui.components.QueueSheet
import com.lowspin.app.ui.components.SongInfoSheet
import com.lowspin.app.ui.components.SongOptionsSheet
import com.lowspin.app.ui.components.SpeedPickerSheet
import com.lowspin.app.ui.screens.about.AboutScreen
import com.lowspin.app.ui.screens.album.AlbumDetailScreen
import com.lowspin.app.ui.screens.album.AlbumViewModel
import com.lowspin.app.ui.screens.artist.ArtistDetailScreen
import com.lowspin.app.ui.screens.artist.ArtistViewModel
import com.lowspin.app.ui.screens.carmode.CarModeScreen
import com.lowspin.app.ui.screens.equalizer.EqualizerScreen
import com.lowspin.app.ui.screens.folderdetail.FolderDetailScreen
import com.lowspin.app.ui.screens.folderdetail.FolderDetailViewModel
import com.lowspin.app.ui.screens.folders.FolderManagerScreen
import com.lowspin.app.ui.screens.folders.FolderManagerViewModel
import com.lowspin.app.ui.screens.hidden.HiddenSongsScreen
import com.lowspin.app.ui.screens.hidden.HiddenSongsViewModel
import com.lowspin.app.ui.screens.library.LibraryScreen
import com.lowspin.app.ui.screens.library.LibraryViewModel
import com.lowspin.app.ui.screens.liked.LikedSongsScreen
import com.lowspin.app.ui.screens.liked.LikedViewModel
import com.lowspin.app.ui.screens.nowplaying.NowPlayingScreen
import com.lowspin.app.ui.screens.permission.PermissionScreen
import com.lowspin.app.ui.screens.playlist.PlaylistDetailScreen
import com.lowspin.app.ui.screens.playlist.PlaylistViewModel
import com.lowspin.app.ui.screens.search.SearchScreen
import com.lowspin.app.ui.screens.search.SearchViewModel
import com.lowspin.app.ui.screens.settings.SettingsScreen
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.settings.LocalListStyle
import com.lowspin.app.ui.settings.LowspinListStyle
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun LowspinApp(container: AppContainer) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val requiredPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var granted by remember { mutableStateOf(hasPermission(context, requiredPermission)) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        if (!result) {
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, requiredPermission)
            } ?: false
            permanentlyDenied = requestedOnce && !canAskAgain
        } else {
            permanentlyDenied = false
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, requiredPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = hasPermission(context, requiredPermission)
                granted = nowGranted
                if (nowGranted) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!granted) {
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onGrantClick = {
                requestedOnce = true
                permissionLauncher.launch(requiredPermission)
            },
            onOpenSettings = { openAppSettings(context) }
        )
        return
    }

    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(container)
    )
    val appearance = LocalAppearance.current
    val compact by appearance.compact.collectAsStateWithLifecycle()
    val showArt by appearance.showArt.collectAsStateWithLifecycle()
    val largeTargets by appearance.largeTargets.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()
    val listStyle = remember(compact, showArt, largeTargets) {
        LowspinListStyle(
            rowHeight = if (compact) 56.dp else 64.dp,
            showArt = showArt,
            largeTouchTargets = largeTargets
        )
    }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPlaylistId = backStackEntry?.arguments?.getLong("playlistId")?.takeIf { it > 0L }

    // Guard: only recover if the back-stack is genuinely empty *after* the
    // NavHost has had a chance to settle. The navController is not ready until
    // it has a graph attached, so check that first.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            // If the entry is null AND the graph is already set, the stack is
            // truly empty — navigate home to recover gracefully.
            @Suppress("SENSELESS_COMPARISON")
            if (entry == null && runCatching { navController.graph.nodes.size() > 0 }.getOrDefault(false)) {
                navController.navigate("library") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val songs by libraryViewModel.songs.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
    val folders by libraryViewModel.folders.collectAsStateWithLifecycle()
    val scanState by libraryViewModel.scanState.collectAsStateWithLifecycle()
    val playerState by libraryViewModel.playerState.collectAsStateWithLifecycle()

    val currentSong = playerState.currentSong
    val currentSongId = currentSong?.id
    val isLiked = remember(songs, currentSongId) {
        songs.firstOrNull { it.id == currentSongId }?.isLiked == true
    }

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var sleepNow by remember { mutableStateOf(System.currentTimeMillis()) }
    val sleepEndsAt by container.sleepTimer.endsAt.collectAsStateWithLifecycle()
    val sleepMode by container.sleepTimer.mode.collectAsStateWithLifecycle()
    val loopRange by container.playbackController.loopRange.collectAsStateWithLifecycle()
    var showABRepeatSheet by rememberSaveable { mutableStateOf(false) }
    var showCarMode by rememberSaveable { mutableStateOf(false) }
    var editTagsSong by remember { mutableStateOf<SongEntity?>(null) }
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    var infoSong by remember { mutableStateOf<SongEntity?>(null) }
    var songToHide by remember { mutableStateOf<SongEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        libraryViewModel.onLibraryAccessGranted()
    }

    LaunchedEffect(showLyrics, currentSongId) {
        lyrics = if (showLyrics) {
            currentSong?.let { container.lyricsRepository.load(it) }
        } else {
            null
        }
    }

    LaunchedEffect(sleepEndsAt) {
        while (sleepEndsAt > 0L) {
            delay(1_000L)
            sleepNow = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAsked) {
            notificationAsked = true
            val grantedNotification = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!grantedNotification) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val sleepLabel = when {
        sleepMode == "track" -> "End of track"
        sleepEndsAt > 0L -> formatDuration((sleepEndsAt - sleepNow).coerceAtLeast(0L))
        else -> "Sleep timer"
    }

    // Stable callback identities keep screen composables skippable while the
    // player ticker publishes a new state every 500 ms.
    val onPlaySong = remember(libraryViewModel, songs) {
        { song: SongEntity ->
            val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            libraryViewModel.playQueue(songs, index, "Your library")
        }
    }
    val onPlayResults = remember(libraryViewModel) {
        { song: SongEntity, list: List<SongEntity> ->
            val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            libraryViewModel.playQueue(list, index, "Search results")
        }
    }
    val onPlayQueue = remember(libraryViewModel) {
        { list: List<SongEntity>, index: Int, label: String ->
            val safeIndex = index.coerceIn(0, list.lastIndex.coerceAtLeast(0))
            libraryViewModel.playQueue(list, safeIndex, label)
        }
    }
    val onOpenOptions = remember { { song: SongEntity -> optionsSong = song } }
    val onBack = remember(navController, activity) {
        {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != "library" && navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else if (currentRoute == "library") {
                activity?.moveTaskToBack(true)
            }
            Unit
        }
    }
    val onOpenPlaylist = remember(navController) { { id: Long -> navController.navigate("playlist/$id") } }
    val onOpenAlbum = remember(navController) { { id: Long -> navController.navigate("album/$id") } }
    val onOpenArtist = remember(navController) { { name: String -> navController.navigate("artist/${Uri.encode(name)}") } }
    val onOpenFolderDetail = remember(navController) { { path: String -> navController.navigate("folderDetail?path=${Uri.encode(path)}") } }
    val onOpenEqualizer = remember(navController) { { navController.navigate("equalizer") } }
    val onOpenSearch = remember(navController) { { navController.navigate("search") } }
    val onOpenLiked = remember(navController) { { navController.navigate("liked") } }
    val onOpenFolders = remember(navController) { { navController.navigate("folders") } }
    val onOpenHidden = remember(navController) { { navController.navigate("hidden") } }
    val onOpenAbout = remember(navController) { { navController.navigate("about") } }
    val onOpenSettings = remember(navController) { { navController.navigate("settings") } }
    val onRequestCreatePlaylist = remember { { showCreatePlaylistDialog = true } }
    val onRefresh = remember(libraryViewModel) { { libraryViewModel.refreshLibrary() } }
    val onTogglePlayPause = remember(libraryViewModel) { { libraryViewModel.togglePlayPause() } }
    val onNext = remember(libraryViewModel) { { libraryViewModel.next() } }
    val onPrevious = remember(libraryViewModel) { { libraryViewModel.previous() } }
    val onSeek = remember(libraryViewModel) { { position: Long -> libraryViewModel.seekTo(position) } }
    val onToggleShuffle = remember(libraryViewModel, playerState.shuffleEnabled) {
        { libraryViewModel.setShuffle(!playerState.shuffleEnabled) }
    }
    val onCycleRepeat = remember(libraryViewModel) { { libraryViewModel.cycleRepeat() } }
    val onTogglePlayerLike = remember(libraryViewModel, currentSongId, isLiked) {
        { libraryViewModel.toggleLike(currentSongId ?: -1L, !isLiked) }
    }

    BackHandler(enabled = showCarMode) { showCarMode = false }
    BackHandler(enabled = !showCarMode && showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = !showCarMode && !showNowPlaying && songToHide != null) { songToHide = null }
    BackHandler(enabled = !showCarMode && !showNowPlaying && songToHide == null && optionsSong != null) {
        optionsSong = null
    }
    BackHandler(
        enabled = !showCarMode &&
            !showNowPlaying &&
            songToHide == null &&
            optionsSong == null &&
            backStackEntry?.destination?.route == "library"
    ) {
        activity?.moveTaskToBack(true)
    }

    CompositionLocalProvider(LocalListStyle provides listStyle) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (currentSong != null) {
                    MiniPlayer(
                        state = playerState,
                        isLiked = isLiked,
                        onExpand = { showNowPlaying = true },
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleLike = onTogglePlayerLike,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "library",
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable("library") {
                    LibraryScreen(
                        songs = songs,
                        playlists = playlists,
                        albums = albums,
                        artists = artists,
                        folders = folders,
                        likedCount = remember(songs) { songs.count { it.isLiked } },
                        scanState = scanState,
                        currentSongId = currentSongId,
                        onSongClick = onPlaySong,
                        onSongOptions = onOpenOptions,
                        onPlaylistClick = onOpenPlaylist,
                        onAlbumClick = onOpenAlbum,
                        onArtistClick = onOpenArtist,
                        onFolderClick = onOpenFolderDetail,
                        onLikedClick = onOpenLiked,
                        onCreatePlaylist = onRequestCreatePlaylist,
                        onRefresh = onRefresh,
                        onSearchClick = onOpenSearch,
                        onManageFolders = onOpenFolders,
                        onHiddenSongs = onOpenHidden,
                        onEqualizerClick = onOpenEqualizer,
                        onAbout = onOpenAbout,
                        onSettings = onOpenSettings,
                        onPlayQueue = onPlayQueue
                    )
                }
                composable("search") {
                    val searchViewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.factory(container)
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onPlaySong = onPlayResults,
                        onSongOptions = onOpenOptions,
                        onPlaylistClick = onOpenPlaylist
                    )
                }
                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { entry ->
                    val playlistId = entry.arguments?.getLong("playlistId") ?: return@composable
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        key = "playlist-$playlistId",
                        factory = PlaylistViewModel.factory(container, playlistId)
                    )
                    PlaylistDetailScreen(
                        viewModel = playlistViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable("liked") {
                    val likedViewModel: LikedViewModel = viewModel(
                        factory = LikedViewModel.factory(container)
                    )
                    LikedSongsScreen(
                        viewModel = likedViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable("folders") {
                    val foldersViewModel: FolderManagerViewModel = viewModel(
                        factory = FolderManagerViewModel.factory(container)
                    )
                    FolderManagerScreen(viewModel = foldersViewModel, onBack = onBack)
                }
                composable("hidden") {
                    val hiddenViewModel: HiddenSongsViewModel = viewModel(
                        factory = HiddenSongsViewModel.factory(container)
                    )
                    HiddenSongsScreen(viewModel = hiddenViewModel, onBack = onBack)
                }
                composable("about") {
                    AboutScreen(onBack = onBack)
                }
                composable("settings") {
                    SettingsScreen(container = container, onBack = onBack)
                }
                composable(
                    route = "album/{albumId}",
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                ) { entry ->
                    val albumId = entry.arguments?.getLong("albumId") ?: return@composable
                    val albumViewModel: AlbumViewModel = viewModel(
                        key = "album-$albumId",
                        factory = AlbumViewModel.factory(container, albumId)
                    )
                    AlbumDetailScreen(
                        viewModel = albumViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable(
                    route = "artist/{artistName}",
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) { entry ->
                    val rawArtistName = entry.arguments?.getString("artistName") ?: return@composable
                    val artistName = Uri.decode(rawArtistName)
                    val artistViewModel: ArtistViewModel = viewModel(
                        key = "artist-$artistName",
                        factory = ArtistViewModel.factory(container, artistName)
                    )
                    ArtistDetailScreen(
                        viewModel = artistViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable(
                    route = "folderDetail?path={path}",
                    arguments = listOf(navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    })
                ) { entry ->
                    val rawPath = entry.arguments?.getString("path") ?: ""
                    val path = Uri.decode(rawPath)
                    val folderDetailViewModel: FolderDetailViewModel = viewModel(
                        key = "folder-$path",
                        factory = FolderDetailViewModel.factory(container, path)
                    )
                    FolderDetailScreen(
                        viewModel = folderDetailViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable("equalizer") {
                    EqualizerScreen(
                        container = container,
                        onBack = onBack
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = if (reduceMotion) 0 else 260,
                    easing = EaseOut
                )
            ) { fullHeight -> fullHeight } + fadeIn(tween(if (reduceMotion) 0 else 200)),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = if (reduceMotion) 0 else 240,
                    easing = EaseOut
                )
            ) { fullHeight -> fullHeight } + fadeOut(tween(if (reduceMotion) 0 else 180))
        ) {
            NowPlayingScreen(
                state = playerState,
                isLiked = isLiked,
                onDismiss = { showNowPlaying = false },
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleLike = onTogglePlayerLike,
                onMoreClick = { currentSong?.let(onOpenOptions) },
                showLyrics = showLyrics,
                lyrics = lyrics,
                onToggleLyrics = { showLyrics = !showLyrics },
                sleepLabel = sleepLabel,
                onSleepClick = { showSleepDialog = true },
                onSpeedClick = { showSpeedSheet = true },
                onEqualizerClick = {
                    showNowPlaying = false
                    navController.navigate("equalizer")
                },
                onQueueClick = { showQueueSheet = true },
                onSongInfoClick = { currentSong?.let { infoSong = it } },
                loopRange = loopRange,
                onABRepeatClick = { showABRepeatSheet = true },
                onCarModeClick = { showCarMode = true }
            )
        }

        AnimatedVisibility(
            visible = showCarMode && currentSong != null,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = if (reduceMotion) 0 else 260,
                    easing = EaseOut
                )
            ) { fullHeight -> fullHeight } + fadeIn(tween(if (reduceMotion) 0 else 200)),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = if (reduceMotion) 0 else 240,
                    easing = EaseOut
                )
            ) { fullHeight -> fullHeight } + fadeOut(tween(if (reduceMotion) 0 else 180))
        ) {
            CarModeScreen(
                state = playerState,
                isLiked = isLiked,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekRelative = { deltaMs ->
                    val newPos = (playerState.positionMs + deltaMs).coerceIn(0L, playerState.durationMs.coerceAtLeast(0L))
                    container.playbackController.seekTo(newPos)
                },
                onToggleLike = onTogglePlayerLike,
                onExit = { showCarMode = false }
            )
        }
    }
    }

    optionsSong?.let { song ->
        SongOptionsSheet(
            song = song,
            isLiked = song.isLiked,
            playlists = playlists,
            onDismiss = { optionsSong = null },
            onToggleLike = {
                libraryViewModel.toggleLike(song.id, !song.isLiked)
                optionsSong = null
            },
            onPlayNext = {
                libraryViewModel.playNext(song)
                optionsSong = null
            },
            onAddToQueue = {
                libraryViewModel.addToQueue(song)
                optionsSong = null
            },
            onAddToPlaylist = { playlistId ->
                libraryViewModel.addToPlaylist(playlistId, song.id) { optionsSong = null }
            },
            onCreatePlaylist = { name ->
                libraryViewModel.createPlaylist(name) { playlistId ->
                    libraryViewModel.addToPlaylist(playlistId, song.id) { }
                    optionsSong = null
                }
            },
            onShowInfo = {
                val s = song
                optionsSong = null
                infoSong = s
            },
            onABRepeat = {
                val s = song
                optionsSong = null
                if (playerState.currentSong?.id != s.id) {
                    container.playbackController.playQueue(listOf(s), 0, "A-B Repeat")
                }
                showABRepeatSheet = true
            },
            onEditTags = {
                val s = song
                optionsSong = null
                editTagsSong = s
            },
            onRemoveFromPlaylist = if (currentPlaylistId != null) {
                {
                    scope.launch {
                        container.playlistRepository.removeSong(currentPlaylistId, song.id)
                    }
                    optionsSong = null
                }
            } else null,
            onRemoveFromLibrary = {
                songToHide = song
                optionsSong = null
            }
        )
    }

    infoSong?.let { song ->
        SongInfoSheet(
            song = song,
            onDismiss = { infoSong = null }
        )
    }

    songToHide?.let { song ->
        AlertDialog(
            onDismissRequest = { songToHide = null },
            title = {
                Text(
                    text = "Remove from HighSpin?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LowspinTextPrimary
                )
            },
            text = {
                Text(
                    text = "\u201C${song.title}\u201D will disappear from your library and " +
                        "future scans. The file stays on your phone and can be restored " +
                        "from Hidden songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { container.musicRepository.hideSong(song) }
                        songToHide = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToHide = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                showCreatePlaylistDialog = false
                libraryViewModel.createPlaylist(name) { id -> navController.navigate("playlist/$id") }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = {
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleLarge,
                    color = LowspinTextPrimary
                )
            },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                        Text(
                            text = "$minutes minutes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LowspinTextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    container.sleepTimer.startMinutes(minutes)
                                    showSleepDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                    Text(
                        text = "End of current track",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LowspinTextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                container.sleepTimer.startEndOfTrack()
                                showSleepDialog = false
                            }
                            .padding(vertical = 10.dp)
                    )
                    if (container.sleepTimer.isActive) {
                        Text(
                            text = "Cancel timer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    container.sleepTimer.cancel()
                                    showSleepDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            state = playerState,
            playbackController = container.playbackController,
            onSaveQueueAsPlaylist = { name, songs ->
                scope.launch {
                    val playlistId = container.playlistRepository.createPlaylist(name)
                    container.playlistRepository.addSongs(playlistId, songs.map { it.id })
                    android.widget.Toast.makeText(
                        context,
                        "Saved ${songs.size} songs to \"$name\"",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showSpeedSheet) {
        SpeedPickerSheet(
            currentSpeed = playerState.playbackSpeed,
            currentPitch = playerState.playbackPitch,
            onSpeedSelected = { speed ->
                container.playbackController.setPlaybackSpeed(speed)
            },
            onPitchSelected = { pitch ->
                container.playbackController.setPlaybackPitch(pitch)
            },
            onSpeedAndPitchSelected = { speed, pitch ->
                container.playbackController.setSpeedAndPitch(speed, pitch)
            },
            onDismiss = { showSpeedSheet = false }
        )
    }

    editTagsSong?.let { song ->
        EditTagsSheet(
            song = song,
            onSave = { title, artist, album, trackNumber, year ->
                scope.launch {
                    container.musicRepository.updateSongTags(
                        id = song.id,
                        title = title,
                        artist = artist,
                        album = album,
                        trackNumber = trackNumber,
                        year = year
                    )
                    container.playbackController.updateSongInCache(
                        songId = song.id,
                        title = title.trim().ifBlank { "Unknown Title" },
                        artist = artist.trim().ifBlank { "Unknown Artist" },
                        album = album.trim().ifBlank { "Unknown Album" },
                        trackNumber = trackNumber.coerceAtLeast(0),
                        year = year.coerceAtLeast(0)
                    )
                }
                editTagsSong = null
            },
            onDismiss = { editTagsSong = null }
        )
    }

    if (showABRepeatSheet) {
        ABRepeatSheet(
            currentPositionMs = playerState.positionMs,
            durationMs = playerState.durationMs,
            currentLoopRange = loopRange,
            onSetLoop = { startMs, endMs ->
                container.playbackController.setLoopRange(startMs, endMs)
            },
            onClearLoop = {
                container.playbackController.clearLoopRange()
            },
            onSeek = { pos ->
                container.playbackController.seekTo(pos)
            },
            onDismiss = { showABRepeatSheet = false }
        )
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun Context.findActivity(): ComponentActivity? {
    var current: Context = this
    while (true) {
        if (current is ComponentActivity) return current
        current = (current as? ContextWrapper)?.baseContext ?: return null
    }
}
