package org.example.project.cards

import android.os.Build
import androidx.annotation.RequiresApi
import coil3.BitmapImage

@RequiresApi(Build.VERSION_CODES.O)
actual fun BitmapImage.sampleArgbPixels(maxSamples: Int): IntArray {
    val src = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
    val w = src.width
    val h = src.height
    if (w == 0 || h == 0) return IntArray(0)
    val total = w * h
    val step = maxOf(1, total / maxSamples)
    val all = IntArray(total)
    src.getPixels(all, 0, w, 0, 0, w, h)
    val count = total / step
    return IntArray(count) { i -> all[i * step] }
}
