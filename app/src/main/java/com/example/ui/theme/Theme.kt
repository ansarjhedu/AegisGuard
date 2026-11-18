package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = CyberSecondary,
    onSecondary = Color(0xFF003919),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Color(0xFF70FF9C),
    tertiary = CyberTertiary,
    onTertiary = Color.White,
    background = CyberDarkBackground,
    onBackground = CyberTextPrimary,
    surface = CyberDarkSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberDarkSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberBorder,
    error = CyberCritical,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006775),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF006D37),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF70FF9C),
    onSecondaryContainer = Color(0xFF00210C),
    tertiary = CyberTertiary,
    background = CyberLightBackground,
    onBackground = CyberLightTextPrimary,
    surface = CyberLightSurface,
    onSurface = CyberLightTextPrimary,
    surfaceVariant = CyberLightSurfaceVariant,
    onSurfaceVariant = CyberLightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = CyberCritical,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek security dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
