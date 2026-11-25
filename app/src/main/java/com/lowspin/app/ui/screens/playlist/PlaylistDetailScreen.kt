package com.lowspin.app.ui.screens.playlist

import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinOnAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.components.CollageArt
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinSurfaceElevated
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatTotalDuration
import kotlin.math.roundToInt

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongOptions: (SongEntity) -> Unit
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artSongIds by viewModel.artSongIds.collectAsStateWithLifecycle()

    val name = playlist?.name ?: "Playlist"
    val seedColor = remember(name) { Color(GradientPlaceholder.primaryColor(name)) }

    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    // Local order so a drag can move rows instantly; persisted when the drag ends.
    var localSongs by remember(songs) { mutableStateOf(songs) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = com.lowspin.app.ui.settings.LocalListStyle.current.rowHeight
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            seedColor.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LowspinTextPrimary
                    )
                }
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Playlist options",
                            tint = LowspinTextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = LowspinSurfaceHigh.copy(alpha = 0.94f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename playlist", color = LowspinTextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                menuOpen = false
                                showRename = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete playlist",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuOpen = false
                                showDelete = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "hero", contentType = "hero") {
                    PlaylistHero(
                        name = name,
                        artSongIds = artSongIds,
                        songCount = songs.size,
                        totalDurationMs = songs.sumOf { it.durationMs },
                        onPlay = { viewModel.play(0) },
                        onShuffle = viewModel::shufflePlay
                    )
                }

                if (localSongs.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No songs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = LowspinTextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Open any song's menu and choose \u201CAdd to playlist\u201D.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = localSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> "song" }
                    ) { index, song ->
                        val isDragging = draggingId == song.id
                        val handleModifier = Modifier.pointerInput(song.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingId = song.id
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    if (draggingId != null) {
                                        viewModel.reorder(localSongs)
                                    }
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val from = localSongs.indexOfFirst { it.id == song.id }
                                    if (from >= 0) {
                                        val steps = (dragOffset / rowHeightPx).roundToInt()
                                        val target = (from + steps).coerceIn(0, localSongs.lastIndex)
                                        if (target != from) {
                                            localSongs = localSongs.toMutableList().apply {
                                                add(target, removeAt(from))
                                            }
                                            dragOffset -= (target - from) * rowHeightPx
                                        }
                                    }
                                }
                            )
                        }

                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = {
                                val index = localSongs.indexOfFirst { it.id == song.id }
                                viewModel.play(index.coerceAtLeast(0))
                            },
                            onMoreClick = { onSongOptions(song) },
                            dragHandleModifier = handleModifier,
                            showDuration = false,
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 1f else 0f)
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        RenamePlaylistDialog(
            initialName = name,
            onDismiss = { showRename = false },
            onRename = { newName ->
                showRename = false
                viewModel.rename(newName)
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = "Delete playlist?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LowspinTextPrimary
                )
            },
            text = {
                Text(
                    text = "\u201C$name\u201D will be removed. The songs stay in your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        viewModel.delete(onDeleted = onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaylistHero(
    name: String,
    artSongIds: List<Long>,
    songCount: Int,
    totalDurationMs: Long,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        CollageArt(songIds = artSongIds, seed = name, size = 190.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = LowspinTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append(if (songCount == 1) "1 song" else "$songCount songs")
                if (songCount > 0) {
                    append(" \u00B7 ")
                    append(formatTotalDuration(totalDurationMs))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .pressScaleClickable(onClick = onPlay)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = LowspinOnAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.titleSmall,
                        color = LowspinOnAccent
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = onShuffle,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                    contentColor = LowspinTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Shuffle", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (songCount > 0) {
            Text(
                text = "Drag the handle to reorder",
                style = MaterialTheme.typography.labelSmall,
                color = LowspinTextMuted
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RenamePlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = "Rename playlist",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = LowspinTextPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Cancel")
            }
        }
    )
}
