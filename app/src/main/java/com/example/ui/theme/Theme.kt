package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightM3Colors = lightColorScheme(
    primary = Color(0xFF1F3D36),          // Pine
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1D1),  // Soft neutral
    onPrimaryContainer = Color(0xFF1F3D36),
    secondary = Color(0xFFC5A77D),        // Gold
    onSecondary = Color(0xFF1B1B19),
    secondaryContainer = Color(0xFFE9E1D1),
    onSecondaryContainer = Color(0xFF1B1B19),
    tertiary = Color(0xFF642B3A),         // Wine accent
    onTertiary = Color.White,
    background = Color(0xFFF6F0E4),       // Light background
    onBackground = Color(0xFF1B1B19),     // Text
    surface = Color(0xFFF6F0E4),
    onSurface = Color(0xFF1B1B19),
    surfaceVariant = Color(0xFFE9E1D1),   // Soft neutral
    onSurfaceVariant = Color(0xFF6A6558), // Muted text
    outline = Color(0xFFC5A77D),          // Gold
    outlineVariant = Color(0xFFE9E1D1),   // Rules
    error = Color(0xFF642B3A),            // Wine
    onError = Color.White
)

private val DarkM3Colors = darkColorScheme(
    primary = Color(0xFF2F6B5A),          // Pine
    onPrimary = Color(0xFFF1EBDD),
    primaryContainer = Color(0xFF262B31),  // Soft neutral
    onPrimaryContainer = Color(0xFF7DB8A5),// Pine text
    secondary = Color(0xFFC5A77D),        // Gold
    onSecondary = Color(0xFF15181C),
    secondaryContainer = Color(0xFF262B31),
    onSecondaryContainer = Color(0xFFF1EBDD),
    tertiary = Color(0xFFD58A9C),         // Wine text
    onTertiary = Color(0xFF15181C),
    background = Color(0xFF15181C),       // Dark background
    onBackground = Color(0xFFF1EBDD),     // Text
    surface = Color(0xFF15181C),
    onSurface = Color(0xFFF1EBDD),
    surfaceVariant = Color(0xFF262B31),   // Soft neutral
    onSurfaceVariant = Color(0xFFA29F94), // Muted text
    outline = Color(0xFFC5A77D),          // Gold
    outlineVariant = Color(0xFF2A2F36),   // Rules
    error = Color(0xFF8A3A50),            // Wine fill
    onError = Color.White
)

@Composable
fun QuranBaantoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val quranColors = if (darkTheme) DarkQuranBaantoColors else LightQuranBaantoColors
    val m3ColorScheme = if (darkTheme) DarkM3Colors else LightM3Colors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalQuranBaantoColors provides quranColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
