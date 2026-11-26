package com.lowspin.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

val PlaylistGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)), // Sunset Fire
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), // Ocean Blue
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // Emerald Mint
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)), // Neon Synth
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Deep Purple
    listOf(Color(0xFFF953C6), Color(0xFFB91D73)), // Rose Berry
    listOf(Color(0xFFF12711), Color(0xFFF5AF19)), // Golden Flare
    listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))  // Midnight Steel
)

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
fun PlaylistCoverArt(
    songIds: List<Long>,
    seed: String,
    size: Dp,
    customCoverUri: String? = null,
    gradientColorIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (!customCoverUri.isNullOrBlank()) {
            val context = LocalContext.current
            val customBitmap by produceState<Bitmap?>(initialValue = null, customCoverUri) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val uri = Uri.parse(customCoverUri)
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                        var inSampleSize = 1
                        val reqDim = 512
                        if (options.outHeight > reqDim || options.outWidth > reqDim) {
                            val halfHeight = options.outHeight / 2
                            val halfWidth = options.outWidth / 2
                            while ((halfHeight / inSampleSize) >= reqDim && (halfWidth / inSampleSize) >= reqDim) {
                                inSampleSize *= 2
                            }
                        }
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, decodeOptions)
                        }
                    }.getOrNull()
                }
            }
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GradientPlaceholderBox(seed = seed, modifier = Modifier.fillMaxSize())
            }
        } else if (gradientColorIndex != null && gradientColorIndex in PlaylistGradients.indices) {
            val colors = PlaylistGradients[gradientColorIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center
            ) {
                VinylGlyph(modifier = Modifier.fillMaxSize(0.62f))
            }
        } else {
            CollageArt(songIds = songIds, seed = seed, size = size)
        }
    }
}

@Composable
fun PlaylistCard(
    name: String,
    songCount: Int,
    artSongId: Long?,
    seed: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customCoverUri: String? = null,
    gradientColorIndex: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!customCoverUri.isNullOrBlank() || gradientColorIndex != null) {
            PlaylistCoverArt(
                songIds = artSongId?.let { listOf(it) } ?: emptyList(),
                seed = seed,
                size = 56.dp,
                customCoverUri = customCoverUri,
                gradientColorIndex = gradientColorIndex
            )
        } else if (artSongId != null) {
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
