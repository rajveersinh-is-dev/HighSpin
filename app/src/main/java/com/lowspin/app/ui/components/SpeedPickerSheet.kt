package com.lowspin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import java.util.Locale
import kotlin.math.abs

val AVAILABLE_SPEEDS = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)

data class AudioModPreset(
    val name: String,
    val speed: Float,
    val pitch: Float
)

val AUDIO_MOD_PRESETS = listOf(
    AudioModPreset("Normal", 1.0f, 1.0f),
    AudioModPreset("Nightcore", 1.15f, 1.15f),
    AudioModPreset("Slowed & Reverb", 0.85f, 0.88f),
    AudioModPreset("Sped Up", 1.25f, 1.0f),
    AudioModPreset("Deep Pitch", 1.0f, 0.82f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedPickerSheet(
    currentSpeed: Float,
    currentPitch: Float = 1f,
    onSpeedSelected: (Float) -> Unit,
    onPitchSelected: (Float) -> Unit = {},
    onSpeedAndPitchSelected: ((Float, Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var speedVal by remember { mutableFloatStateOf(currentSpeed) }
    var pitchVal by remember { mutableFloatStateOf(currentPitch) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tempo & Pitch DSP",
                    style = MaterialTheme.typography.titleLarge,
                    color = LowspinTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        speedVal = 1f
                        pitchVal = 1f
                        if (onSpeedAndPitchSelected != null) {
                            onSpeedAndPitchSelected(1f, 1f)
                        } else {
                            onSpeedSelected(1f)
                            onPitchSelected(1f)
                        }
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Quick Audio Mod Presets
            Text(
                text = "DSP PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AUDIO_MOD_PRESETS.take(3).forEach { preset ->
                    val active = abs(preset.speed - speedVal) < 0.03f && abs(preset.pitch - pitchVal) < 0.03f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                speedVal = preset.speed
                                pitchVal = preset.pitch
                                if (onSpeedAndPitchSelected != null) {
                                    onSpeedAndPitchSelected(preset.speed, preset.pitch)
                                } else {
                                    onSpeedSelected(preset.speed)
                                    onPitchSelected(preset.pitch)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) LowspinOnAccent else LowspinTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AUDIO_MOD_PRESETS.drop(3).forEach { preset ->
                    val active = abs(preset.speed - speedVal) < 0.03f && abs(preset.pitch - pitchVal) < 0.03f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                speedVal = preset.speed
                                pitchVal = preset.pitch
                                if (onSpeedAndPitchSelected != null) {
                                    onSpeedAndPitchSelected(preset.speed, preset.pitch)
                                } else {
                                    onSpeedSelected(preset.speed)
                                    onPitchSelected(preset.pitch)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) LowspinOnAccent else LowspinTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Playback Speed Slider & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Speed",
                    style = MaterialTheme.typography.titleSmall,
                    color = LowspinTextPrimary
                )
                Text(
                    text = String.format(Locale.US, "%.2fx", speedVal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = speedVal,
                onValueChange = {
                    speedVal = it
                    onSpeedSelected(it)
                },
                valueRange = 0.5f..2.5f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(12.dp))

            // Audio Pitch Slider & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Pitch",
                    style = MaterialTheme.typography.titleSmall,
                    color = LowspinTextPrimary
                )
                Text(
                    text = String.format(Locale.US, "%.2fx", pitchVal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = pitchVal,
                onValueChange = {
                    pitchVal = it
                    onPitchSelected(it)
                },
                valueRange = 0.5f..1.8f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(14.dp))

            // Quick Speed Grid
            Text(
                text = "SPEED PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            val chunked = AVAILABLE_SPEEDS.chunked(5)
            for (row in chunked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (speed in row) {
                        val selected = abs(speed - speedVal) < 0.04f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    speedVal = speed
                                    onSpeedSelected(speed)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) LowspinOnAccent else LowspinTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
