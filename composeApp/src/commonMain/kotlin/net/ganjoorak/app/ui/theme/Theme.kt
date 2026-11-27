package net.ganjoorak.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ganjoorak.app.data.local.AppSettings
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.gandom
import net.ganjoorak.app.resources.irannastaliq
import net.ganjoorak.app.resources.nahid
import net.ganjoorak.app.resources.parastoo
import net.ganjoorak.app.resources.sahel
import net.ganjoorak.app.resources.samim
import net.ganjoorak.app.resources.shabnam
import net.ganjoorak.app.resources.tanha
import net.ganjoorak.app.resources.vazirmatn
import org.jetbrains.compose.resources.Font

// M3 Expressive polish (Phase A, revised 2026-11-27): MONOCHROME schemes —
// NO olive/sage/green hue anywhere. Light is white with primary #171717
// (the pre-Phase-A look), Dark is near-black with primary #E5E5E5, Paper
// keeps its brown primary #785A32. The three roles in each scheme are
// distinct by TONE only (all R=G=B greys in Light/Dark; browns in Paper),
// never by hue — the #D6D3CC TonalSpot seed is no longer the colour basis.
// Every on-* pair below is >= 4.5:1 (checked numerically, see the
// contrast script); Paper's tertiary was sage green and is now brown.
private val LightColors = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF1C1C1C),
    secondary = Color(0xFF5C5C5C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF262626),
    tertiary = Color(0xFF6E6E6E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFEFEF),
    onTertiaryContainer = Color(0xFF2E2E2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFE9E9E9),
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceTint = Color(0xFF171717),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF1F1F1),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = Color(0xFF747474),
    outlineVariant = Color(0xFFC7C7C7),
    inverseSurface = Color(0xFF2F2F2F),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFC7C7C7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDADADA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5E5E5),
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFF3D3D3D),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFFA8A8A8),
    onTertiary = Color(0xFF1B1B1B),
    tertiaryContainer = Color(0xFF3A3A3A),
    onTertiaryContainer = Color(0xFFEFEFEF),
    background = Color(0xFF131313),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF131313),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF464646),
    onSurfaceVariant = Color(0xFFC7C7C7),
    surfaceTint = Color(0xFFE5E5E5),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1B1B1B),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353535),
    outline = Color(0xFF8F8F8F),
    outlineVariant = Color(0xFF464646),
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF2F2F2F),
    inversePrimary = Color(0xFF171717),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF131313),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PaperColors = lightColorScheme(
    primary = Color(0xFF785A32),
    onPrimary = Color(0xFFFAF7ED),
    primaryContainer = Color(0xFFEEE8D8),
    onPrimaryContainer = Color(0xFF403428),
    secondary = Color(0xFF6B5B45),
    onSecondary = Color(0xFFFAF7ED),
    secondaryContainer = Color(0xFFEDE2C5),
    onSecondaryContainer = Color(0xFF201B0A),
    tertiary = Color(0xFF7A5C3E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEAD9C2),
    onTertiaryContainer = Color(0xFF3E2F1E),
    background = Color(0xFFF4F0E5),
    onBackground = Color(0xFF403428),
    surface = Color(0xFFF4F0E5),
    onSurface = Color(0xFF403428),
    surfaceVariant = Color(0xFFEEE8D8),
    onSurfaceVariant = Color(0xFF6E604E),
    surfaceTint = Color(0xFF785A32),
    surfaceContainerLowest = Color(0xFFFAF7ED),
    surfaceContainerLow = Color(0xFFF6F1E3),
    surfaceContainer = Color(0xFFF1EBDC),
    surfaceContainerHigh = Color(0xFFEEE8D8),
    surfaceContainerHighest = Color(0xFFE1D2B4),
    outline = Color(0xFFB4A688),
    outlineVariant = Color(0xFFD6CCB7),
    inverseSurface = Color(0xFF403428),
    inverseOnSurface = Color(0xFFFAF7ED),
    inversePrimary = Color(0xFFE1D2B4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF4F0E5),
    surfaceDim = Color(0xFFE1D2B4),
    error = Color(0xFFB04428),
    onError = Color(0xFFFAF7ED),
    errorContainer = Color(0xFFF0D9CE),
    onErrorContainer = Color(0xFF5C1F0E),
)

// Expressive shape scale (material3 1.9.0): only the five classic slots are
// settable — the public Shapes constructor takes extraSmall/small/medium/
// large/extraLarge; largeIncreased/extraLargeIncreased/extraExtraLarge exist
// only as internal defaults (20/32/48dp). extraLarge is 28dp (the
// expressive value) instead of the old 20dp. Only corners change; sizes and
// padding stay put.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** How poem text is rendered; derived from [AppSettings]. */
@Immutable
data class PoemStyle(
    val fontFamily: FontFamily,
    val fontSize: TextUnit,
    val layout: CoupletLayout,
    val showVerseNumbers: Boolean,
    val autoScroll: Boolean,
    /** IranNastaliq glyphs span about 2.4 em, so verses need looser lines than the other fonts. */
    val lineHeightMultiplier: Float = 1.9f,
)

