package net.ganjoorak.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import net.ganjoorak.app.data.local.AppSettings
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.gandom
import net.ganjoorak.app.resources.nahid
import net.ganjoorak.app.resources.parastoo
import net.ganjoorak.app.resources.sahel
import net.ganjoorak.app.resources.samim
import net.ganjoorak.app.resources.shabnam
import net.ganjoorak.app.resources.tanha
import net.ganjoorak.app.resources.vazirmatn
import org.jetbrains.compose.resources.Font

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5E4CF),
    onPrimaryContainer = Color(0xFF2E1A06),
    secondary = Color(0xFF6D5C4A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE5DA),
    onSecondaryContainer = Color(0xFF261A0E),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF161412),
    surface = Color(0xFFFAFAF9),
    onSurface = Color(0xFF161412),
    surfaceVariant = Color(0xFFEFECE8),
    onSurfaceVariant = Color(0xFF5B5650),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F4F1),
    surfaceContainer = Color(0xFFF1EEEA),
    surfaceContainerHigh = Color(0xFFEBE8E3),
    surfaceContainerHighest = Color(0xFFE5E1DC),
    outline = Color(0xFFC9C2BA),
    outlineVariant = Color(0xFFE4DED7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3BA80),
    onPrimary = Color(0xFF432C0E),
    primaryContainer = Color(0xFF5C4222),
    onPrimaryContainer = Color(0xFFFFDDB4),
    secondary = Color(0xFFD7C3AE),
    onSecondary = Color(0xFF3A2E21),
    secondaryContainer = Color(0xFF3A3129),
    onSecondaryContainer = Color(0xFFF3DFC9),
    background = Color(0xFF0E0E0D),
    onBackground = Color(0xFFEDEAE6),
    surface = Color(0xFF0E0E0D),
    onSurface = Color(0xFFEDEAE6),
    surfaceVariant = Color(0xFF2A2825),
    onSurfaceVariant = Color(0xFFB9B2AA),
    surfaceContainerLowest = Color(0xFF090908),
    surfaceContainerLow = Color(0xFF151413),
    surfaceContainer = Color(0xFF1A1918),
    surfaceContainerHigh = Color(0xFF22211F),
    surfaceContainerHighest = Color(0xFF2B2A28),
    outline = Color(0xFF59544E),
    outlineVariant = Color(0xFF33302C),
)

private val PaperColors = lightColorScheme(
    primary = Color(0xFF785A32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6D6B8),
    onPrimaryContainer = Color(0xFF2B1D08),
    secondary = Color(0xFF6E604E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E0CD),
    onSecondaryContainer = Color(0xFF2A2116),
    background = Color(0xFFF4F0E5),
    onBackground = Color(0xFF403428),
    surface = Color(0xFFF4F0E5),
    onSurface = Color(0xFF403428),
    surfaceVariant = Color(0xFFEAE3D3),
    onSurfaceVariant = Color(0xFF6E604E),
    surfaceContainerLowest = Color(0xFFFAF7ED),
    surfaceContainerLow = Color(0xFFF0EBDE),
    surfaceContainer = Color(0xFFEEE8D8),
    surfaceContainerHigh = Color(0xFFE8E1CF),
    surfaceContainerHighest = Color(0xFFE2DAC6),
    outline = Color(0xFFC4B79D),
    outlineVariant = Color(0xFFD6CCB7),
)

/** How poem text is rendered; derived from [AppSettings]. */
@Immutable
data class PoemStyle(
    val fontFamily: FontFamily,
    val fontSize: TextUnit,
    val layout: CoupletLayout,
    val showVerseNumbers: Boolean,
    val autoScroll: Boolean,
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
    )

    SystemBarsEffect(lightBackground = !dark)
    MaterialTheme(colorScheme = colors, typography = Typography().withFont(uiFont)) {
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
