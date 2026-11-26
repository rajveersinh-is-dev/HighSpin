package com.lowspin.app.ui.screens.decades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary

data class DecadeBucket(
    val decadeStart: Int,
    val label: String,
    val songs: List<SongEntity>
)

fun groupSongsByDecade(songs: List<SongEntity>): List<DecadeBucket> {
    val buckets = HashMap<Int, MutableList<SongEntity>>()
    for (song in songs) {
        val decade = if (song.year in 1900..2099) (song.year / 10) * 10 else 0
        buckets.getOrPut(decade) { ArrayList() }.add(song)
    }
    return buckets.entries
        .sortedByDescending { it.key }
        .map { (decade, list) ->
            val label = when {
                decade == 0 -> "Undated Tracks"
                decade < 1960 -> "Pre-1960s Classics"
                else -> "${decade}s"
            }
            DecadeBucket(decade, label, list.sortedBy { it.year })
        }
}

@Composable
fun DecadeBrowserScreen(
    songs: List<SongEntity>,
    currentSongId: Long?,
    onBack: () -> Unit,
    onPlayQueue: (List<SongEntity>, Int, String) -> Unit,
    onSongOptions: (SongEntity) -> Unit
) {
    val buckets = remember(songs) { groupSongsByDecade(songs) }
    var selectedDecade by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedDecade != null) selectedDecade = null else onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            val activeBucket = buckets.firstOrNull { it.decadeStart == selectedDecade }
            Text(
                text = activeBucket?.label ?: "Browse by Decade",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (activeBucket != null && activeBucket.songs.isNotEmpty()) {
                IconButton(onClick = { onPlayQueue(activeBucket.songs, 0, activeBucket.label) }) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play decade",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val activeBucket = buckets.firstOrNull { it.decadeStart == selectedDecade }
        if (activeBucket != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(items = activeBucket.songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = {
                            val idx = activeBucket.songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                            onPlayQueue(activeBucket.songs, idx, activeBucket.label)
                        },
                        onMoreClick = { onSongOptions(song) },
                        onLongClick = { onSongOptions(song) }
                    )
                }
            }
        } else if (buckets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No songs in library yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = buckets, key = { it.decadeStart }) { bucket ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedDecade = bucket.decadeStart }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val artSong = bucket.songs.firstOrNull()
                        if (artSong != null) {
                            AlbumArtImage(
                                songId = artSong.id,
                                seed = bucket.label,
                                modifier = Modifier.size(56.dp),
                                sizePx = 160,
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LowspinTextPrimary
                            )
                            Text(
                                text = "${bucket.songs.size} ${if (bucket.songs.size == 1) "track" else "tracks"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onPlayQueue(bucket.songs, 0, bucket.label) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Play",
                                style = MaterialTheme.typography.labelMedium,
                                color = LowspinOnAccent
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(92.dp)) }
            }
        }
    }
}
