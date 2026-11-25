package com.lowspin.app.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlaybackService
import com.lowspin.app.ui.settings.HighSpinThemes
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.settings.LowspinAccentShades
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appearance = LocalAppearance.current
    var refresh by remember { mutableStateOf(0) }

    val themeIndex by appearance.themeIndex.collectAsStateWithLifecycle()
    val pureBlack by appearance.pureBlack.collectAsStateWithLifecycle()
    val visualizerEnabled by appearance.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by appearance.visualizerStyle.collectAsStateWithLifecycle()
    val accentIndex by appearance.accentIndex.collectAsStateWithLifecycle()
    val textScale by appearance.textScale.collectAsStateWithLifecycle()
    val compact by appearance.compact.collectAsStateWithLifecycle()
    val showArt by appearance.showArt.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()
    val highContrast by appearance.highContrast.collectAsStateWithLifecycle()
    val largeTargets by appearance.largeTargets.collectAsStateWithLifecycle()
    val liveNotifications by appearance.liveNotifications.collectAsStateWithLifecycle()

    var cacheBytes by remember { mutableLongStateOf(container.albumArtRepository.diskCacheSizeBytes()) }
    var isRescanning by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val notificationsEnabled = remember(refresh) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelState = remember(refresh) { notificationChannelState(context) }
    val batteryOptimised = remember(refresh) {
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        manager?.isIgnoringBatteryOptimizations(context.packageName) == false
    }

    Column(
        Modifier
            .fillMaxSize()
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingCard {
                Text(
                    text = "Aesthetic Color Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    HighSpinThemes.forEachIndexed { index, theme ->
                        val selected = themeIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { appearance.setTheme(index) }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(theme.primary)
                                    .padding(if (selected) 3.dp else 0.dp)
                                    .then(
                                        if (selected) {
                                            Modifier.background(
                                                LowspinTextPrimary,
                                                RoundedCornerShape(18.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(theme.primary)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = theme.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ToggleRow("Pure Black (AMOLED OLED)", pureBlack, appearance::setPureBlack)
                ToggleRow("Audio Visualizer", visualizerEnabled, appearance::setVisualizerEnabled)
                if (visualizerEnabled) {
                    ChoiceRow(
                        title = "Visualizer Animation Style",
                        options = listOf(
                            "Equalizer Bars" to 0f,
                            "Neon Wave" to 1f,
                            "Pulse Dots" to 2f,
                            "Liquid Aura" to 3f
                        ),
                        selected = visualizerStyle.toFloat(),
                        onSelect = { appearance.setVisualizerStyle(it.toInt()) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    title = "Text size",
                    options = listOf("Small" to 0.9f, "Default" to 1f, "Large" to 1.15f),
                    selected = textScale,
                    onSelect = appearance::setTextScale
                )
                ToggleRow("Compact lists", compact, appearance::setCompact)
                ToggleRow("Show album art", showArt, appearance::setShowArt)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Library & Storage",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingCard {
                val cacheFormatted = if (cacheBytes < 1024 * 1024) {
                    "${cacheBytes / 1024} KB"
                } else {
                    String.format(java.util.Locale.US, "%.1f MB", cacheBytes.toDouble() / (1024 * 1024))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Album Art Cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LowspinTextPrimary
                        )
                        Text(
                            text = "Disk usage: $cacheFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    container.albumArtRepository.clearCaches()
                                    cacheBytes = container.albumArtRepository.diskCacheSizeBytes()
                                }
                                Toast.makeText(context, "Album art cache cleared", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Clear")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Rescan Media Library",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LowspinTextPrimary
                        )
                        Text(
                            text = if (isRescanning) "Scanning files..." else "Scan device for new and updated tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (!isRescanning) {
                                isRescanning = true
                                scope.launch {
                                    container.musicRepository.refreshLibrary()
                                    isRescanning = false
                                    Toast.makeText(context, "Library scan started", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isRescanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isRescanning) "Scanning" else "Rescan")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Offline Guarantee & Privacy",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline by Design",
                            style = MaterialTheme.typography.titleSmall,
                            color = LowspinTextPrimary
                        )
                        Text(
                            text = "HighSpin has zero internet permissions declared in AndroidManifest.xml. Zero analytics, zero crash telemetry, zero cloud synchronization. All audio decoding, lyrics rendering, and equalizer DSP runs purely on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingCard {
                ToggleRow("Reduce motion", reduceMotion, appearance::setReduceMotion)
                ToggleRow("High contrast text", highContrast, appearance::setHighContrast)
                ToggleRow("Larger touch targets", largeTargets, appearance::setLargeTargets)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Notifications & Background Play",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingCard {
                ToggleRow(
                    title = "Android 15 Live Notifications",
                    checked = liveNotifications,
                    onChange = appearance::setLiveNotifications
                )
                Text(
                    text = "Enables real-time progress, live track ticker, and status-bar capsule for Android 15 & 16.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            DiagnosticCard(
                title = "Notifications enabled",
                value = if (notificationsEnabled) "Yes" else "No — playback controls won't appear"
            )
            DiagnosticCard(
                title = "Media channel",
                value = channelState
            )
            DiagnosticCard(
                title = "Battery optimisation",
                value = if (batteryOptimised) "On — may hide the notification" else "Off"
            )

            Spacer(Modifier.height(12.dp))

            if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActionButton("Grant notification permission") {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Spacer(Modifier.height(8.dp))
            }

            ActionButton("Open notification settings") {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
            Spacer(Modifier.height(8.dp))

            if (batteryOptimised) {
                ActionButton("Disable battery optimisation") {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "On OxygenOS, One UI, MIUI and similar skins, enable the " +
                    "\u201CMedia playback\u201D notification category and allow unrestricted background " +
                    "activity for HighSpin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = LowspinTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LowspinOnAccent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<Pair<String, Float>>,
    selected: Float,
    onSelect: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row {
            options.forEach { (label, value) ->
                val active = selected == value
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) LowspinOnAccent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = LowspinTextPrimary
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

private fun notificationChannelState(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "System managed"
    val channel = context.getSystemService(NotificationManager::class.java)
        ?.getNotificationChannel(PlaybackService.NOTIFICATION_CHANNEL_ID)
        ?: return "Not created yet — play a song once"
    return when {
        channel.importance == NotificationManager.IMPORTANCE_NONE -> "Disabled — enable it in notification settings"
        channel.importance >= NotificationManager.IMPORTANCE_LOW -> "Enabled"
        else -> "Low importance — controls may be hidden"
    }
}
