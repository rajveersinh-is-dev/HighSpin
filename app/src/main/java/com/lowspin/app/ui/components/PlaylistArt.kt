package com.lowspin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

/**
 * A 2x2 album-art collage, the way playlist covers are usually presented.
 * Falls back to deterministic gradients when a slot has no artwork.
 */
@Composable
fun CollageArt(
    songIds: List<Long>,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val ids = songIds.take(4)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    ) {
        when (ids.size) {
            0 -> GradientPlaceholderBox(seed = seed, modifier = Modifier.fillMaxSize())
            1 -> AlbumArtImage(
                songId = ids[0],
                seed = seed,
                modifier = Modifier.fillMaxSize(),
                sizePx = 640,
                shape = RectangleShape
            )
            else -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    CollageCell(ids.getOrNull(0), seed, Modifier.weight(1f).fillMaxHeight())
                    CollageCell(ids.getOrNull(1), seed, Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f)) {
                    CollageCell(ids.getOrNull(2), seed, Modifier.weight(1f).fillMaxHeight())
                    CollageCell(ids.getOrNull(3), seed, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun CollageCell(songId: Long?, seed: String, modifier: Modifier) {
    if (songId != null) {
        AlbumArtImage(
            songId = songId,
            seed = seed,
            modifier = modifier,
            sizePx = 320,
            shape = RectangleShape
        )
    } else {
        GradientPlaceholderBox(seed = "$seed-fallback", modifier = modifier)
    }
}

@Composable
fun PlaylistCard(
    name: String,
    songCount: Int,
    artSongId: Long?,
    seed: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (artSongId != null) {
            AlbumArtImage(
                songId = artSongId,
                seed = seed,
                modifier = Modifier.size(56.dp),
                sizePx = 224
            )
        } else {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))) {
                GradientPlaceholderBox(seed = seed, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = LowspinTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodySmall,
                color = if (songCount == 0) LowspinTextMuted else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Pinned first entry in the Playlists tab. */
@Composable
fun LikedSongsCard(
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = LowspinTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Liked Songs",
                style = MaterialTheme.typography.titleSmall,
                color = LowspinTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Dashed-looking "create playlist" row that closes out the Playlists tab. */
@Composable
fun NewPlaylistCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "New playlist",
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LowspinTextPrimary
        )
        trailing?.invoke()
    }
}
