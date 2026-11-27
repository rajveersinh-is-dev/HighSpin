package com.lowspin.app.ui.screens.smartplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.db.LowspinDatabase
import com.lowspin.app.data.db.SmartPlaylistEntity
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.MoodClassifier
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistScreen(
    database: LowspinDatabase,
    allSongs: List<SongEntity>,
    onBack: () -> Unit,
    onPlayQueue: (List<SongEntity>, Int, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val smartPlaylists by database.smartPlaylistDao().observeAll().collectAsStateWithLifecycle(emptyList())
    var showCreateSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = "Smart Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LowspinTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (smartPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No Smart Playlists Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LowspinTextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Create dynamic playlists that automatically filter your music library by BPM, mood, play count, liked status, or release year.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LowspinTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(smartPlaylists, key = { it.id }) { pl ->
                        val matchingSongs = remember(pl.rules, allSongs) {
                            evaluateRules(pl.rules, allSongs)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                .clickable {
                                    if (matchingSongs.isNotEmpty()) {
                                        onPlayQueue(matchingSongs, 0, pl.name)
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = LowspinTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${matchingSongs.size} songs \u00B7 ${formatRulesSummary(pl.rules)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LowspinTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (matchingSongs.isNotEmpty()) {
                                    IconButton(onClick = { onPlayQueue(matchingSongs, 0, pl.name) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch { database.smartPlaylistDao().delete(pl.id) }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = LowspinTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = LowspinOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Create Smart Playlist")
        }

        if (showCreateSheet) {
            CreateSmartPlaylistSheet(
                onDismiss = { showCreateSheet = false },
                onSave = { name, rulesJson ->
                    scope.launch {
                        database.smartPlaylistDao().insert(
                            SmartPlaylistEntity(name = name, rules = rulesJson)
                        )
                    }
                    showCreateSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateSmartPlaylistSheet(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var likedOnly by remember { mutableStateOf(false) }
    var minBpm by remember { mutableFloatStateOf(0f) }
    var maxBpm by remember { mutableFloatStateOf(200f) }
    var minPlayCount by remember { mutableFloatStateOf(0f) }
    var minYear by remember { mutableFloatStateOf(0f) }
    val availableMoods = listOf("Energetic", "Calm", "Happy", "Melancholic", "Focus", "Party", "Dark")
    val selectedMoods = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New Smart Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LowspinTextPrimary
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Liked Songs Only", style = MaterialTheme.typography.bodyMedium, color = LowspinTextPrimary)
                Switch(
                    checked = likedOnly,
                    onCheckedChange = { likedOnly = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = LowspinOnAccent, checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "BPM Range: ${minBpm.roundToInt()} - ${if (maxBpm >= 200f) "Any" else "${maxBpm.roundToInt()}"}",
                style = MaterialTheme.typography.bodyMedium,
                color = LowspinTextPrimary
            )
            Slider(
                value = minBpm,
                onValueChange = { minBpm = it.coerceAtMost(maxBpm) },
                valueRange = 0f..200f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
            Slider(
                value = maxBpm,
                onValueChange = { maxBpm = it.coerceAtLeast(minBpm) },
                valueRange = 0f..200f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Min Play Count: ${minPlayCount.roundToInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = LowspinTextPrimary
            )
            Slider(
                value = minPlayCount,
                onValueChange = { minPlayCount = it },
                valueRange = 0f..50f,
                steps = 10,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Released After Year: ${if (minYear.roundToInt() <= 1960) "Any" else minYear.roundToInt().toString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = LowspinTextPrimary
            )
            Slider(
                value = minYear,
                onValueChange = { minYear = it },
                valueRange = 1960f..2026f,
                steps = 33,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(12.dp))

            Text("Mood Filters", style = MaterialTheme.typography.bodyMedium, color = LowspinTextPrimary)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableMoods.forEach { mood ->
                    val isSelected = mood in selectedMoods
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedMoods.remove(mood)
                            else selectedMoods.add(mood)
                        },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = LowspinOnAccent
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val finalName = name.ifBlank { "Smart Mix" }
                    val json = JSONObject().apply {
                        if (likedOnly) put("likedOnly", true)
                        if (minBpm > 0f) put("minBpm", minBpm.roundToInt())
                        if (maxBpm < 200f) put("maxBpm", maxBpm.roundToInt())
                        if (minPlayCount > 0f) put("minPlayCount", minPlayCount.roundToInt())
                        if (minYear > 1960f) put("minYear", minYear.roundToInt())
                        if (selectedMoods.isNotEmpty()) {
                            val arr = JSONArray()
                            selectedMoods.forEach { arr.put(it) }
                            put("moods", arr)
                        }
                    }
                    onSave(finalName, json.toString())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = LowspinOnAccent
                )
            ) {
                Text("Save Smart Playlist", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

private fun evaluateRules(rulesJson: String, allSongs: List<SongEntity>): List<SongEntity> {
    if (rulesJson.isBlank()) return allSongs
    val obj = runCatching { JSONObject(rulesJson) }.getOrNull() ?: return allSongs

    val likedOnly = obj.optBoolean("likedOnly", false)
    val minBpm = obj.optInt("minBpm", 0)
    val maxBpm = obj.optInt("maxBpm", 0)
    val minPlayCount = obj.optInt("minPlayCount", 0)
    val minYear = obj.optInt("minYear", 0)
    val moods = obj.optJSONArray("moods")?.let { arr ->
        (0 until arr.length()).map { arr.getString(it) }.toSet()
    } ?: emptySet()

    return allSongs.filter { song ->
        if (likedOnly && !song.isLiked) return@filter false
        if (minBpm > 0 && song.bpm > 0 && song.bpm < minBpm) return@filter false
        if (maxBpm > 0 && song.bpm > 0 && song.bpm > maxBpm) return@filter false
        if (minPlayCount > 0 && song.playCount < minPlayCount) return@filter false
        if (minYear > 0 && song.year > 0 && song.year < minYear) return@filter false
        if (moods.isNotEmpty()) {
            val songMood = MoodClassifier.classify(song)?.label ?: ""
            if (songMood !in moods) return@filter false
        }
        true
    }
}

private fun formatRulesSummary(rulesJson: String): String {
    val obj = runCatching { JSONObject(rulesJson) }.getOrNull() ?: return "Dynamic rules"
    val parts = mutableListOf<String>()
    if (obj.optBoolean("likedOnly", false)) parts.add("Liked")
    val minB = obj.optInt("minBpm", 0)
    val maxB = obj.optInt("maxBpm", 0)
    if (minB > 0 && maxB > 0) parts.add("$minB-$maxB BPM")
    else if (minB > 0) parts.add("BPM \u2265 $minB")
    else if (maxB > 0) parts.add("BPM \u2264 $maxB")

    val minP = obj.optInt("minPlayCount", 0)
    if (minP > 0) parts.add("Plays \u2265 $minP")
    val minYear = obj.optInt("minYear", 0)
    if (minYear > 0) parts.add("Year \u2265 $minYear")
    val moods = obj.optJSONArray("moods")
    if (moods != null && moods.length() > 0) {
        parts.add("${moods.length()} moods")
    }
    return if (parts.isEmpty()) "All library" else parts.joinToString(" \u00B7 ")
}
