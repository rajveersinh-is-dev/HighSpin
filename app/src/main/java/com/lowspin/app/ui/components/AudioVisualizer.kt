package com.lowspin.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, hardware-accelerated animated audio visualizer.
 * Supports 5 aesthetic styles:
 * - 0: Dynamic Equalizer Spectrum Bars with floating peak caps
 * - 1: Glowing Multi-Harmonic Neon Waves
 * - 2: Minimalist Dancing Rhythm Dots
 * - 3: Liquid Aura / Harmonic Flow with volumetric fluid bloom
 * - 4: Orbital Pulsar / Concentric Harmonic Frequency Halo
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 26,
    style: Int = 0,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 48.dp,
    reduceMotion: Boolean = false
) {
    val playFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = if (reduceMotion) tween(durationMillis = 0) else tween(durationMillis = 400),
        label = "playFactor"
    )

    val transition = rememberInfiniteTransition(label = "visualizerPhase")
    val animPhase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val animPhase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase1 = if (reduceMotion) 1f else animPhase1
    val phase2 = if (reduceMotion) 1.5f else animPhase2

    // Precalculated frequency multipliers to give realistic equalizer spectrum shaping
    // (higher bass on left, dynamic mids, taper at high frequencies)
    val spectrumShape = remember(barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // Bass bump around 0.15 - 0.35, gentle treble slope
            0.65f + 0.35f * sin((norm * PI).toFloat())
        }
    }

    val primary = accentColor
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height

        when (style) {
            1 -> {
                // Style 1: Glowing Neon Waves
                drawNeonWaves(
                    width = width,
                    canvasHeight = canvasHeight,
                    playFactor = playFactor,
                    phase1 = phase1,
                    phase2 = phase2,
                    primary = primary,
                    secondary = secondary
                )
            }
            2 -> {
                // Style 2: Rhythm Dots
                drawPulseDots(
                    width = width,
                    canvasHeight = canvasHeight,
                    barCount = barCount,
                    playFactor = playFactor,
                    phase1 = phase1,
                    spectrumShape = spectrumShape,
                    primary = primary
                )
            }
            3 -> {
                // Style 3: Liquid Aura / Harmonic Flow
                drawLiquidAura(
                    width = width,
                    canvasHeight = canvasHeight,
                    playFactor = playFactor,
                    phase1 = phase1,
                    phase2 = phase2,
                    primary = primary,
                    secondary = secondary
                )
            }
            4 -> {
                // Style 4: Orbital Pulsar / Harmonic Frequency Halo
                drawOrbitalPulsar(
                    width = width,
                    canvasHeight = canvasHeight,
                    playFactor = playFactor,
                    phase1 = phase1,
                    phase2 = phase2,
                    primary = primary,
                    secondary = secondary
                )
            }
            else -> {
                // Style 0 (Default): Spectrum Equalizer Bars
                drawSpectrumBars(
                    width = width,
                    canvasHeight = canvasHeight,
                    barCount = barCount,
                    playFactor = playFactor,
                    phase1 = phase1,
                    phase2 = phase2,
                    spectrumShape = spectrumShape,
                    primary = primary,
                    secondary = secondary
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrumBars(
    width: Float,
    canvasHeight: Float,
    barCount: Int,
    playFactor: Float,
    phase1: Float,
    phase2: Float,
    spectrumShape: FloatArray,
    primary: Color,
    secondary: Color
) {
    val totalGaps = (barCount - 1).coerceAtLeast(1)
    val gapWidth = (width / barCount) * 0.28f
    val barWidth = ((width - (totalGaps * gapWidth)) / barCount).coerceAtLeast(2f)
    val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

    for (i in 0 until barCount) {
        val shape = spectrumShape[i]
        val harmonic = abs(
            0.5f * sin(phase1 + i * 0.42f) +
                0.35f * cos(phase2 + i * 0.65f) +
                0.25f * sin(phase1 * 1.5f + i * 0.28f)
        ).coerceIn(0.08f, 1f)

        val heightFraction = (harmonic * shape * playFactor).coerceIn(0.05f, 1f)
        val barH = canvasHeight * heightFraction * 0.92f
        val left = i * (barWidth + gapWidth)
        val top = canvasHeight - barH

        // Draw bar
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(primary, secondary.copy(alpha = 0.85f)),
                startY = top,
                endY = canvasHeight
            ),
            topLeft = Offset(left, top),
            size = Size(barWidth, barH),
            cornerRadius = corner
        )

        // Draw peak cap if playing
        if (playFactor > 0.4f) {
            val peakTop = (top - 4.dp.toPx()).coerceAtLeast(0f)
            drawLine(
                color = primary.copy(alpha = 0.9f),
                start = Offset(left, peakTop),
                end = Offset(left + barWidth, peakTop),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonWaves(
    width: Float,
    canvasHeight: Float,
    playFactor: Float,
    phase1: Float,
    phase2: Float,
    primary: Color,
    secondary: Color
) {
    val centerY = canvasHeight / 2f
    val amplitude = (canvasHeight * 0.42f) * playFactor

    val path1 = Path()
    val path2 = Path()

    val steps = 40
    val stepX = width / steps

    for (step in 0..steps) {
        val x = step * stepX
        val norm = step.toFloat() / steps
        val envelope = sin((norm * PI).toFloat())

        val y1 = centerY + sin(phase1 + norm * 3.5f * PI.toFloat()) * amplitude * envelope
        val y2 = centerY + cos(phase2 + norm * 2.8f * PI.toFloat()) * amplitude * 0.75f * envelope

        if (step == 0) {
            path1.moveTo(x, y1)
            path2.moveTo(x, y2)
        } else {
            path1.lineTo(x, y1)
            path2.lineTo(x, y2)
        }
    }

    // Secondary subtle wave
    drawPath(
        path = path2,
        color = secondary.copy(alpha = 0.45f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Primary glowing wave
    drawPath(
        path = path1,
        color = primary,
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPulseDots(
    width: Float,
    canvasHeight: Float,
    barCount: Int,
    playFactor: Float,
    phase1: Float,
    spectrumShape: FloatArray,
    primary: Color
) {
    val stepX = width / barCount.coerceAtLeast(1)
    val centerY = canvasHeight / 2f
    val maxRadius = (stepX * 0.35f).coerceIn(3.dp.toPx(), 8.dp.toPx())

    for (i in 0 until barCount) {
        val shape = spectrumShape[i]
        val offsetFactor = sin(phase1 + i * 0.5f) * playFactor
        val cx = i * stepX + stepX / 2f
        val cy = centerY + offsetFactor * (canvasHeight * 0.35f) * shape
        val r = (maxRadius * (0.4f + 0.6f * abs(offsetFactor))).coerceAtLeast(2.dp.toPx())

        drawCircle(
            color = primary.copy(alpha = (0.35f + 0.65f * abs(offsetFactor)).coerceIn(0.2f, 1f)),
            radius = r,
            center = Offset(cx, cy)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiquidAura(
    width: Float,
    canvasHeight: Float,
    playFactor: Float,
    phase1: Float,
    phase2: Float,
    primary: Color,
    secondary: Color
) {
    val centerY = canvasHeight * 0.52f
    val amplitude = (canvasHeight * 0.38f) * playFactor

    val path = Path()
    val fillPath = Path()
    fillPath.moveTo(0f, canvasHeight)

    val steps = 44
    val stepX = width / steps

    for (step in 0..steps) {
        val x = step * stepX
        val norm = step.toFloat() / steps
        val envelope = sin((norm * PI).toFloat())
        val wave = sin(phase1 + norm * 2.8f * PI.toFloat()) * 0.65f +
            cos(phase2 + norm * 3.8f * PI.toFloat()) * 0.35f
        val y = centerY + wave * amplitude * envelope

        if (step == 0) {
            path.moveTo(x, y)
            fillPath.lineTo(x, y)
        } else {
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }

    fillPath.lineTo(width, canvasHeight)
    fillPath.close()

    // Volumetric luminous under-glow
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                primary.copy(alpha = 0.28f * playFactor),
                secondary.copy(alpha = 0.08f * playFactor),
                Color.Transparent
            ),
            startY = centerY - amplitude,
            endY = canvasHeight
        )
    )

    // Vibrant surface wave crest
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(
            colors = listOf(primary, secondary, primary)
        ),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbitalPulsar(
    width: Float,
    canvasHeight: Float,
    playFactor: Float,
    phase1: Float,
    phase2: Float,
    primary: Color,
    secondary: Color
) {
    val centerX = width / 2f
    val centerY = canvasHeight / 2f
    val maxRadius = (canvasHeight * 0.44f) * playFactor

    // 1. Concentric orbital pulsing rings
    val ringCount = 3
    for (r in 1..ringCount) {
        val norm = r.toFloat() / ringCount
        val pulse = sin(phase1 + norm * PI.toFloat()) * 0.25f
        val radius = (maxRadius * norm * (1f + pulse * playFactor)).coerceAtLeast(2.dp.toPx())
        val alpha = ((1f - norm * 0.45f) * playFactor * (0.6f + 0.4f * pulse)).coerceIn(0.1f, 1f)
        val strokeW = (2.2.dp.toPx() * (1f - norm * 0.3f)).coerceAtLeast(1.dp.toPx())

        drawCircle(
            color = if (r % 2 == 1) primary.copy(alpha = alpha) else secondary.copy(alpha = alpha),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeW)
        )
    }

    // 2. Mirrored frequency pulsar spikes radiating horizontally
    val rayCount = 16
    val halfWidth = (width / 2f) - maxRadius - 12.dp.toPx()
    if (halfWidth > 20f) {
        val rayStep = halfWidth / rayCount
        for (i in 0 until rayCount) {
            val norm = i.toFloat() / rayCount
            val harmonic = abs(sin(phase2 + norm * 3f * PI.toFloat()) * cos(phase1 + norm * 2f))
            val rayH = (canvasHeight * 0.36f * harmonic * playFactor).coerceAtLeast(2.dp.toPx())
            val strokeW = (rayStep * 0.55f).coerceIn(2.dp.toPx(), 4.dp.toPx())
            val brush = Brush.verticalGradient(
                colors = listOf(primary, secondary),
                startY = centerY - rayH,
                endY = centerY + rayH
            )

            // Right side ray
            val rx = centerX + maxRadius + 8.dp.toPx() + i * rayStep
            drawLine(
                brush = brush,
                start = Offset(rx, centerY - rayH),
                end = Offset(rx, centerY + rayH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Left side mirrored ray
            val lx = centerX - maxRadius - 8.dp.toPx() - i * rayStep
            drawLine(
                brush = brush,
                start = Offset(lx, centerY - rayH),
                end = Offset(lx, centerY + rayH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Central glowing star core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.85f * playFactor),
                secondary.copy(alpha = 0.35f * playFactor),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = (maxRadius * 0.5f).coerceAtLeast(4.dp.toPx())
        ),
        radius = (maxRadius * 0.5f).coerceAtLeast(4.dp.toPx()),
        center = Offset(centerX, centerY)
    )
}
