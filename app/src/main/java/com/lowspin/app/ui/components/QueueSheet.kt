package com.lowspin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.media.PlaybackController
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinSurfaceElevated
import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerUiState,
    playbackController: PlaybackController,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueSongs = remember(state.queueSize, state.currentSong) {
        playbackController.getQueueSongs()
    }
    val currentSong = state.currentSong
    val currentIndex = remember(queueSongs, currentSong) {
        if (currentSong == null) -1
        else queueSongs.indexOfFirst { it.id == currentSong.id }
    }

    var localUpcoming by remember(queueSongs, currentIndex) {
        val upcoming = if (currentIndex >= 0 && currentIndex < queueSongs.size - 1) {
            queueSongs.subList(currentIndex + 1, queueSongs.size).toMutableList()
        } else {
            mutableListOf()
        }
        mutableStateOf(upcoming)
    }

    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LowspinSurfaceHigh.copy(alpha = 0.96f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Play Queue",
                        style = MaterialTheme.typography.titleLarge,
                        color = LowspinTextPrimary
                    )
                    Text(
                        text = "${queueSongs.size} songs total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (localUpcoming.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                playbackController.clearUpcomingQueue()
                                localUpcoming = mutableListOf()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear upcoming")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = LowspinTextPrimary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (currentSong != null) {
                    item(key = "now-playing-header") {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item(key = "now-playing-item") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LowspinSurfaceElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtImage(
                                songId = currentSong.id,
                                seed = currentSong.album.ifBlank { currentSong.title },
                                modifier = Modifier.size(48.dp),
                                sizePx = 128
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item(key = "upcoming-header") {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Next Up (${localUpcoming.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = LowspinTextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (localUpcoming.isEmpty()) {
                    item(key = "upcoming-empty") {
                        Text(
                            text = "No upcoming songs in the queue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LowspinTextMuted,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = localUpcoming,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isDragging = draggingId == song.id
                        val realIndex = currentIndex + 1 + index

                        val dragModifier = Modifier.pointerInput(song.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingId = song.id
                                    dragOffset = 0f
                                },
                                onDragEnd = {
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
                                    val from = localUpcoming.indexOfFirst { it.id == song.id }
                                    if (from >= 0) {
                                        val steps = (dragOffset / rowHeightPx).roundToInt()
                                        val target = (from + steps).coerceIn(0, localUpcoming.lastIndex)
                                        if (target != from) {
                                            localUpcoming = localUpcoming.toMutableList().apply {
                                                add(target, removeAt(from))
                                            }
                                            playbackController.moveQueueItem(
                                                currentIndex + 1 + from,
                                                currentIndex + 1 + target
                                            )
                                            dragOffset -= (target - from) * rowHeightPx
                                        }
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDragging) LowspinSurfaceElevated else Color.Transparent)
                                .zIndex(if (isDragging) 1f else 0f)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtImage(
                                songId = song.id,
                                seed = song.album.ifBlank { song.title },
                                modifier = Modifier.size(44.dp),
                                sizePx = 128
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
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
                            IconButton(
                                onClick = {
                                    playbackController.removeQueueItem(realIndex)
                                    localUpcoming = localUpcoming.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = dragModifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = LowspinTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
