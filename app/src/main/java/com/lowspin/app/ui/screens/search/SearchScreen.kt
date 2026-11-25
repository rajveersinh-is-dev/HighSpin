package com.lowspin.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.RecentSearchEntity
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.components.PlaylistCard
import com.lowspin.app.ui.components.SectionHeader
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val trimmed = query.trim()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = "Songs, artists, albums",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LowspinTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.commitSearch()
                        keyboard?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = LowspinTextPrimary,
                    focusedTextColor = LowspinTextPrimary,
                    unfocusedTextColor = LowspinTextPrimary
                )
            )
        }

        val searching = trimmed.isNotEmpty() && results.query != trimmed
        Box(Modifier.height(2.dp)) {
            if (searching) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Box(Modifier.weight(1f)) {
            if (trimmed.isEmpty()) {
                RecentSearches(
                    searches = recentSearches,
                    onPick = viewModel::onQueryChange,
                    onRemove = viewModel::removeRecentSearch,
                    onClearAll = viewModel::clearRecentSearches
                )
            } else if (!searching && results.isEmpty) {
                NoResults(query = trimmed)
            } else {
                SearchResultsList(
                    results = results,
                    currentSongId = currentSongId,
                    onSongClick = { song ->
                        viewModel.commitSearch(trimmed)
                        onPlaySong(song, results.songs)
                    },
                    onSongOptions = onSongOptions,
                    onPlaylistClick = { id ->
                        viewModel.commitSearch(trimmed)
                        onPlaylistClick(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: SearchResults,
    currentSongId: Long?,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (results.playlists.isNotEmpty()) {
            item(key = "playlists-header", contentType = "header") {
                SectionHeader(title = "Playlists")
            }
            items(
                items = results.playlists,
                key = { "playlist-${it.id}" },
                contentType = { "playlist" }
            ) { playlist: PlaylistSummary ->
                PlaylistCard(
                    name = playlist.name,
                    songCount = playlist.songCount,
                    artSongId = playlist.artSongId,
                    seed = playlist.name,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
        if (results.songs.isNotEmpty()) {
            item(key = "songs-header", contentType = "header") {
                SectionHeader(title = "Songs")
            }
            items(
                items = results.songs,
                key = { "song-${it.id}" },
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(
    searches: List<RecentSearchEntity>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (searches.isEmpty()) {
            EmptySearchHint()
            return@Column
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .pressScaleClickable(onClick = onClearAll)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searches.forEach { search ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScaleClickable(onClick = { onPick(search.query) })
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = search.query,
                        style = MaterialTheme.typography.labelLarge,
                        color = LowspinTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { onRemove(search.query) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove search",
                            tint = LowspinTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Search your library",
            style = MaterialTheme.typography.titleMedium,
            color = LowspinTextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Find any song, artist or album stored on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No results for \u201C$query\u201D",
            style = MaterialTheme.typography.titleMedium,
            color = LowspinTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Check the spelling or try a different word.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
