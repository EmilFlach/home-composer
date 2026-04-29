package org.example.project

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

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
fun AppTheme(darkTheme: Boolean, seedColor: Color? = null, content: @Composable () -> Unit) {
    // Always call rememberDynamicColorScheme unconditionally to keep Compose slot order stable.
    // Use the seed when provided, otherwise fall back to the hardcoded schemes.
    val effectiveSeed = seedColor ?: Color(0xFF6B20D0)
    val dynamicScheme = rememberDynamicColorScheme(
        seedColor = effectiveSeed,
        isDark = darkTheme,
        // Content keeps the seed hue prominent in the primary palette and produces
        // an analogous tertiary that harmonizes with it, instead of Vibrant's
        // generic loud-but-shifted result.
        style = PaletteStyle.Content,
    )
    val tinted = remember(dynamicScheme, effectiveSeed, darkTheme) {
        dynamicScheme.tunedForSeed(effectiveSeed, isDark = darkTheme)
    }
    val colorScheme = if (seedColor != null) tinted else if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// Pull the seed color into background/surface tones so the chrome is noticeably
// tinted, and force primary to the user's actual pick (the M3 algorithm's
// tone-80/tone-40 substitute is too muted compared to the swatch the user
// selected).
//
// In dark mode we construct surfaces from pure black + seed at increasing
// ratios: lerping the M3-generated near-black background toward a bright seed
// barely shifts it off black, but `lerp(Black, seed, r)` lands directly on a
// dark tint of the seed's hue. The ratios still grow with elevation so
// surfaceContainer* keeps an elevation gradient.
private fun ColorScheme.tunedForSeed(seed: Color, isDark: Boolean): ColorScheme {
    val onSeed = if (seed.luminance() > 0.45f) Color.Black else Color.White
    return if (isDark) {
        copy(
            primary                 = seed,
            onPrimary               = onSeed,
            background              = lerp(Color.Black, seed, 0.20f),
            surface                 = lerp(Color.Black, seed, 0.20f),
            surfaceVariant          = lerp(Color.Black, seed, 0.30f),
            surfaceContainerLowest  = lerp(Color.Black, seed, 0.16f),
            surfaceContainerLow     = lerp(Color.Black, seed, 0.23f),
            surfaceContainer        = lerp(Color.Black, seed, 0.27f),
            surfaceContainerHigh    = lerp(Color.Black, seed, 0.32f),
            surfaceContainerHighest = lerp(Color.Black, seed, 0.37f),
        )
    } else {
        val surfaceTint = 0.12f
        val variantTint = 0.16f
        copy(
            primary                 = seed,
            onPrimary               = onSeed,
            background              = lerp(background, seed, surfaceTint),
            surface                 = lerp(surface, seed, surfaceTint),
            surfaceVariant          = lerp(surfaceVariant, seed, variantTint),
            surfaceContainerLowest  = lerp(surfaceContainerLowest, seed, surfaceTint),
            surfaceContainerLow     = lerp(surfaceContainerLow, seed, surfaceTint),
            surfaceContainer        = lerp(surfaceContainer, seed, surfaceTint),
            surfaceContainerHigh    = lerp(surfaceContainerHigh, seed, surfaceTint),
            surfaceContainerHighest = lerp(surfaceContainerHighest, seed, surfaceTint),
        )
    }
}
