package com.example.drinkwatch

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.AppSettings
import com.example.drinkwatch.data.model.Theme
import com.example.drinkwatch.ui.navigation.AppNavHost
import com.example.drinkwatch.ui.theme.DrinkWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as DrinkWatchApplication
            val settings by app.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.theme) {
                Theme.LIGHT  -> false
                Theme.DARK   -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            }
            DrinkWatchTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}