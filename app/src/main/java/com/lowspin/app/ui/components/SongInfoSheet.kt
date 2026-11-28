package com.lowspin.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.lyrics.Lyrics
import com.lowspin.app.data.lyrics.LyricsRepository
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AudioTechnicalSpecs(
    val formatBadge: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val fileSizeBytes: Long,
    val mimeType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: SongEntity,
    lyricsRepository: LyricsRepository? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val specs by produceState<AudioTechnicalSpecs?>(initialValue = null, song.id) {
        value = withContext(Dispatchers.IO) {
            extractAudioSpecs(context, song)
        }
    }

    val scope = rememberCoroutineScope()
    var isFetchingLyrics by remember { mutableStateOf(false) }
    var runtimeLyrics by remember(song.id) { mutableStateOf<Lyrics?>(null) }
    var lyricsLoaded by remember(song.id) { mutableStateOf(lyricsRepository == null) }

    LaunchedEffect(song.id) {
        if (lyricsRepository != null) {
            runtimeLyrics = lyricsRepository.load(song)
            lyricsLoaded = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
        contentColor = LowspinTextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    songId = song.id,
                    seed = song.album.ifBlank { song.title },
                    modifier = Modifier.size(64.dp),
                    sizePx = 256,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = LowspinTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album.ifBlank { "Unknown Album" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Audio Specs Badge Card
            val formatBadge = specs?.formatBadge ?: deriveFormatFromMime(song.mimeType)
            val isLossless = formatBadge.contains("FLAC", ignoreCase = true) ||
                formatBadge.contains("WAV", ignoreCase = true) ||
                formatBadge.contains("ALAC", ignoreCase = true)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "AUDIO SPECIFICATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Format Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLossless) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLossless) LowspinOnAccent else LowspinTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecItem(
                            label = "Bitrate",
                            value = specs?.bitrateKbps?.let { "$it kbps" } ?: "VBR / Standard"
                        )
                        SpecItem(
                            label = "Sample Rate",
                            value = specs?.sampleRateHz?.let { "${it / 1000.0} kHz" } ?: "44.1 kHz"
                        )
                        SpecItem(
                            label = "Duration",
                            value = formatDuration(song.durationMs)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // File Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FILE DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val fileSizeFormatted = specs?.fileSizeBytes?.takeIf { it > 0 }?.let { bytes ->
                        String.format(Locale.US, "%.2f MB", bytes.toDouble() / (1024 * 1024))
                    } ?: "Unknown size"

                    val trackDisplay = when {
                        song.trackNumber > 0 && song.discNumber > 0 -> "#${song.trackNumber} (Disc ${song.discNumber})"
                        song.trackNumber > 0 -> "#${song.trackNumber}"
                        song.discNumber > 0 -> "Disc ${song.discNumber}"
                        else -> "Not tagged"
                    }

                    val resolvedPath = song.matchKey.ifBlank {
                        if (song.folderPath.isNotBlank()) "${song.folderPath}/${song.title}" else "Unknown"
                    }

                    DetailRow(label = "File Size", value = fileSizeFormatted)
                    DetailRow(label = "MIME Type", value = song.mimeType)
                    DetailRow(label = "Folder", value = song.folderName)
                    DetailRow(label = "Track / Disc", value = trackDisplay)
                    val currentLyrics = runtimeLyrics
                    val lyricsStatusText = when {
                        isFetchingLyrics -> "Fetching from web…"
                        !lyricsLoaded -> "Checking…"
                        currentLyrics != null -> {
                            val kind = if (currentLyrics.synced) "Synced" else "Plain"
                            val src = currentLyrics.sourceLabel.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                            "Available ($kind, ${currentLyrics.lines.size} lines$src)"
                        }
                        else -> "Not available"
                    }
                    DetailRow(
                        label = "Lyrics",
                        value = lyricsStatusText,
                        highlightValue = lyricsLoaded && currentLyrics != null
                    )
                    if (lyricsLoaded && currentLyrics == null && lyricsRepository != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        isFetchingLyrics = true
                                        val fetched = lyricsRepository.fetchOnline(song)
                                        if (fetched != null) {
                                            runtimeLyrics = fetched
                                            Toast.makeText(context, "Lyrics imported from web", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No online lyrics found for this song", Toast.LENGTH_SHORT).show()
                                        }
                                        isFetchingLyrics = false
                                    }
                                },
                                enabled = !isFetchingLyrics
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isFetchingLyrics) "Searching…" else "Fetch from Web",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    if (song.bpm > 0) {
                        DetailRow(label = "Tempo", value = "${song.bpm} BPM")
                    }
                    if (song.musicalKey.isNotBlank() && song.musicalKey != "Unknown") {
                        DetailRow(label = "Musical Key", value = song.musicalKey)
                    }
                    if (song.year > 0) {
                        DetailRow(label = "Release Year", value = song.year.toString())
                    }
                    if (song.dateAdded > 0) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        DetailRow(label = "Added to Device", value = sdf.format(Date(song.dateAdded * 1000)))
                    }
                    DetailRow(label = "File Path", value = resolvedPath)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Buttons: Share & Copy Path
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        shareAudioFile(context, song)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = LowspinOnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Share File", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        val resolvedPath = song.matchKey.ifBlank {
                            if (song.folderPath.isNotBlank()) "${song.folderPath}/${song.title}" else song.title
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Song Path", resolvedPath)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Path", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    highlightValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (highlightValue) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (highlightValue) MaterialTheme.colorScheme.primary else LowspinTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun deriveFormatFromMime(mimeType: String): String {
    return when {
        mimeType.contains("flac", ignoreCase = true) -> "FLAC"
        mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "AAC / M4A"
        mimeType.contains("ogg", ignoreCase = true) -> "OGG"
        mimeType.contains("wav", ignoreCase = true) -> "WAV"
        mimeType.contains("opus", ignoreCase = true) -> "OPUS"
        else -> "MP3"
    }
}

private fun extractAudioSpecs(context: Context, song: SongEntity): AudioTechnicalSpecs {
    var bitrateKbps: Int? = null
    var sampleRateHz: Int? = null
    var fileSize: Long = 0L

    try {
        val uri = Uri.parse(song.contentUri)
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            fileSize = pfd.statSize
        }
    } catch (_: Throwable) {
        // Fallback to checking local file if accessible
    }

    if (fileSize <= 0L) {
        val candidatePath = song.matchKey.ifBlank {
            if (song.folderPath.isNotBlank()) "${song.folderPath}/${song.title}" else ""
        }
        if (candidatePath.isNotBlank()) {
            runCatching {
                val f = File(candidatePath)
                if (f.exists() && f.isFile) fileSize = f.length()
            }
        }
    }

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(song.contentUri))
        val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        bitrateKbps = rawBitrate?.toIntOrNull()?.let { it / 1000 }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val rawSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            sampleRateHz = rawSampleRate?.toIntOrNull()
        }
    } catch (_: Throwable) {
        // Protected or corrupt media stream
    } finally {
        runCatching { retriever.release() }
    }

    return AudioTechnicalSpecs(
        formatBadge = deriveFormatFromMime(song.mimeType),
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        fileSizeBytes = fileSize,
        mimeType = song.mimeType
    )
}

private fun shareAudioFile(context: Context, song: SongEntity) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = song.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, Uri.parse(song.contentUri))
            putExtra(Intent.EXTRA_TITLE, "${song.artist} - ${song.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share audio file"))
    } catch (e: Throwable) {
        Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