val LocalPoemStyle = staticCompositionLocalOf {
    PoemStyle(FontFamily.Default, 18.sp, CoupletLayout.Auto, showVerseNumbers = false, autoScroll = true)
}

fun fontLabel(font: PoemFont): String = when (font) {
    PoemFont.Vazirmatn -> "وزیرمتن"
    PoemFont.Samim -> "صمیم"
    PoemFont.Tanha -> "تنها"
    PoemFont.Shabnam -> "شبنم"
    PoemFont.Gandom -> "گندم"
    PoemFont.Parastoo -> "پرستو"
    PoemFont.Sahel -> "ساحل"
    PoemFont.Nahid -> "ناهید"
    PoemFont.IranNastaliq -> "ایران نستعلیق"
}

@Composable
fun fontFamilyFor(font: PoemFont): FontFamily = FontFamily(
    Font(
        when (font) {
            PoemFont.Vazirmatn -> Res.font.vazirmatn
            PoemFont.Samim -> Res.font.samim
            PoemFont.Tanha -> Res.font.tanha
            PoemFont.Shabnam -> Res.font.shabnam
            PoemFont.Gandom -> Res.font.gandom
            PoemFont.Parastoo -> Res.font.parastoo
            PoemFont.Sahel -> Res.font.sahel
            PoemFont.Nahid -> Res.font.nahid
            PoemFont.IranNastaliq -> Res.font.irannastaliq
        },
    ),
)

private fun Typography.withFont(font: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = font),
    displayMedium = displayMedium.copy(fontFamily = font),
    displaySmall = displaySmall.copy(fontFamily = font),
    headlineLarge = headlineLarge.copy(fontFamily = font),
    headlineMedium = headlineMedium.copy(fontFamily = font),
    headlineSmall = headlineSmall.copy(fontFamily = font),
    titleLarge = titleLarge.copy(fontFamily = font),
    titleMedium = titleMedium.copy(fontFamily = font),
    titleSmall = titleSmall.copy(fontFamily = font),
    bodyLarge = bodyLarge.copy(fontFamily = font),
    bodyMedium = bodyMedium.copy(fontFamily = font),
    bodySmall = bodySmall.copy(fontFamily = font),
    labelLarge = labelLarge.copy(fontFamily = font),
    labelMedium = labelMedium.copy(fontFamily = font),
    labelSmall = labelSmall.copy(fontFamily = font),
)
// Note: the *Emphasized styles are internal in material3 1.9.0 (constructor
// and accessors), so they cannot be re-fonted here or read anywhere —
// FontWeight.Bold stays the emphasis mechanism (plan §2.5 fallback).

@Composable
fun GanjoorakTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System, ThemeMode.Dynamic -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light, ThemeMode.Paper -> false
    }
    val colors: ColorScheme = when {
        settings.theme == ThemeMode.Paper -> PaperColors
        // Dynamic: wallpaper scheme on Android 12+; older Android and
        // desktop return null and fall back to the monochrome scheme.
        settings.theme == ThemeMode.Dynamic -> dynamicColorScheme(dark) ?: if (dark) DarkColors else LightColors
        dark -> DarkColors
        else -> LightColors
    }
    val uiFont = fontFamilyFor(PoemFont.Vazirmatn)
    val poemStyle = PoemStyle(
        fontFamily = fontFamilyFor(settings.poemFont),
        fontSize = 18.sp * settings.fontScale,
        layout = settings.coupletLayout,
        showVerseNumbers = settings.showVerseNumbers,
        autoScroll = settings.autoScrollWithAudio,
        lineHeightMultiplier = if (settings.poemFont == PoemFont.IranNastaliq) 2.4f else 1.9f,
    )

    SystemBarsEffect(lightBackground = !dark)
    MaterialTheme(colorScheme = colors, typography = Typography().withFont(uiFont), shapes = AppShapes) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalPoemStyle provides poemStyle,
        ) {
            content()
        }
    }
}

/** Keeps status/navigation bar icons legible against the app background. */
@Composable
expect fun SystemBarsEffect(lightBackground: Boolean)

/**
 * Material You dynamic colour from the phone wallpaper (ThemeMode.Dynamic).
 * Android 12+ returns the platform dynamic scheme (light/dark following the
 * system); older Android and desktop return null, and the caller falls back
 * to the monochrome Light/Dark scheme. The scheme drives ALL roles, so the
 * floating pill follows it automatically. Note: a wallpaper can be any hue —
 * including greenish ones. That is the point of this mode: it is the user's
 * explicit choice, not the app default.
 */
@Composable
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?
