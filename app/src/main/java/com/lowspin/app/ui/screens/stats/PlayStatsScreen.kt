package com.lowspin.app.ui.screens.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.di.AppContainer
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.theme.LowspinTextPrimary
import kotlin.math.roundToInt

@Composable
fun PlayStatsScreen(
    container: AppContainer,
    songs: List<SongEntity>,
    currentSongId: Long?,
    onBack: () -> Unit,
    onPlayQueue: (List<SongEntity>, Int, String) -> Unit,
    onSongOptions: (SongEntity) -> Unit
) {
    val weekAgo = remember { System.currentTimeMillis() - (7L * 24 * 3600 * 1000) }
    val totalMs by container.musicRepository.totalListenedMs.collectAsStateWithLifecycle(initialValue = 0L)
    val weeklyMs by remember(weekAgo) { container.musicRepository.observeWeeklyListenedMs(weekAgo) }
        .collectAsStateWithLifecycle(initialValue = 0L)
    val totalPlays by container.musicRepository.totalPlayCount.collectAsStateWithLifecycle(initialValue = 0)
    val weeklyPlays by remember(weekAgo) { container.musicRepository.observeWeeklyPlayCount(weekAgo) }
        .collectAsStateWithLifecycle(initialValue = 0)
    val topArtists by remember(weekAgo) { container.musicRepository.observeTopArtistsSince(weekAgo, 8) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val mostPlayedSongs by container.musicRepository.mostPlayed.collectAsStateWithLifecycle(initialValue = emptyList())

    val avgBpm = remember(songs) {
        val bpms = songs.filter { it.bpm > 0 }.map { it.bpm }
        if (bpms.isEmpty()) 0 else bpms.average().roundToInt()
    }
    val topKey = remember(songs) {
        songs.filter { it.musicalKey.isNotBlank() && it.musicalKey != "Unknown" }
            .groupingBy { it.musicalKey }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "—"
    }

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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = "Listening Stats",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hero_row_1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Listening",
                        value = formatHoursMinutes(totalMs ?: 0L),
                        subtitle = "$totalPlays recorded plays",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "This Week",
                        value = formatHoursMinutes(weeklyMs ?: 0L),
                        subtitle = "$weeklyPlays plays in 7 days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "hero_row_2") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Library Avg BPM",
                        value = if (avgBpm > 0) "$avgBpm" else "—",
                        subtitle = "Detected tempo",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Dominant Key",
                        value = topKey,
                        subtitle = "Most common tonality",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "top_artists_header") {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Top Artists This Week",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LowspinTextPrimary
                )
            }

            if (topArtists.isEmpty()) {
                item(key = "no_top_artists") {
                    Text(
                        text = "Listen to a few tracks to build your weekly artist leaderboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = topArtists, key = { "artist_${it.artist}" }) { stat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            songId = stat.artSongId,
                            seed = stat.artist,
                            modifier = Modifier.size(44.dp),
                            sizePx = 128,
                            shape = CircleShape
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stat.artist,
                                style = MaterialTheme.typography.titleSmall,
                                color = LowspinTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stat.playCount} plays · ${formatHoursMinutes(stat.totalDurationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "most_played_header") {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Most Played Songs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LowspinTextPrimary
                )
            }

            if (mostPlayedSongs.isEmpty()) {
                item(key = "no_most_played") {
                    Text(
                        text = "No play counts recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = mostPlayedSongs, key = { "most_${it.id}" }) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            SongRow(
                                song = song,
                                isCurrent = song.id == currentSongId,
                                onClick = {
                                    val idx = mostPlayedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    onPlayQueue(mostPlayedSongs, idx, "Most Played")
                                },
                                onMoreClick = { onSongOptions(song) },
                                onLongClick = { onSongOptions(song) },
                                showDuration = false
                            )
                        }
                        Text(
                            text = "${song.playCount}×",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatHoursMinutes(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
