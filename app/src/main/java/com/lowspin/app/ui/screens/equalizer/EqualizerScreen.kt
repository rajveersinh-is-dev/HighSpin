package com.lowspin.app.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.BandInfo
import com.lowspin.app.media.EqualizerPresets
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinSurfaceElevated
import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun EqualizerScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val equalizerSettings = container.equalizerSettings
    val equalizerManager = container.equalizerManager

    val enabled by equalizerSettings.enabled.collectAsStateWithLifecycle()
    val activePreset by equalizerSettings.preset.collectAsStateWithLifecycle()
    val bassBoost by equalizerSettings.bassBoost.collectAsStateWithLifecycle()
    val virtualizer by equalizerSettings.virtualizer.collectAsStateWithLifecycle()
    val customBandLevels by equalizerSettings.customBandLevels.collectAsStateWithLifecycle()

    var bands by remember { mutableStateOf(equalizerManager.getBands()) }

    LaunchedEffect(enabled, activePreset) {
        bands = equalizerManager.getBands()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = { equalizerSettings.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LowspinOnAccent,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(EqualizerPresets) { index, name ->
                    val selected = activePreset == index && enabled
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(enabled = enabled) {
                                equalizerSettings.setPreset(index)
                                equalizerManager.applyEqualizer(true, index)
                                bands = equalizerManager.getBands()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) LowspinOnAccent else if (enabled) LowspinTextPrimary else LowspinTextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Frequency Bands",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val displayBands = if (bands.isNotEmpty()) bands else defaultFallbackBands()
                    for (band in displayBands) {
                        BandSliderRow(
                            band = band,
                            enabled = enabled,
                            levelMb = customBandLevels[band.index] ?: band.currentLevelMb,
                            onLevelChange = { newLevel ->
                                equalizerManager.setBandLevel(band.index, newLevel)
                                bands = equalizerManager.getBands()
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Audio Effects",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (enabled) LowspinTextPrimary else LowspinTextMuted
                        )
                        Text(
                            text = "${(bassBoost / 10)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = bassBoost.toFloat(),
                        onValueChange = {
                            equalizerSettings.setBassBoost(it.toInt())
                            equalizerManager.applyBassBoost(enabled, it.toInt())
                        },
                        valueRange = 0f..1000f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = LowspinSurfaceElevated
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Virtualizer (3D Audio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D Surround / Virtualizer",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (enabled) LowspinTextPrimary else LowspinTextMuted
                        )
                        Text(
                            text = "${(virtualizer / 10)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = virtualizer.toFloat(),
                        onValueChange = {
                            equalizerSettings.setVirtualizer(it.toInt())
                            equalizerManager.applyVirtualizer(enabled, it.toInt())
                        },
                        valueRange = 0f..1000f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = LowspinSurfaceElevated
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BandSliderRow(
    band: BandInfo,
    enabled: Boolean,
    levelMb: Int,
    onLevelChange: (Int) -> Unit
) {
    val freqLabel = formatFreq(band.centerFreqHz)
    val dbVal = levelMb / 100f
    val dbLabel = if (dbVal > 0) "+%.1f dB".format(dbVal) else "%.1f dB".format(dbVal)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(
                text = freqLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) LowspinTextPrimary else LowspinTextMuted
            )
            Text(
                text = dbLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = levelMb.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = LowspinSurfaceElevated
            )
        )
    }
}

private fun formatFreq(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val k = freqHz / 1000f
        if (k % 1f == 0f) "${k.toInt()} kHz" else "%.1f kHz".format(k)
    } else {
        "$freqHz Hz"
    }
}

private fun defaultFallbackBands(): List<BandInfo> = listOf(
    BandInfo(0, 60, -1500, 1500, 0),
    BandInfo(1, 230, -1500, 1500, 0),
    BandInfo(2, 910, -1500, 1500, 0),
    BandInfo(3, 3600, -1500, 1500, 0),
    BandInfo(4, 14000, -1500, 1500, 0)
)
