package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B57FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE5FF),
    onPrimaryContainer = Color(0xFF1B0066),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8B0FF),
    onPrimary = Color(0xFF2E00A0),
    primaryContainer = Color(0xFF4539CC),
    onPrimaryContainer = Color(0xFFEAE5FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF003740),
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
