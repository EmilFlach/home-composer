package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── DEEP VIOLET  (dark theme) ────────────────────────────
private val DarkColors = darkColorScheme(
    primary                  = Color(0xFF9B40FF),
    onPrimary                = Color(0xFF080010),
    primaryContainer         = Color(0xFF3A0080),
    onPrimaryContainer       = Color(0xFFDDC0FF),

    secondary                = Color(0xFF00D4FF),
    onSecondary              = Color(0xFF080010),
    secondaryContainer       = Color(0xFF003044),
    onSecondaryContainer     = Color(0xFF80EAFF),

    tertiary                 = Color(0xFFFF006E),
    onTertiary               = Color(0xFF080010),
    tertiaryContainer        = Color(0xFF4A0022),
    onTertiaryContainer      = Color(0xFFFFB3CC),

    background               = Color(0xFF080010),
    onBackground             = Color(0xFFEDE0FF),

    surface                  = Color(0xFF080010),
    onSurface                = Color(0xFFEDE0FF),
    surfaceVariant           = Color(0xFF1E0040),
    onSurfaceVariant         = Color(0xFFE3DAF6),

    // M3 container-surface tokens (drives Card colours)
    surfaceContainerLowest   = Color(0xFF060008),
    surfaceContainerLow      = Color(0xFF110020),   // ← default Card bg
    surfaceContainer         = Color(0xFF180030),
    surfaceContainerHigh     = Color(0xFF1E0040),
    surfaceContainerHighest  = Color(0xFF260050),

    outline                  = Color(0xFF3A1860),
    outlineVariant           = Color(0xFF280848),

    inverseSurface           = Color(0xFFEDE0FF),
    inverseOnSurface         = Color(0xFF080010),
    inversePrimary           = Color(0xFF6B20D0),

    scrim                    = Color(0xFF000000),
    error                    = Color(0xFFFF2D6B),
    onError                  = Color(0xFF080010),
    errorContainer           = Color(0xFF4A0020),
    onErrorContainer         = Color(0xFFFFB3CC),
)

// ── SATURATED LIGHT  (light theme) ───────────────────────
private val LightColors = lightColorScheme(
    primary                  = Color(0xFF6B20D0),
    onPrimary                = Color.White,
    primaryContainer         = Color(0xFFEADCFF),
    onPrimaryContainer       = Color(0xFF22006A),

    secondary                = Color(0xFF0088BB),
    onSecondary              = Color.White,
    secondaryContainer       = Color(0xFFCCF0FF),
    onSecondaryContainer     = Color(0xFF003A4F),

    background               = Color(0xFFF4F0FF),
    onBackground             = Color(0xFF12003A),
    surface                  = Color(0xFFF4F0FF),
    onSurface                = Color(0xFF12003A),
    surfaceVariant           = Color(0xFFEADDFF),
    onSurfaceVariant         = Color(0xFF2F2450),
    outline                  = Color(0xFF9080B8),
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
