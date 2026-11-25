package com.lowspin.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.ui.LocalAlbumArtRepository
import kotlin.math.PI
import kotlin.math.sin

/**
 * Photorealistic turntable vinyl component with:
 * - Dynamic album artwork in the center label
 * - Concentric physical vinyl micro-grooves & specular light reflections
 * - Animated tonearm / needle with resting & playing positions
 * - Pulsing ambient backlight halo
 */
@Composable
fun TurntableVinyl(
    songId: Long,
    albumSeed: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    dominantColor: Color = MaterialTheme.colorScheme.primary,
    reduceMotion: Boolean = false
) {
    val repository = LocalAlbumArtRepository.current
    val bitmap by produceState<Bitmap?>(initialValue = null, songId) {
        value = repository.loadBitmap(songId, 500)
    }

    // Continuous 33.3 RPM rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpinTransition")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylSpinAngle"
    )

    // Needle arm angle: 0deg resting, 26deg on record
    val needleAngle by animateFloatAsState(
        targetValue = if (isPlaying) 26f else 0f,
        animationSpec = if (reduceMotion) tween(durationMillis = 0) else tween(durationMillis = 600),
        label = "tonearmAngle"
    )

    // Ambient glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val currentRotation = if (isPlaying && !reduceMotion) spinAngle else 0f
    val needleJitter = if (isPlaying && !reduceMotion) {
        0.35f * sin((spinAngle * 4f * PI.toFloat() / 180f))
    } else 0f
    val accent = dominantColor

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient backlight halo
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    alpha = if (isPlaying) glowPulse else 0.4f
                    scaleX = if (isPlaying) 1.04f else 1.0f
                    scaleY = if (isPlaying) 1.04f else 1.0f
                }
                .shadow(
                    elevation = 32.dp,
                    shape = CircleShape,
                    spotColor = accent,
                    ambientColor = accent
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                        radius = 450f
                    ),
                    shape = CircleShape
                )
        )

        // 2. Vinyl disc container
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .clip(CircleShape)
                .graphicsLayer { rotationZ = currentRotation },
            contentAlignment = Alignment.Center
        ) {
            // Vinyl surface grooves and sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer edge ring
                drawCircle(
                    color = Color(0xFF141418),
                    radius = r,
                    center = center
                )

                // Concentric micro-grooves
                val grooveCount = 18
                for (g in 1..grooveCount) {
                    val gr = r * (0.42f + (g.toFloat() / grooveCount) * 0.54f)
                    drawCircle(
                        color = Color.White.copy(alpha = if (g % 3 == 0) 0.08f else 0.04f),
                        radius = gr,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Run-out groove transition ring with golden sheen
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = 0.25f),
                    radius = r * 0.435f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Vinyl specular light sheen wedges (giving 3D vinyl reflections)
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = r * 0.98f,
                    center = center
                )
            }

            // Center Label (Album Art or Placeholder)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.42f)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1C28)),
                contentAlignment = Alignment.Center
            ) {
                val art = bitmap
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    GradientPlaceholderBox(
                        seed = albumSeed,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Center spindle hole and metallic ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0xFFD6D6DF),
                        radius = 16.dp.toPx(),
                        center = c,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF0A0A0E),
                        radius = 8.dp.toPx(),
                        center = c
                    )
                }
            }
        }

        // 3. Stylus Tonearm (anchored in top-right corner)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, end = 4.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Canvas(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        // Pivot around top-right base
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.15f)
                        rotationZ = needleAngle + needleJitter
                    }
            ) {
                val base = Offset(size.width * 0.85f, size.height * 0.15f)
                val elbow = Offset(size.width * 0.45f, size.height * 0.55f)
                val head = Offset(size.width * 0.20f, size.height * 0.88f)

                // Tonearm pivot base
                drawCircle(color = Color(0xFF32323E), radius = 14.dp.toPx(), center = base)
                drawCircle(color = Color(0xFF7A7A8C), radius = 8.dp.toPx(), center = base)

                // Arm bar
                drawLine(
                    color = Color(0xFFC4C4D4),
                    start = base,
                    end = elbow,
                    strokeWidth = 3.5.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFB0B0C4),
                    start = elbow,
                    end = head,
                    strokeWidth = 3.dp.toPx()
                )

                // Cartridge / Needle head
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(head.x - 6.dp.toPx(), head.y - 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 20.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}
