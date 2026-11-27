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

// M3 Expressive polish (Phase A): TonalSpot scheme generated offline from seed
// #D6D3CC (material-color-utilities, standard contrast 0.0) and pasted here —
// material3 1.9.0 has no public seed->scheme API and there is no kolor dep.
// Secondary is NOT the library clone: it is shifted to a warmer, greyer
// khaki (HCT hue 90, chroma 10) so primary/secondary/tertiary read apart at
// a glance; every on-* pair below is >= 4.5:1 (checked numerically).
// Paper keeps its brown primary #785A32; only its cloned roles are split.
private val LightColors = lightColorScheme(
    primary = Color(0xFF5F621A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E891),
    onPrimaryContainer = Color(0xFF474A01),
    secondary = Color(0xFF665E4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE1CA),
    onSecondaryContainer = Color(0xFF211B0D),
    tertiary = Color(0xFF3C6658),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFECDA),
    onTertiaryContainer = Color(0xFF244E41),
    background = Color(0xFFFCFAEC),
    onBackground = Color(0xFF1C1C14),
    surface = Color(0xFFFCFAEC),
    onSurface = Color(0xFF1C1C14),
    surfaceVariant = Color(0xFFE5E3D2),
    onSurfaceVariant = Color(0xFF48473B),
    surfaceTint = Color(0xFF5F621A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4E6),
    surfaceContainer = Color(0xFFF1EEE1),
    surfaceContainerHigh = Color(0xFFEBE8DB),
    surfaceContainerHighest = Color(0xFFE5E3D6),
    outline = Color(0xFF787869),
    outlineVariant = Color(0xFFC9C7B6),
    inverseSurface = Color(0xFF313128),
    inverseOnSurface = Color(0xFFF4F1E4),
    inversePrimary = Color(0xFFC8CC78),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFCFAEC),
    surfaceDim = Color(0xFFDDDACD),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8CC78),
    onPrimary = Color(0xFF303300),
    primaryContainer = Color(0xFF474A01),
    onPrimaryContainer = Color(0xFFE4E891),
    secondary = Color(0xFFD1C5AF),
    onSecondary = Color(0xFF363020),
    secondaryContainer = Color(0xFF4D4635),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFA3D0BF),
    onTertiary = Color(0xFF09372B),
    tertiaryContainer = Color(0xFF244E41),
    onTertiaryContainer = Color(0xFFBFECDA),
    background = Color(0xFF14140C),
    onBackground = Color(0xFFE5E3D6),
    surface = Color(0xFF14140C),
    onSurface = Color(0xFFE5E3D6),
    surfaceVariant = Color(0xFF48473B),
    onSurfaceVariant = Color(0xFFC9C7B6),
    surfaceTint = Color(0xFFC8CC78),
    surfaceContainerLowest = Color(0xFF0E0F08),
    surfaceContainerLow = Color(0xFF1C1C14),
    surfaceContainer = Color(0xFF202018),
    surfaceContainerHigh = Color(0xFF2A2A22),
    surfaceContainerHighest = Color(0xFF35352C),
    outline = Color(0xFF929182),
    outlineVariant = Color(0xFF48473B),
    inverseSurface = Color(0xFFE5E3D6),
    inverseOnSurface = Color(0xFF313128),
    inversePrimary = Color(0xFF5F621A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3A31),
    surfaceDim = Color(0xFF14140C),
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
    secondary = Color(0xFF665E48),
    onSecondary = Color(0xFFFAF7ED),
    secondaryContainer = Color(0xFFEDE2C5),
    onSecondaryContainer = Color(0xFF201B0A),
    tertiary = Color(0xFF52643E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD5EABA),
    onTertiaryContainer = Color(0xFF3B4C29),
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

// Expressive shape tokens (material3 1.9.0): the old 4/6/10/14/20dp set, completed
// with largeIncreased 20dp, extraLarge 28dp, extraLargeIncreased 32dp and
// extraExtraLarge 48dp. Only corners change; sizes and padding stay put.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
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
    // Emphasized styles are separate fields; without the UI font they would
    // render Persian text in the platform default (no Vazirmatn glyphs).
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = font),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = font),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = font),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = font),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = font),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = font),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = font),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = font),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = font),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = font),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = font),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = font),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = font),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = font),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = font),
)

@Composable
fun GanjoorakTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light, ThemeMode.Paper -> false
    }
    val colors: ColorScheme = when {
        settings.theme == ThemeMode.Paper -> PaperColors
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
