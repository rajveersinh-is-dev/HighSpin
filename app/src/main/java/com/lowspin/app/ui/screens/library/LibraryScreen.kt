package com.lowspin.app.ui.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.lowspin.app.data.db.AlbumSummary
import com.lowspin.app.data.db.ArtistSummary
import com.lowspin.app.data.db.FolderSummary
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.LikedSongsCard
import com.lowspin.app.ui.components.NewPlaylistCard
import com.lowspin.app.ui.components.PlaylistCard
import com.lowspin.app.ui.components.SectionHeader
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.components.VinylGlyph
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    playlists: List<PlaylistSummary>,
    albums: List<AlbumSummary>,
    artists: List<ArtistSummary>,
    folders: List<FolderSummary>,
    likedCount: Int,
    scanState: ScanState,
    currentSongId: Long?,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onLikedClick: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit,
    onManageFolders: () -> Unit,
    onHiddenSongs: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAbout: () -> Unit,
    onSettings: () -> Unit,
    onPlayQueue: ((List<SongEntity>, Int, String) -> Unit)? = null
) {
    val scanning = scanState is ScanState.Scanning || scanState is ScanState.Enriching
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val filteredCount = (scanState as? ScanState.Complete)?.filteredCount ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LibraryHeader(
            songCount = songs.size,
            onSearchClick = onSearchClick,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            scanning = scanning,
            onRefresh = onRefresh,
            onManageFolders = onManageFolders,
            onHiddenSongs = onHiddenSongs,
            onEqualizerClick = onEqualizerClick,
            onAbout = onAbout,
            onSettings = onSettings
        )

        ScanBanner(scanState)

        if (filteredCount > 0) {
            FilteredBanner(count = filteredCount, onManage = onManageFolders)
        }

        LibraryTabs(selected = tab, onSelect = { tab = it })

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> SongsTab(
                    songs = songs,
                    scanState = scanState,
                    currentSongId = currentSongId,
                    onSongClick = onSongClick,
                    onSongOptions = onSongOptions,
                    onRefresh = onRefresh
                )
                1 -> PlaylistsTab(
                    playlists = playlists,
                    songs = songs,
                    likedCount = likedCount,
                    onSongClick = onSongClick,
                    onPlayQueue = onPlayQueue,
                    onPlaylistClick = onPlaylistClick,
                    onLikedClick = onLikedClick,
                    onCreatePlaylist = onCreatePlaylist
                )
                2 -> AlbumsTab(
                    albums = albums,
                    onAlbumClick = onAlbumClick
                )
                3 -> ArtistsTab(
                    artists = artists,
                    onArtistClick = onArtistClick
                )
                4 -> FoldersTab(
                    folders = folders,
                    onFolderClick = onFolderClick
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    onSearchClick: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    scanning: Boolean,
    onRefresh: () -> Unit,
    onManageFolders: () -> Unit,
    onHiddenSongs: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAbout: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VinylGlyph(
                modifier = Modifier.size(24.dp),
                rotating = scanning
            )
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (songCount == 0) "Offline" else "$songCount tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = LowspinTextPrimary
            )
        }
        IconButton(onClick = onEqualizerClick) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Equalizer",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            IconButton(onClick = { onMenuOpenChange(true) }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f)
            ) {
                DropdownMenuItem(
                    text = { Text("Rescan library", color = LowspinTextPrimary) },
                    enabled = !scanning,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = if (scanning) LowspinTextMuted else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onRefresh()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Equalizer", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onEqualizerClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Manage folders", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onManageFolders()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hidden songs", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.HideSource,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onHiddenSongs()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About HighSpin", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onAbout()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabPill(label = "Songs", selected = selected == 0, onClick = { onSelect(0) })
        TabPill(label = "Playlists", selected = selected == 1, onClick = { onSelect(1) })
        TabPill(label = "Albums", selected = selected == 2, onClick = { onSelect(2) })
        TabPill(label = "Artists", selected = selected == 3, onClick = { onSelect(3) })
        TabPill(label = "Folders", selected = selected == 4, onClick = { onSelect(4) })
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        animationSpec = tween(180),
        label = "tabBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) LowspinOnAccent else LowspinTextPrimary,
        animationSpec = tween(180),
        label = "tabContent"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = content
        )
    }
}

