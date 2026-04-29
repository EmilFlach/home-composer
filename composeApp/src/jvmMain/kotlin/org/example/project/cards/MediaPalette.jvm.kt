package org.example.project.cards

import coil3.BitmapImage
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

actual fun BitmapImage.sampleArgbPixels(maxSamples: Int): IntArray {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return IntArray(0)
    val total = w * h
    val step = maxOf(1, total / maxSamples)
    val info = ImageInfo(ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), w, h)
    val bytes = bitmap.readPixels(info) ?: return IntArray(0)
    val count = total / step
    return IntArray(count) { i ->
        val off = (i * step) * 4
        if (off + 3 >= bytes.size) return@IntArray 0
        val r = bytes[off].toInt() and 0xFF
        val g = bytes[off + 1].toInt() and 0xFF
        val b = bytes[off + 2].toInt() and 0xFF
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
