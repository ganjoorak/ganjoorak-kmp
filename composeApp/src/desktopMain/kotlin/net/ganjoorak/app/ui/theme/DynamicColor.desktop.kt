package net.ganjoorak.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Desktop has no wallpaper colour source: always null, and GanjoorakTheme
 * falls back to the monochrome Light/Dark scheme.
 */
@Composable
actual fun dynamicColorScheme(dark: Boolean): ColorScheme? = null
