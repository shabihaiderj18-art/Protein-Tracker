package app.protein.tracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.protein.tracker.domain.ThemeMode
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.ui.ProteinAppUi
import app.protein.tracker.ui.theme.ProteinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsFlow = (application as ProteinApp).container.settings.settings

        setContent {
            val settings: UserSettings? by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // Draw nothing for the few milliseconds it takes to read the settings,
            // so the wrong theme never flashes.
            val current = settings ?: return@setContent

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (current.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            ProteinTheme(darkTheme = darkTheme, dynamicColor = current.dynamicColor) {
                ProteinAppUi()
            }
        }
    }
}
