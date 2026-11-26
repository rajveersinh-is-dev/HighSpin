package com.lowspin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.ui.settings.HighSpinThemes
import com.lowspin.app.ui.settings.LocalAppearance

@Composable
fun LowspinTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val themeIndex by appearance.themeIndex.collectAsStateWithLifecycle()
    val pureBlack by appearance.pureBlack.collectAsStateWithLifecycle()
    val highContrast by appearance.highContrast.collectAsStateWithLifecycle()

    val colorScheme = remember(themeIndex, pureBlack, highContrast) {
        val preset = HighSpinThemes[themeIndex.coerceIn(0, HighSpinThemes.lastIndex)]
        val primary = preset.primary
        val secondary = preset.secondary

        val bg = if (pureBlack) Color(0xFF000000) else preset.background
        val surface = if (pureBlack) Color(0xFF000000) else preset.surface
        val surfaceHigh = if (pureBlack) Color(0xFF101014) else Color(
            red = (preset.surface.red * 1.35f).coerceAtMost(1f),
            green = (preset.surface.green * 1.35f).coerceAtMost(1f),
            blue = (preset.surface.blue * 1.35f).coerceAtMost(1f)
        )

        darkColorScheme(
            primary = primary,
            onPrimary = LowspinOnAccent,
            primaryContainer = secondary,
            onPrimaryContainer = LowspinTextPrimary,
            secondary = secondary,
            onSecondary = LowspinOnAccent,
            background = bg,
            onBackground = LowspinTextPrimary,
            surface = surface,
            onSurface = LowspinTextPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = if (highContrast) {
                Color(0xFFE0E0EC)
            } else {
                LowspinTextSecondary
            },
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHigh,
            error = LowspinError,
            onError = bg,
            outline = surfaceHigh,
            outlineVariant = surfaceHigh
        )
    }

    val fontFamily = remember(context) { lowspinFontFamily(context) }
    val typography = remember(fontFamily) { lowspinTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
