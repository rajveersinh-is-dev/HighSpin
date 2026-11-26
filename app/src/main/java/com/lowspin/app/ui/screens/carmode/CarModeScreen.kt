package com.lowspin.app.ui.screens.carmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.components.AlbumArtImage
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextPrimary

/**
 * High-contrast, distraction-free portrait and landscape driving mode
 * with giant touch targets and rapid controls.
 */
@Composable
fun CarModeScreen(
    state: PlayerUiState,
    isLiked: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekRelative: (deltaMs: Long) -> Unit,
    onToggleLike: () -> Unit,
    onExit: () -> Unit
) {
    val song = state.currentSong
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // Landscape Driving Mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Exit + Art + Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onExit, modifier = Modifier.size(52.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Exit Car Mode",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "CAR MODE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (song != null) {
                        AlbumArtImage(
                            songId = song.id,
                            seed = song.album.ifBlank { song.title },
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            sizePx = 400
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFCCCCCC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Right Column: Giant Controls
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Giant Primary Transport Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(76.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = LowspinOnAccent
                            )
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(76.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Secondary Quick Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSeekRelative(-15_000L) }, modifier = Modifier.size(60.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 15 seconds",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        IconButton(onClick = onToggleLike, modifier = Modifier.size(60.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like Song",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else LowspinTextPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        IconButton(onClick = { onSeekRelative(30_000L) }, modifier = Modifier.size(60.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Forward30,
                                contentDescription = "Skip 30 seconds",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Driving Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Exit Car Mode",
                            tint = LowspinTextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CAR MODE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Center Large Artwork
                if (song != null) {
                    AlbumArtImage(
                        songId = song.id,
                        seed = song.album.ifBlank { song.title },
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        sizePx = 600
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFCCCCCC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Giant Transport Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(80.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = LowspinOnAccent
                            )
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(80.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Secondary Rapid Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSeekRelative(-15_000L) }, modifier = Modifier.size(64.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 15 seconds",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        IconButton(onClick = onToggleLike, modifier = Modifier.size(64.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like Song",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else LowspinTextPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        IconButton(onClick = { onSeekRelative(30_000L) }, modifier = Modifier.size(64.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Forward30,
                                contentDescription = "Skip 30 seconds",
                                tint = LowspinTextPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
