package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioColorScheme = darkColorScheme(
    primary = StudioPurple,
    onPrimary = StudioText,
    secondary = StudioCyan,
    onSecondary = StudioBg,
    tertiary = StudioAmber,
    onTertiary = StudioBg,
    background = StudioBg,
    onBackground = StudioText,
    surface = StudioSurface,
    onSurface = StudioText,
    surfaceVariant = StudioSurface2,
    onSurfaceVariant = StudioMuted,
    outline = StudioBorder,
    error = StudioRed,
    onError = StudioText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = StudioColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = StudioBg.toArgb()
            window.navigationBarColor = StudioBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
