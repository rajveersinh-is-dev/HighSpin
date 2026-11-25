package com.lowspin.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The Lowspin vinyl: violet disc, two faint grooves, dark label and a small
 * violet hole. Rotates at 3 seconds per turn when [rotating] is true.
 */
@Composable
fun VinylGlyph(
    modifier: Modifier = Modifier,
    rotating: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylSpin"
    )

    val accent = MaterialTheme.colorScheme.primary
    val label = MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = if (rotating) angle else 0f
        }
    ) {
        val size = minOf(this.size.width, this.size.height)
        val center = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = size / 2f

        drawCircle(color = accent, radius = radius, center = center)
        drawCircle(
            color = label.copy(alpha = 0.3f),
            radius = radius * 0.80f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = label.copy(alpha = 0.3f),
            radius = radius * 0.63f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawCircle(color = label, radius = radius * 0.34f, center = center)
        drawCircle(color = accent, radius = radius * 0.085f, center = center)
    }
}
