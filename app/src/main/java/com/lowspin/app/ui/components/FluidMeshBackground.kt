package com.lowspin.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Apple Music-style multi-color fluid radial animated gradient mesh.
 * Renders behind auto-scrolling synced lyrics with organic floating color blobs.
 */
@Composable
fun FluidMeshBackground(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val color1 = dominantColor.copy(alpha = 0.55f)
    val color2 = lerp(dominantColor, primary, 0.45f).copy(alpha = 0.50f)
    val color3 = secondary.copy(alpha = 0.45f)
    val color4 = lerp(tertiary, Color(0xFF6C5CE7), 0.35f).copy(alpha = 0.40f)

    if (reduceMotion) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(color1, color2, surfaceColor.copy(alpha = 0.95f))
                    )
                )
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "fluidMesh")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(17_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(23_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base background fill
            drawRect(color = surfaceColor.copy(alpha = 0.94f))

            // Blob 1: Top-Left to Center Orbit
            val center1X = w * (0.35f + 0.22f * cos(phase1))
            val center1Y = h * (0.30f + 0.18f * sin(phase1))
            val radius1 = (w + h) * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, color1.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(center1X, center1Y),
                    radius = radius1
                ),
                radius = radius1,
                center = Offset(center1X, center1Y)
            )

            // Blob 2: Bottom-Right to Center Orbit
            val center2X = w * (0.65f + 0.20f * sin(phase2))
            val center2Y = h * (0.65f + 0.22f * cos(phase2))
            val radius2 = (w + h) * 0.44f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, color2.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(center2X, center2Y),
                    radius = radius2
                ),
                radius = radius2,
                center = Offset(center2X, center2Y)
            )

            // Blob 3: Center-Left to Bottom-Left Orbit
            val center3X = w * (0.28f + 0.18f * cos(phase3 + 1.2f))
            val center3Y = h * (0.72f + 0.15f * sin(phase3 + 1.2f))
            val radius3 = (w + h) * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, color3.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(center3X, center3Y),
                    radius = radius3
                ),
                radius = radius3,
                center = Offset(center3X, center3Y)
            )

            // Blob 4: Top-Right Orbit
            val center4X = w * (0.75f + 0.15f * sin(phase1 + 2.5f))
            val center4Y = h * (0.22f + 0.16f * cos(phase2 + 1.8f))
            val radius4 = (w + h) * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color4, color4.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(center4X, center4Y),
                    radius = radius4
                ),
                radius = radius4,
                center = Offset(center4X, center4Y)
            )

            // Contrast enhancement scrim ensuring readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.65f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
        }
    }
}
