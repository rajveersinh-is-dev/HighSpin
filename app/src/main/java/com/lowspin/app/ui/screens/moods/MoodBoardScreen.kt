package com.lowspin.app.ui.screens.moods

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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.MoodCategory
import com.lowspin.app.data.scanner.MoodClassifier
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun MoodBoardScreen(
    songs: List<SongEntity>,
    currentSongId: Long?,
    analysisProgress: Pair<Int, Int>?,
    onAnalyzeAudio: () -> Unit,
    onBack: () -> Unit,
    onPlayQueue: (List<SongEntity>, Int, String) -> Unit,
    onSongOptions: (SongEntity) -> Unit
) {
    val grouped = remember(songs) { MoodClassifier.groupByMood(songs) }
    val unanalyzedCount = remember(songs) { songs.count { it.bpm == 0 } }
    var selectedMood by remember { mutableStateOf<MoodCategory?>(null) }

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
                if (selectedMood != null) selectedMood = null else onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = selectedMood?.label ?: "Mood Board Playlists",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            val activeSongs = selectedMood?.let { grouped[it] }.orEmpty()
            if (activeSongs.isNotEmpty()) {
                IconButton(onClick = { onPlayQueue(activeSongs, 0, selectedMood!!.label) }) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play mood",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val currentMood = selectedMood
        if (currentMood != null) {
            val moodSongs = grouped[currentMood].orEmpty()
            if (moodSongs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No analyzed tracks match ${currentMood.label} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(items = moodSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = {
                                val idx = moodSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                onPlayQueue(moodSongs, idx, currentMood.label)
                            },
                            onMoreClick = { onSongOptions(song) },
                            onLongClick = { onSongOptions(song) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (analysisProgress != null || unanalyzedCount > 0) {
                    item(key = "analyzer_banner") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(14.dp)
                        ) {
                            if (analysisProgress != null) {
                                val (done, total) = analysisProgress
                                Text(
                                    text = "Analyzing audio BPM & Musical Key ($done / $total)…",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = LowspinTextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (total > 0) done.toFloat() / total else 0f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "$unanalyzedCount tracks pending BPM & Key",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LowspinTextPrimary
                                        )
                                        Text(
                                            text = "On-device PCM tempo & harmonic detector",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = onAnalyzeAudio,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = LowspinOnAccent
                                        )
                                    ) {
                                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Analyze")
                                    }
                                }
                            }
                        }
                    }
                }

                items(items = MoodCategory.entries, key = { it.id }) { mood ->
                    val moodSongs = grouped[mood].orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(mood.colorTopHex).copy(alpha = 0.28f),
                                        Color(mood.colorBottomHex).copy(alpha = 0.22f)
                                    )
                                )
                            )
                            .clickable { selectedMood = mood }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(mood.colorTopHex), Color(mood.colorBottomHex))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LowspinTextPrimary
                            )
                            Text(
                                text = mood.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${moodSongs.size} ${if (moodSongs.size == 1) "song" else "songs"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (moodSongs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onPlayQueue(moodSongs, 0, mood.label) }
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
                }
                item { Spacer(Modifier.height(92.dp)) }
            }
        }
    }
}
