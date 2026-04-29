package org.example.project.cards

import coil3.BitmapImage

actual fun BitmapImage.sampleArgbPixels(maxSamples: Int): IntArray {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return IntArray(0)
    val total = w * h
    val step = maxOf(1, total / maxSamples)
    val all = IntArray(total)
    bitmap.getPixels(all, 0, w, 0, 0, w, h)
    val count = total / step
    return IntArray(count) { i -> all[i * step] }
}
