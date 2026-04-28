package org.example.project

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────
//  NEON COLOR PALETTE  (deep-space futuristic)
// ─────────────────────────────────────────────────────────
object NeonColors {
    // Backgrounds
    val Background        = Color(0xFF080010)
    val Surface           = Color(0xFF110020)
    val SurfaceElevated   = Color(0xFF1E0040)

    // Chip / pill surface
    val ChipBackground    = Color(0xFF1E0040)
    val ChipContent       = Color(0xFFCCA8FF)

    // Accent neons
    val PrimaryPurple     = Color(0xFFA060FF)
    val NeonCyan          = Color(0xFF00D4FF)
    val NeonAmber         = Color(0xFFFFB300)
    val NeonAmberWarm     = Color(0xFFFF6D00)
    val HotMagenta        = Color(0xFFFF006E)

    // Text
    val TextPrimary       = Color(0xFFE0E8FF)
    val TextMuted         = Color(0xFF6080A8)

    // Shutter visuals (cyberpunk steel)
    val ShutterFrame      = Color(0xFF0D1E3A)
    val ShutterGlass      = Color(0xFF030A14)
    val ShutterSlat       = Color(0xFF0A1E36)
    val ShutterNeonEdge   = Color(0xFF0066CC)
}

// ─────────────────────────────────────────────────────────
//  GRADIENT BRUSHES
// ─────────────────────────────────────────────────────────
object NeonGradients {
    val PurpleToCyan = Brush.linearGradient(
        listOf(NeonColors.PrimaryPurple, NeonColors.NeonCyan)
    )
    val CyanToPurple = Brush.linearGradient(
        listOf(NeonColors.NeonCyan, NeonColors.PrimaryPurple)
    )
    val AmberToOrange = Brush.linearGradient(
        listOf(NeonColors.NeonAmber, NeonColors.NeonAmberWarm)
    )
    // Subtle gradient for the card border (50 % opacity so it doesn't overpower)
    val CardBorder = Brush.linearGradient(
        listOf(
            NeonColors.PrimaryPurple.copy(alpha = 0.55f),
            NeonColors.NeonCyan.copy(alpha = 0.55f)
        )
    )
    // Screen-level vertical background gradient
    val ScreenBackground = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF0C0020),
            0.35f to NeonColors.Background,
            1.0f to NeonColors.Background
        )
    )
}

// ─────────────────────────────────────────────────────────
//  MODIFIER EXTENSION  – add once to any Card
// ─────────────────────────────────────────────────────────
private val CardCorners = RoundedCornerShape(12.dp)

fun Modifier.neonCardBorder(shape: Shape = CardCorners): Modifier = composed {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val brush = Brush.linearGradient(
        listOf(primary.copy(alpha = 0.55f), secondary.copy(alpha = 0.55f))
    )
    border(width = 1.dp, brush = brush, shape = shape)
}
