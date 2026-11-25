package com.lowspin.app.ui.components

import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.theme.LowspinSurfaceElevated
import com.lowspin.app.ui.theme.LowspinTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: SongEntity,
    isLiked: Boolean,
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onShowInfo: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromLibrary: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LowspinSurfaceHigh.copy(alpha = 0.94f),
        contentColor = LowspinTextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (showPlaylistPicker) {
            PlaylistPicker(
                song = song,
                playlists = playlists,
                onBack = { showPlaylistPicker = false },
                onPick = onAddToPlaylist,
                onCreate = { showCreateDialog = true }
            )
        } else {
            Column(Modifier.padding(bottom = 28.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImage(
                        songId = song.id,
                        seed = song.album.ifBlank { song.title },
                        modifier = Modifier.size(56.dp),
                        sizePx = 224
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                SheetActionRow(
                    icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else LowspinTextPrimary,
                    onClick = onToggleLike
                )
                SheetActionRow(
                    icon = Icons.Rounded.SkipNext,
                    label = "Play next",
                    onClick = onPlayNext
                )
                SheetActionRow(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Add to queue",
                    onClick = onAddToQueue
                )
                SheetActionRow(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    label = "Add to playlist",
                    onClick = { showPlaylistPicker = true }
                )
                if (onShowInfo != null) {
                    SheetActionRow(
                        icon = Icons.Rounded.Info,
                        label = "Track info & audio specs",
                        onClick = onShowInfo
                    )
                }
                if (onRemoveFromPlaylist != null) {
                    SheetActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Remove from this playlist",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onRemoveFromPlaylist
                    )
                }
                if (onRemoveFromLibrary != null) {
                    SheetActionRow(
                        icon = Icons.Rounded.HideSource,
                        label = "Remove from HighSpin",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onRemoveFromLibrary
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            }
        )
    }
}

@Composable
private fun PlaylistPicker(
    song: SongEntity,
    playlists: List<PlaylistSummary>,
    onBack: () -> Unit,
    onPick: (Long) -> Unit,
    onCreate: () -> Unit
) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Add to playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = LowspinTextPrimary
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            item {
                SheetActionRow(
                    icon = Icons.Rounded.Add,
                    label = "New playlist",
                    tint = LowspinTextPrimary,
                    onClick = onCreate
                )
            }
            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "No playlists yet. Create one to start collecting songs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
            items(items = playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(playlist.id) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (playlist.artSongId != null) {
                        AlbumArtImage(
                            songId = playlist.artSongId,
                            seed = playlist.name,
                            modifier = Modifier.size(40.dp),
                            sizePx = 160
                        )
                    } else {
                        Box(Modifier.size(40.dp)) {
                            GradientPlaceholderBox(
                                seed = playlist.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (playlist.songCount == 1) {
                                "1 song"
                            } else {
                                "${playlist.songCount} songs"
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
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = LowspinTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LowspinSurfaceHigh.copy(alpha = 0.94f),
        title = {
            Text(
                text = "New playlist",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                singleLine = true,
                placeholder = { Text("Playlist name") },
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = LowspinTextPrimary)
            ) {
                Text("Create")
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
