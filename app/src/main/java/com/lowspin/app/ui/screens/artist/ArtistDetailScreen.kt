package com.lowspin.app.ui.screens.artist

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatTotalDuration

@Composable
fun ArtistDetailScreen(
    viewModel: ArtistViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongOptions: (SongEntity) -> Unit
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artistName = viewModel.artistName
    val firstSong = songs.firstOrNull()
    val dominantArgb = firstSong?.dominantColorArgb ?: 0
    val seedColor = remember(artistName, dominantArgb) {
        if (dominantArgb != 0) Color(dominantArgb)
        else Color(GradientPlaceholder.primaryColor(artistName))
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            seedColor.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
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
                    text = "Artist",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "hero", contentType = "hero") {
                    ArtistHero(
                        artistName = artistName,
                        artSongId = firstSong?.id,
                        albumsCount = songs.map { it.albumId }.distinct().size,
                        songCount = songs.size,
                        totalDurationMs = songs.sumOf { it.durationMs },
                        onPlay = { viewModel.play(0) },
                        onShuffle = viewModel::shufflePlay
                    )
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "song" }
                ) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { viewModel.play(index) },
                        onMoreClick = { onSongOptions(song) },
                        onLongClick = { onSongOptions(song) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistHero(
    artistName: String,
    artSongId: Long?,
    albumsCount: Int,
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
        if (artSongId != null) {
            AlbumArtImage(
                songId = artSongId,
                seed = artistName,
                modifier = Modifier.size(170.dp),
                sizePx = 512,
                shape = CircleShape
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineMedium,
            color = LowspinTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                if (albumsCount > 0) append(if (albumsCount == 1) "1 album \u00B7 " else "$albumsCount albums \u00B7 ")
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
        Spacer(Modifier.height(14.dp))
    }
}
