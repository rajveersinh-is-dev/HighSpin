package com.lowspin.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lowspin.app.ui.theme.LowspinTextPrimary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tactile circular rotary knob for quick Bass Boost adjustment (0..1000 mB).
 */
@Composable
fun BassBoostDial(
    strength: Int,
    onStrengthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp
) {
    val view = LocalView.current
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val normalized = (strength.coerceIn(0, 1000) / 1000f)
    var lastHapticBucket by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        // Map angle starting at bottom-left (135 deg) clockwise 270 deg to bottom-right (45 deg)
                        var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                        if (deg < 0f) deg += 360f
                        val fromStart = (deg - 135f + 360f) % 360f
                        val clampedSweep = if (fromStart > 270f) {
                            if (fromStart > 315f) 0f else 270f
                        } else fromStart
                        val newStrength = ((clampedSweep / 270f) * 1000f).roundToInt().coerceIn(0, 1000)
                        val bucket = newStrength / 50
                        if (bucket != lastHapticBucket) {
                            lastHapticBucket = bucket
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onStrengthChange(newStrength)
                    }
                }
        ) {
            val strokeW = 10.dp.toPx()
            val inset = strokeW * 1.4f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // Background sweep track (270 degrees starting at 135 deg)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Active bass arc
            if (normalized > 0.005f) {
                drawArc(
                    color = primary,
                    startAngle = 135f,
                    sweepAngle = 270f * normalized,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Inner metallic dial face
            val knobR = arcSize.width * 0.36f
            drawCircle(
                color = surfaceColor,
                radius = knobR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = primary.copy(alpha = 0.35f),
                radius = knobR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Pointer dot on dial face
            val angleRad = Math.toRadians((135f + 270f * normalized).toDouble())
            val dotDist = knobR * 0.68f
            drawCircle(
                color = if (normalized > 0f) primary else Color.Gray,
                radius = 4.5.dp.toPx(),
                center = Offset(
                    cx + (cos(angleRad) * dotDist).toFloat(),
                    cy + (sin(angleRad) * dotDist).toFloat()
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${strength / 10}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LowspinTextPrimary
            )
            Text(
                text = "BASS",
                style = MaterialTheme.typography.labelSmall,
                color = primary
            )
        }
    }
}
