package com.lowspin.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Heart button that explodes a colorful burst of 14 particles radially
 * when toggled from unliked -> liked.
 */
@Composable
fun LikeParticleButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val particleColors = remember(primary, secondary) {
        listOf(primary, secondary, Color(0xFFFF4081), Color(0xFFFFD54F), Color(0xFF00F0FF))
    }

    var previousLiked by remember { mutableStateOf(isLiked) }
    var burstId by remember { mutableIntStateOf(0) }

    val burstProgress = remember { Animatable(0f) }
    val iconScale = remember { Animatable(1f) }

    LaunchedEffect(isLiked) {
        if (isLiked && !previousLiked && !reduceMotion) {
            burstId++
        }
        previousLiked = isLiked
    }

    LaunchedEffect(burstId) {
        if (burstId > 0) {
            burstProgress.snapTo(0f)
            iconScale.snapTo(0.75f)
            iconScale.animateTo(
                1.28f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            iconScale.animateTo(1f, animationSpec = tween(180))
        }
    }

    LaunchedEffect(burstId) {
        if (burstId > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, animationSpec = tween(durationMillis = 550))
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (burstProgress.value > 0f && burstProgress.value < 1f) {
            val t = burstProgress.value
            Canvas(modifier = Modifier.size(58.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension / 2f
                val count = 14
                val alpha = (1f - t).coerceIn(0f, 1f)

                for (i in 0 until count) {
                    val angle = (2.0 * PI * i / count) + (i % 2) * 0.14
                    val speedFactor = if (i % 2 == 0) 1.0f else 0.72f
                    val dist = maxR * (0.25f + 0.75f * t) * speedFactor
                    val px = cx + (cos(angle) * dist).toFloat()
                    val py = cy + (sin(angle) * dist).toFloat() + (t * t * 6.dp.toPx())
                    val r = (if (i % 2 == 0) 3.2.dp else 2.2.dp).toPx() * (1f - t * 0.65f)
                    drawCircle(
                        color = particleColors[i % particleColors.size].copy(alpha = alpha),
                        radius = r,
                        center = Offset(px, py)
                    )
                }
            }
        }

        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isLiked) "Remove from Liked Songs" else "Like song",
                tint = if (isLiked) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
            )
        }
    }
}
