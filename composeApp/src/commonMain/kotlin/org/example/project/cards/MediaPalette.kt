package org.example.project.cards

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import coil3.BitmapImage

val LocalOnMediaPaletteAccentChanged = compositionLocalOf<((Color) -> Unit)?> { null }

data class MediaPalette(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val onBackground: Color,
)

expect fun BitmapImage.sampleArgbPixels(maxSamples: Int): IntArray

fun extractMediaPalette(bitmapImage: BitmapImage): MediaPalette? {
    val pixels = bitmapImage.sampleArgbPixels(600)
    if (pixels.isEmpty()) return null

    data class Sample(val h: Float, val s: Float, val l: Float, val score: Float)

    val samples = buildList {
        for (argb in pixels) {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2f
            if (max == min) continue
            val d = max - min
            val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            if (s < 0.18f || l < 0.12f || l > 0.88f) continue
            val h = when (max) {
                r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
                g -> ((b - r) / d + 2f) / 6f
                else -> ((r - g) / d + 4f) / 6f
            }
            val score = s * (1f - kotlin.math.abs(2f * l - 1f))
            add(Sample(h, s, l, score))
        }
    }
    if (samples.isEmpty()) return null

    // Weighted hue-bin voting (12 bins × 30°)
    val binWeightedHue = FloatArray(12)
    val binWeight = FloatArray(12)
    for (sample in samples) {
        val bin = (sample.h * 12f).toInt().coerceIn(0, 11)
        binWeightedHue[bin] += sample.h * sample.score
        binWeight[bin] += sample.score
    }
    val best = binWeight.indices.maxByOrNull { binWeight[it] } ?: return null
    if (binWeight[best] < 0.01f) return null
    val hue = binWeightedHue[best] / binWeight[best]

    return MediaPalette(
        background = hslToColor(hue, 0.42f, 0.08f),
        surface = hslToColor(hue, 0.28f, 0.14f),
        accent = hslToColor(hue, 0.88f, 0.62f),
        onBackground = Color.White,
    )
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    if (s == 0f) return Color(l, l, l)
    fun hue2rgb(p: Float, q: Float, t: Float): Float {
        var t2 = if (t < 0f) t + 1f else if (t > 1f) t - 1f else t
        return when {
            t2 < 1f / 6f -> p + (q - p) * 6f * t2
            t2 < 0.5f -> q
            t2 < 2f / 3f -> p + (q - p) * (2f / 3f - t2) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(
        red = hue2rgb(p, q, h + 1f / 3f),
        green = hue2rgb(p, q, h),
        blue = hue2rgb(p, q, h - 1f / 3f),
    )
}
