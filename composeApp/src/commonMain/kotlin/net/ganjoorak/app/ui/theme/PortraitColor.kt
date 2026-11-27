package net.ganjoorak.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import coil3.Image
import kotlin.math.pow

/**
 * Dominant colour of a loaded portrait, for the PoetHero second colour
 * source (plan §2.2). Android extracts it from the Coil bitmap; desktop
 * returns null and the hero falls back to the neutral scheme. Any failure
 * (no image, no saturated pixels) returns null — never a guessed colour.
 */
expect fun dominantColor(image: Image): Color?

/**
 * Portrait colour as a container background: the portrait hue blended
 * lightly (25%) over [ColorScheme.surfaceContainerHigh], so the chip stays
 * close to the surface and the text on it stays legible. Returns null when
 * the blend would fail the 4.5:1 contrast gate for
 * [ColorScheme.onSecondaryContainer] — the caller then uses the neutral
 * container instead.
 */
fun portraitContainerColor(portrait: Color, scheme: ColorScheme): Color? {
    val blended = Color(
        red = scheme.surfaceContainerHigh.red + (portrait.red - scheme.surfaceContainerHigh.red) * 0.25f,
        green = scheme.surfaceContainerHigh.green + (portrait.green - scheme.surfaceContainerHigh.green) * 0.25f,
        blue = scheme.surfaceContainerHigh.blue + (portrait.blue - scheme.surfaceContainerHigh.blue) * 0.25f,
        alpha = 1f,
    )
    return if (contrastRatio(scheme.onSecondaryContainer, blended) >= 4.5f) blended else null
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = relativeLuminance(foreground)
    val l2 = relativeLuminance(background)
    val (hi, lo) = if (l1 >= l2) l1 to l2 else l2 to l1
    return (hi + 0.05f) / (lo + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun channel(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f
        else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}
