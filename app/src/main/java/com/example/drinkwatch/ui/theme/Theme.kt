package com.example.drinkwatch.ui.theme

import android.os.Build
import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Static fallback schemes for devices running below Android 12 (API 31).
// On API 31+, dynamicLightColorScheme / dynamicDarkColorScheme take over and
// derive every role from the user's wallpaper palette.

private val LightColorScheme = lightColorScheme(
    primary                 = Blue40,
    onPrimary               = Neutral100,
    primaryContainer        = Blue90,
    onPrimaryContainer      = Blue10,
    secondary               = Amber40,
    onSecondary             = Neutral100,
    secondaryContainer      = Amber90,
    onSecondaryContainer    = Amber10,
    tertiary                = Teal40,
    onTertiary              = Neutral100,
    tertiaryContainer       = Teal90,
    onTertiaryContainer     = Teal10,
    error                   = Red40,
    onError                 = Neutral100,
    errorContainer          = Red90,
    onErrorContainer        = Red10,
    surface                 = Neutral99,
    onSurface               = Neutral10,
    surfaceVariant          = NeutralVar90,
    onSurfaceVariant        = NeutralVar30,
    surfaceContainerLowest  = Neutral100,
    surfaceContainerLow     = Neutral96,
    surfaceContainer        = Neutral94,
    surfaceContainerHigh    = Neutral92,
    surfaceContainerHighest = Neutral90,
    surfaceDim              = Neutral87,
    surfaceBright           = Neutral99,
    outline                 = NeutralVar50,
    outlineVariant          = NeutralVar80,
    scrim                   = Neutral10,
    inverseSurface          = Neutral20,
    inverseOnSurface        = Neutral95,
    inversePrimary          = Blue80,
    surfaceTint             = Blue40,
)

private val DarkColorScheme = darkColorScheme(
    primary                 = Blue80,
    onPrimary               = Blue20,
    primaryContainer        = Blue40,
    onPrimaryContainer      = Blue90,
    secondary               = Amber80,
    onSecondary             = Amber20,
    secondaryContainer      = Amber40,
    onSecondaryContainer    = Amber90,
    tertiary                = Teal80,
    onTertiary              = Teal20,
    tertiaryContainer       = Teal40,
    onTertiaryContainer     = Teal90,
    error                   = Red80,
    onError                 = Red10,
    errorContainer          = Red40,
    onErrorContainer        = Red90,
    surface                 = Neutral10,
    onSurface               = Neutral90,
    surfaceVariant          = NeutralVar30,
    onSurfaceVariant        = NeutralVar80,
    surfaceContainerLowest  = Neutral04,
    surfaceContainerLow     = Neutral10,
    surfaceContainer        = Neutral12,
    surfaceContainerHigh    = Neutral17,
    surfaceContainerHighest = Neutral22,
    surfaceDim              = Neutral06,
    surfaceBright           = Neutral24,
    outline                 = NeutralVar50,
    outlineVariant          = NeutralVar30,
    scrim                   = Neutral10,
    inverseSurface          = Neutral90,
    inverseOnSurface        = Neutral20,
    inversePrimary          = Blue40,
    surfaceTint             = Blue80,
)

@SuppressLint("NewApi")  // dynamicDarkColorScheme/dynamicLightColorScheme are guarded by SDK_INT >= S above
@Composable
fun DrinkWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses the system wallpaper palette on Android 12+; falls back to
    // the static Blue/Amber scheme on older devices.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}