package com.lowspin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.ui.settings.LocalListStyle
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration

/**
 * The single list row used everywhere a song appears. Parameters are stable so
 * Compose skips unchanged rows while a long list scrolls. The glow is a static
 * radial gradient from a cached color — no blur, no per-frame work.
 */
@Composable
fun SongRow(
    song: SongEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    dragHandleModifier: Modifier? = null,
    showDuration: Boolean = true,
    showTrackNumber: Boolean = false
) {
    val style = LocalListStyle.current
    val glowColor = remember(song.id, song.dominantColorArgb) {
        if (song.dominantColorArgb != 0) {
            Color(song.dominantColorArgb)
        } else {
            Color(GradientPlaceholder.primaryColor(song.album.ifBlank { song.title }))
        }
    }
    val handleSize = if (style.largeTouchTargets) 48.dp else 44.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(style.rowHeight)
            .pressScaleClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTrackNumber && song.trackNumber > 0) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = song.trackNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else LowspinTextMuted
                )
            }
        }
        if (style.showArt) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    glowColor.copy(alpha = 0.32f),
                                    glowColor.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                AlbumArtImage(
                    songId = song.id,
                    seed = song.album.ifBlank { song.title },
                    modifier = Modifier.size(48.dp),
                    sizePx = 128
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else LowspinTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.bpm > 0 || (song.musicalKey.isNotBlank() && song.musicalKey != "Unknown")) {
                    Spacer(Modifier.width(6.dp))
                    val badge = buildString {
                        if (song.bpm > 0) append("${song.bpm} BPM")
                        if (song.bpm > 0 && song.musicalKey.isNotBlank() && song.musicalKey != "Unknown") append(" · ")
                        if (song.musicalKey.isNotBlank() && song.musicalKey != "Unknown") append(song.musicalKey)
                    }
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                        maxLines = 1,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        if (showDuration) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Song options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (dragHandleModifier != null) {
            Box(
                modifier = dragHandleModifier.size(handleSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Reorder",
                    tint = LowspinTextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
