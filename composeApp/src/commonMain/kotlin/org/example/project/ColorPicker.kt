package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.auth.HaEntityState
import org.example.project.cards.LocalEntityStatesFlow
import org.example.project.cards.LocalEntityStatesLoaded
import org.example.project.cards.WeatherForecastCard

// ── Color conversion helpers ──────────────────────────────────────────────────

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
    val v = max
    val s = if (max == 0f) 0f else delta / max
    val rawH = when {
        delta == 0f -> 0f
        max == r    -> 60f * (((g - b) / delta) % 6f)
        max == g    -> 60f * ((b - r) / delta + 2f)
        else        -> 60f * ((r - g) / delta + 4f)
    }
    val h = if (rawH < 0f) rawH + 360f else rawH
    return Triple(h, s, v)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}

// ── Public composable ─────────────────────────────────────────────────────────

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val (initH, initS, initV) = remember(initialColor) { initialColor.toHsv() }
    var hue        by remember { mutableFloatStateOf(initH) }
    var saturation by remember { mutableFloatStateOf(initS.coerceAtLeast(0.01f)) }
    var brightness by remember { mutableFloatStateOf(initV.coerceAtLeast(0.01f)) }

    val currentColor = remember(hue, saturation, brightness) {
        hsvToColor(hue, saturation, brightness)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a primary color") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Choose a primary color — a full harmonious palette will be generated automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SatValBox(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChanged = { s, v -> saturation = s; brightness = v },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )

                HueSlider(
                    hue = hue,
                    onHueChanged = { hue = it },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                    )
                    val r = (currentColor.red * 255 + 0.5f).toInt()
                    val g = (currentColor.green * 255 + 0.5f).toInt()
                    val b = (currentColor.blue * 255 + 0.5f).toInt()
                    val rgb = (r shl 16) or (g shl 8) or b
                    Text(
                        text = "#${rgb.toString(16).padStart(6, '0').uppercase()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Live preview: only this subtree gets the picked seed color via a
                // nested AppTheme. The dialog chrome stays on the active theme until
                // the user clicks Apply.
                val previewDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val previewFlow = remember {
                    MutableStateFlow<Map<String, HaEntityState>?>(buildShowcaseEntityStates())
                }
                val previewConfig = remember { showcaseCardConfig("weather-forecast", "weather.home") }
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTheme(darkTheme = previewDark, seedColor = currentColor) {
                    CompositionLocalProvider(
                        LocalEntityStatesFlow provides previewFlow,
                        LocalEntityStatesLoaded provides true,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            WeatherForecastCard(
                                config = previewConfig,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Internal widgets ──────────────────────────────────────────────────────────

@Composable
private fun SatValBox(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChanged: (sat: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val boxW = maxWidth
        val boxH = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(Color.White, hsvToColor(hue, 1f, 1f))),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChanged(
                            (offset.x / size.width).coerceIn(0f, 1f),
                            1f - (offset.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onChanged(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            1f - (change.position.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                },
        )

        val cursorX = boxW * saturation
        val cursorY = boxH * (1f - brightness)
        val thumbR  = 10.dp
        Box(
            modifier = Modifier
                .offset(x = cursorX - thumbR, y = cursorY - thumbR)
                .size(thumbR * 2)
                .clip(CircleShape)
                .background(hsvToColor(hue, saturation, brightness))
                .border(2.dp, Color.White, CircleShape),
        )
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(4.dp))) {
        val boxW = maxWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Red,
                            30f / 360f to Color(0xFFFF7F00),
                            60f / 360f to Color.Yellow,
                            120f / 360f to Color.Green,
                            180f / 360f to Color.Cyan,
                            240f / 360f to Color.Blue,
                            300f / 360f to Color.Magenta,
                            1f to Color.Red,
                        ),
                    ),
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onHueChanged((offset.x / size.width * 360f).coerceIn(0f, 360f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onHueChanged((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                    }
                },
        )

        val cursorX = boxW * (hue / 360f)
        Box(
            modifier = Modifier
                .offset(x = cursorX - 2.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f)),
        )
    }
}