@Composable
private fun SongsTab(
    songs: List<SongEntity>,
    scanState: ScanState,
    currentSongId: Long?,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onRefresh: () -> Unit
) {
    when {
        songs.isEmpty() && scanState !is ScanState.Complete && scanState !is ScanState.Failed ->
            LoadingState()

        songs.isEmpty() -> EmptyLibrary(onRefresh)

        else -> {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            var activeLetter by remember { mutableStateOf<Char?>(null) }

            // Grouping letter map: Letter -> first index in songs list
            val letterIndices = remember(songs) {
                val map = mutableMapOf<Char, Int>()
                songs.forEachIndexed { index, song ->
                    val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
                    val letter = if (firstChar in 'A'..'Z') firstChar else '#'
                    if (!map.containsKey(letter)) {
                        map[letter] = index
                    }
                }
                map
            }

            val alphabet = remember(letterIndices) {
                val letters = ('A'..'Z').filter { letterIndices.containsKey(it) }
                if (letterIndices.containsKey('#')) listOf('#') + letters else letters
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                ) {
                    items(
                        items = songs,
                        key = { it.id },
                        contentType = { "song" }
                    ) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onSongClick(song) },
                            onMoreClick = { onSongOptions(song) },
                            onLongClick = { onSongOptions(song) }
                        )
                    }
                }

                // Interactive A-Z Fast Scrollbar
                if (songs.size >= 12 && alphabet.size > 2) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.50f))
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            alphabet.forEach { char ->
                                val targetIndex = letterIndices[char]
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (activeLetter == char) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .clickable {
                                            if (targetIndex != null) {
                                                activeLetter = char
                                                coroutineScope.launch {
                                                    listState.scrollToItem(targetIndex)
                                                }
                                            }
                                        }
                                        .padding(vertical = 1.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // Floating active letter indicator bubble
                    activeLetter?.let { char ->
                        LaunchedEffect(char) {
                            kotlinx.coroutines.delay(1200)
                            activeLetter = null
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .shadow(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = LowspinOnAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistSummary>,
    songs: List<SongEntity>,
    likedCount: Int,
    onSongClick: (SongEntity) -> Unit,
    onPlayQueue: ((List<SongEntity>, Int, String) -> Unit)?,
    onPlaylistClick: (Long) -> Unit,
    onLikedClick: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "liked", contentType = "liked") {
            LikedSongsCard(
                songCount = likedCount,
                onClick = onLikedClick,
                modifier = Modifier.animateItem()
            )
        }
        item(key = "recent", contentType = "recent") {
            val thirtyDaysAgo = remember { System.currentTimeMillis() - (30L * 24 * 3600 * 1000) }
            val recentSongs = remember(songs) {
                songs.filter { it.dateAdded * 1000L > thirtyDaysAgo }.sortedByDescending { it.dateAdded }
            }
            SmartPlaylistCard(
                title = "Recently Added",
                subtitle = if (recentSongs.size == 1) "1 song added recently" else "${recentSongs.size} songs added recently",
                icon = Icons.Rounded.Schedule,
                gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
                onClick = {
                    if (recentSongs.isNotEmpty()) {
                        if (onPlayQueue != null) {
                            onPlayQueue(recentSongs, 0, "Recently Added")
                        } else {
                            onSongClick(recentSongs.first())
                        }
                    } else if (songs.isNotEmpty()) {
                        onSongClick(songs.first())
                    }
                },
                modifier = Modifier.animateItem()
            )
        }
        item(key = "most_played", contentType = "most_played") {
            val mostPlayedSongs = remember(songs) {
                songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
            }
            SmartPlaylistCard(
                title = "Most Played",
                subtitle = if (mostPlayedSongs.size == 1) "1 song in rotation" else "${mostPlayedSongs.size} songs in rotation",
                icon = Icons.Rounded.Star,
                gradientColors = listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
                onClick = {
                    if (mostPlayedSongs.isNotEmpty()) {
                        if (onPlayQueue != null) {
                            onPlayQueue(mostPlayedSongs, 0, "Most Played")
                        } else {
                            onSongClick(mostPlayedSongs.first())
                        }
                    } else if (songs.isNotEmpty()) {
                        onSongClick(songs.first())
                    }
                },
                modifier = Modifier.animateItem()
            )
        }
        item(key = "header", contentType = "header") {
            SectionHeader(title = "Your playlists")
        }
        if (playlists.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                Text(
                    text = "Create a playlist to collect the songs you love.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(
            items = playlists,
            key = { it.id },
            contentType = { "playlist" }
        ) { playlist ->
            PlaylistCard(
                name = playlist.name,
                songCount = playlist.songCount,
                artSongId = playlist.artSongId,
                seed = playlist.name,
                onClick = { onPlaylistClick(playlist.id) },
                modifier = Modifier.animateItem()
            )
        }
        item(key = "new", contentType = "new") {
            NewPlaylistCard(
                onClick = onCreatePlaylist,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LowspinTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = LowspinTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<AlbumSummary>,
    onAlbumClick: (Long) -> Unit
) {
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No albums found in your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(items = albums, key = { "${it.albumId}_${it.album}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .pressScaleClickable(onClick = { onAlbumClick(album.albumId) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (album.artSongId > 0) {
                        AlbumArtImage(
                            songId = album.artSongId,
                            seed = album.album,
                            modifier = Modifier.size(56.dp),
                            sizePx = 160,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = album.album,
                            style = MaterialTheme.typography.titleSmall,
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${album.artist} \u00B7 ${if (album.songCount == 1) "1 song" else "${album.songCount} songs"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<ArtistSummary>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No artists found in your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(items = artists, key = { it.artist }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .pressScaleClickable(onClick = { onArtistClick(artist.artist) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (artist.artSongId > 0) {
                        AlbumArtImage(
                            songId = artist.artSongId,
                            seed = artist.artist,
                            modifier = Modifier.size(56.dp),
                            sizePx = 160,
                            shape = CircleShape
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = artist.artist,
                            style = MaterialTheme.typography.titleSmall,
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                if (artist.albumCount > 0) append("${artist.albumCount} albums \u00B7 ")
                                append(if (artist.songCount == 1) "1 song" else "${artist.songCount} songs")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTab(
    folders: List<FolderSummary>,
    onFolderClick: (String) -> Unit
) {
    if (folders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No music folders found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(items = folders, key = { "${it.folderPath}_${it.folderName}" }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .pressScaleClickable(onClick = { onFolderClick(folder.folderPath) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folder.folderName,
                            style = MaterialTheme.typography.titleSmall,
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${if (folder.songCount == 1) "1 song" else "${folder.songCount} songs"} \u00B7 ${folder.folderPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilteredBanner(count: Int, onManage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.FolderOff,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (count == 1) {
                "1 recording hidden"
            } else {
                "$count recordings hidden"
            },
            style = MaterialTheme.typography.labelSmall,
            color = LowspinTextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Manage",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .pressScaleClickable(onClick = onManage)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ScanBanner(scanState: ScanState) {
    when (scanState) {
        is ScanState.Scanning -> {
            val fraction = if (scanState.total > 0) {
                scanState.scanned.toFloat() / scanState.total.toFloat()
            } else {
                0f
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (scanState.total > 0) {
                        "Scanning library\u2026 ${scanState.scanned} of ${scanState.total}"
                    } else {
                        "Scanning library\u2026"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is ScanState.Enriching -> {
            val fraction = if (scanState.total > 0) {
                scanState.done.toFloat() / scanState.total.toFloat()
            } else {
                0f
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Reading tags\u2026 ${scanState.done} of ${scanState.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is ScanState.Failed -> {
            Text(
                text = "Scan failed: ${scanState.message}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        else -> Unit
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VinylGlyph(modifier = Modifier.size(48.dp), rotating = true)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Looking for music on this device\u2026",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibrary(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.headlineSmall,
            color = LowspinTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Copy audio files to your phone, then rescan. HighSpin only reads local files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                contentColor = LowspinTextPrimary
            )
        ) {
            Text("Rescan library", style = MaterialTheme.typography.titleSmall)
        }
    }
}
