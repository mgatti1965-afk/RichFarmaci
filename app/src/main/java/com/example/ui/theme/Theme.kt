package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = Slate600,
    tertiary = Pink40,
    background = Slate900,
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White,
    onSurfaceVariant = White
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = Slate600,
    tertiary = Pink40,
    background = GrayBackground,
    surface = White,
    surfaceVariant = BlueInputBg,
    onPrimary = White,
    onSecondary = White,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate900
)

@Composable
fun RichFarmaciTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure high contrast custom palette
    content: @Composable () -> Unit,
) {
    // Force light scheme to avoid dark mode conflicts
    val colorScheme = LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
