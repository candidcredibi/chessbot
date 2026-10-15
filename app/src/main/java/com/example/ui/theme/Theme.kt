package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChessDarkColorScheme = darkColorScheme(
    primary = ChessGreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00391A),
    onPrimaryContainer = Color(0xFF75FF9A),
    secondary = ChessBlueGM,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF002F6C),
    onSecondaryContainer = Color(0xFFB0D0FF),
    tertiary = ChessAmberWarning,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = ChessRedAlert,
    onError = Color.White
)

@Composable
fun ChessMasterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChessDarkColorScheme,
        typography = Typography,
        content = content
    )
}
