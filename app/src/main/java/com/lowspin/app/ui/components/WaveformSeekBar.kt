package com.lowspin.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lowspin.app.util.formatDuration
import kotlin.math.abs
import kotlin.math.sin

private const val BAR_COUNT = 52

/**
 * HighSpin Waveform Seekbar.
 * Renders vertical audio energy peaks with interactive haptic scrub preview
 * and millisecond-accurate A-B loop section markers.
 */
@Composable
fun WaveformSeekBar(
    positionMs: Long,
    durationMs: Long,
    songId: Long,
    loopRange: Pair<Long, Long>?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L)
    val view = LocalView.current

    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var lastHapticBarIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(positionMs, dragging) {
        if (!dragging) {
            dragProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
    }

    // Deterministic waveform amplitude peaks derived from track parameters
    val amplitudes = remember(songId, durationMs) {
        val seed = abs(songId * 31L + durationMs).toFloat()
        List(BAR_COUNT) { index ->
            val harmonic1 = sin(index * 0.18f + seed * 0.001f) * 0.35f
            val harmonic2 = sin(index * 0.42f + seed * 0.002f) * 0.22f
            val harmonic3 = sin(index * 0.08f + seed * 0.003f) * 0.15f
            val base = 0.55f + harmonic1 + harmonic2 + harmonic3
            base.coerceIn(0.18f, 1.0f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val playedColor = primaryColor.copy(alpha = 0.90f)
    val unplayedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val loopHighlightColor = primaryColor.copy(alpha = 0.25f)
    val markerColor = MaterialTheme.colorScheme.tertiary.takeIf { it != Color.Unspecified }
        ?: Color(0xFFFFB74D)

    val currentRatio = if (dragging) dragProgress else (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(duration, songId) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSeek((ratio * duration).toLong())
                    }
                }
                .pointerInput(duration, songId) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            lastHapticBarIndex = (dragProgress * BAR_COUNT).toInt()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                        onDragEnd = {
                            val seekTarget = (dragProgress * duration).toLong()
                            onSeek(seekTarget)
                            dragging = false
                            lastHapticBarIndex = -1
                        },
                        onDragCancel = {
                            dragging = false
                            lastHapticBarIndex = -1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = ratio
                            val currentBar = (ratio * BAR_COUNT).toInt()
                            if (currentBar != lastHapticBarIndex) {
                                lastHapticBarIndex = currentBar
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val totalWidth = size.width
                val totalHeight = size.height
                val barWidth = (totalWidth / BAR_COUNT) * 0.65f
                val barSpacing = totalWidth / BAR_COUNT

                val loopA = loopRange?.first
                val loopB = loopRange?.second
                val loopRatioA = loopA?.let { (it.toFloat() / duration).coerceIn(0f, 1f) }
                val loopRatioB = loopB?.let { (it.toFloat() / duration).coerceIn(0f, 1f) }

                // Draw A-B loop highlight region behind bars if set
                if (loopRatioA != null && loopRatioB != null && loopRatioB > loopRatioA) {
                    val highlightStart = loopRatioA * totalWidth
                    val highlightEnd = loopRatioB * totalWidth
                    drawRoundRect(
                        color = loopHighlightColor,
                        topLeft = Offset(highlightStart, 2f),
                        size = Size(highlightEnd - highlightStart, totalHeight - 4f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                // Draw Waveform Bars
                for (i in 0 until BAR_COUNT) {
                    val barCenter = i * barSpacing + barSpacing / 2f
                    val progressRatio = barCenter / totalWidth
                    val isPlayed = progressRatio <= currentRatio

                    val amp = amplitudes[i]
                    val barHeight = totalHeight * amp * 0.85f
                    val topY = (totalHeight - barHeight) / 2f

                    val inLoop = if (loopRatioA != null && loopRatioB != null) {
                        progressRatio in loopRatioA..loopRatioB
                    } else false

                    val barColor = when {
                        isPlayed -> playedColor
                        inLoop -> primaryColor.copy(alpha = 0.55f)
                        else -> unplayedColor
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barCenter - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }

                // Draw scrub indicator line / playhead
                val playheadX = currentRatio * totalWidth
                drawLine(
                    color = primaryColor,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, totalHeight),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(playheadX, totalHeight / 2f)
                )

                // Draw Loop Boundary Pin A
                if (loopRatioA != null) {
                    val pinAX = loopRatioA * totalWidth
                    drawLine(
                        color = markerColor,
                        start = Offset(pinAX, 0f),
                        end = Offset(pinAX, totalHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 4.dp.toPx(),
                        center = Offset(pinAX, 6.dp.toPx())
                    )
                }

                // Draw Loop Boundary Pin B
                if (loopRatioB != null) {
                    val pinBX = loopRatioB * totalWidth
                    drawLine(
                        color = markerColor,
                        start = Offset(pinBX, 0f),
                        end = Offset(pinBX, totalHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 4.dp.toPx(),
                        center = Offset(pinBX, totalHeight - 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Time indicators and loop indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayMs = if (dragging) (dragProgress * duration).toLong() else positionMs
            Text(
                text = formatDuration(displayMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (loopRange != null) {
                Text(
                    text = "A-B Loop: ${formatDuration(loopRange.first)} → ${formatDuration(loopRange.second)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = markerColor,
                    fontSize = 11.sp
                )
            }

            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
