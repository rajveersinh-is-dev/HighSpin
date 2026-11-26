package com.lowspin.app.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.media.PlayerUiState
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.theme.LowspinTextPrimary

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    isLiked: Boolean,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val song = state.currentSong ?: return
    val appearance = LocalAppearance.current
    val glassEnabled by appearance.glassMiniPlayer.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val tint = remember(state.dominantColor, song.id, surfaceVariant, glassEnabled) {
        val base = state.dominantColor
            .takeIf { it != 0 }
            ?.let { Color(it) }
            ?: Color(GradientPlaceholder.primaryColor(song.album.ifBlank { song.title }))
        val mixed = lerp(base, surfaceVariant, 0.55f)
        if (glassEnabled) mixed.copy(alpha = 0.72f) else mixed
    }
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var dragX by remember { mutableFloatStateOf(0f) }

    val glassEffectModifier = remember(glassEnabled) {
        if (glassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
        } else {
            Modifier
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Frosted inner glass glow backing
        if (glassEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = RenderEffect
                                    .createBlurEffect(26f, 26f, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        } else Modifier
                    )
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                tint.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                tint.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = glassEffectModifier
                .fillMaxWidth()
                .background(
                    if (glassEnabled) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.12f),
                                tint,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(tint, tint))
                    }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (glassEnabled) 0.26f else 0.12f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX < -120f) onNext() else if (dragX > 120f) onPrevious()
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                        onHorizontalDrag = { _, delta -> dragX += delta }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .pressScaleClickable(onClick = onExpand)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val artModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier
                            .size(42.dp)
                            .sharedElement(
                                state = rememberSharedContentState(key = "album-art-${song.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    }
                } else {
                    Modifier.size(42.dp)
                }

                AlbumArtImage(
                    songId = song.id,
                    seed = song.album.ifBlank { song.title },
                    modifier = artModifier,
                    sizePx = 128,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isPlaying) {
                            VinylGlyph(
                                modifier = Modifier.size(13.dp),
                                rotating = true
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LowspinTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LikeParticleButton(
                    isLiked = isLiked,
                    onToggle = onToggleLike,
                    reduceMotion = reduceMotion
                )
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = LowspinTextPrimary
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }
    }
}
