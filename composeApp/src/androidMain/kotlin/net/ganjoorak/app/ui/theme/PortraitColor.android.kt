package net.ganjoorak.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import coil3.BitmapImage
import coil3.Image

/**
 * Android: dominant colour of the Coil bitmap — the average of saturated,
 * mid-tone pixels of a 32px downscale. Returns null when the image is not a
 * bitmap or too few pixels qualify (grey/white portraits fall back neutral).
 */
actual fun dominantColor(image: Image): Color? {
    val bitmap = (image as? BitmapImage)?.bitmap ?: return null
    val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    val pixels = IntArray(32 * 32)
    scaled.getPixels(pixels, 0, 32, 0, 0, 32, 32)
    if (scaled != bitmap) scaled.recycle()
    var r = 0L
    var g = 0L
    var b = 0L
    var count = 0L
    for (pixel in pixels) {
        val a = pixel ushr 24 and 0xFF
        if (a < 128) continue
        val pr = pixel ushr 16 and 0xFF
        val pg = pixel ushr 8 and 0xFF
        val pb = pixel and 0xFF
        val max = maxOf(pr, pg, pb)
        val min = minOf(pr, pg, pb)
        if (max - min < 24) continue // near-grey: no hue to take
        val lum = (pr + pg + pb) / 3
        if (lum < 24 || lum > 232) continue // near-black / near-white
        r += pr
        g += pg
        b += pb
        count++
    }
    if (count < 100) return null // less than ~10% of pixels qualify
    return Color(
        red = (r / count).toInt() / 255f,
        green = (g / count).toInt() / 255f,
        blue = (b / count).toInt() / 255f,
        alpha = 1f,
    )
}
