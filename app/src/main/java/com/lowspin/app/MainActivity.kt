package com.lowspin.app

import android.graphics.Color
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.ui.LocalAlbumArtRepository
import com.lowspin.app.ui.LowspinApp
import com.lowspin.app.ui.settings.AppearanceSettings
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.theme.LowspinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val container = (application as LowspinApplication).container
        val appearance = AppearanceSettings(applicationContext)

        setContent {
            val textScale by appearance.textScale.collectAsStateWithLifecycle()
            val density = LocalDensity.current

            CompositionLocalProvider(LocalAppearance provides appearance) {
                LowspinTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = density.density,
                            fontScale = density.fontScale * textScale
                        ),
                        LocalAlbumArtRepository provides container.albumArtRepository
                    ) {
                        LowspinApp(container)
                    }
                }
            }
        }
    }
}
