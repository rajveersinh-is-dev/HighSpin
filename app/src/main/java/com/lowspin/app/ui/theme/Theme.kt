package com.lowspin.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.ui.settings.HighSpinThemes
import com.lowspin.app.ui.settings.LocalAppearance

@Composable
fun LowspinTheme(
    dominantColor: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val themeIndex by appearance.themeIndex.collectAsStateWithLifecycle()
    val pureBlack by appearance.pureBlack.collectAsStateWithLifecycle()
    val highContrast by appearance.highContrast.collectAsStateWithLifecycle()
    val dynamicPalette by appearance.dynamicPalette.collectAsStateWithLifecycle()

    val targetColors = remember(themeIndex, pureBlack, highContrast, dynamicPalette, dominantColor) {
        val preset = HighSpinThemes[themeIndex.coerceIn(0, HighSpinThemes.lastIndex)]
        val useDynamic = dynamicPalette && dominantColor != 0

        val primary = if (useDynamic) deriveVividAccent(dominantColor) else preset.primary
        val secondary = if (useDynamic) deriveHarmoniousSecondary(primary) else preset.secondary

        val bg = if (pureBlack) {
            Color(0xFF000000)
        } else if (useDynamic) {
            lerp(preset.background, primary, 0.08f)
        } else {
            preset.background
        }

        val surface = if (pureBlack) {
            Color(0xFF000000)
        } else if (useDynamic) {
            lerp(preset.surface, primary, 0.11f)
        } else {
            preset.surface
        }

        val surfaceHigh = if (pureBlack) {
            if (useDynamic) lerp(Color(0xFF101014), primary, 0.09f) else Color(0xFF101014)
        } else {
            Color(
                red = (surface.red * 1.35f).coerceAtMost(1f),
                green = (surface.green * 1.35f).coerceAtMost(1f),
                blue = (surface.blue * 1.35f).coerceAtMost(1f)
            )
        }

        listOf(primary, secondary, bg, surface, surfaceHigh)
    }

    val primary by animateColorAsState(targetColors[0], tween(450), label = "themePrimary")
    val secondary by animateColorAsState(targetColors[1], tween(450), label = "themeSecondary")
    val bg by animateColorAsState(targetColors[2], tween(450), label = "themeBg")
    val surface by animateColorAsState(targetColors[3], tween(450), label = "themeSurface")
    val surfaceHigh by animateColorAsState(targetColors[4], tween(450), label = "themeSurfaceHigh")

    val colorScheme = remember(primary, secondary, bg, surface, surfaceHigh, highContrast) {
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

private fun deriveVividAccent(argb: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    hsv[1] = hsv[1].coerceIn(0.42f, 0.86f)
    hsv[2] = hsv[2].coerceAtLeast(0.84f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun deriveHarmoniousSecondary(primary: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    hsv[0] = (hsv[0] + 34f) % 360f
    hsv[1] = (hsv[1] * 0.92f).coerceIn(0.35f, 0.85f)
    hsv[2] = (hsv[2] * 0.85f).coerceIn(0.55f, 0.92f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
