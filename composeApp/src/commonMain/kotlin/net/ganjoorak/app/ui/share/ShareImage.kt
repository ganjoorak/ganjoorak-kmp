package net.ganjoorak.app.ui.share

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/** Image templates from the website's share dialog. */
enum class ShareTheme(
    val label: String,
    val background: Color,
    val accent: Color,
    val title: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val brand: Color,
) {
    Night(
        "شب", Color(0xFF111111), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.92f),
        Color.White.copy(alpha = 0.90f), Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.52f),
    ),
    Paper(
        "کاغذ", Color(0xFFF4EAD7), Color(0x1F734E26), Color(0xFF3D2B1F),
        Color(0xFF493527), Color(0xAD3D2B1F), Color(0x2E3D2B1F), Color(0x8A3D2B1F),
    ),
    Ink(
        "مرکب", Color(0xFFF7F7F4), Color(0x1414181C), Color(0xFF17191D),
        Color(0xFF202329), Color(0x9E17191D), Color(0x2917191D), Color(0x8017191D),
    ),
}

enum class ShareLayout(val label: String) { Single("تک‌ستون"), Couplet("دوستون") }

const val SHARE_IMAGE_WIDTH = 1080
const val SHARE_IMAGE_HEIGHT = 1350
private const val SIDE_MARGIN = 105f
private const val TEXT_TOP = 340f
private const val TEXT_BOTTOM = SHARE_IMAGE_HEIGHT - 185f

/** A measurer at 1px = 1sp so the image is laid out in its own pixel space, independent of the screen. */
fun shareTextMeasurer(resolver: FontFamily.Resolver) =
    TextMeasurer(resolver, Density(1f), LayoutDirection.Rtl)

/** Draws the 1080×1350 share card the website produces with canvas. */
fun renderShareImage(
    measurer: TextMeasurer,
    title: String,
    poet: String,
    lines: List<String>,
    theme: ShareTheme,
    font: FontFamily,
    layout: ShareLayout,
): ImageBitmap {
    val width = SHARE_IMAGE_WIDTH.toFloat()
    val height = SHARE_IMAGE_HEIGHT.toFloat()
    val bitmap = ImageBitmap(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Rtl, Canvas(bitmap), Size(width, height)) {
        drawRect(theme.background)
        drawRect(
            Brush.linearGradient(
                0f to theme.accent,
                0.45f to Color.White.copy(alpha = 0.02f),
                1f to theme.accent,
                start = Offset.Zero,
                end = Offset(width, height),
            ),
        )
        drawRect(theme.border, topLeft = Offset(56f, 56f), size = Size(width - 112f, height - 112f), style = Stroke(2f))

        val contentWidth = (width - 2 * SIDE_MARGIN).toInt()
        val titleLayout = measurer.measure(
            title,
            style(font, 50, theme.title, FontWeight.Bold, lineHeightRatio = 1.24f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints.fixedWidth(contentWidth),
        )
        drawText(titleLayout, topLeft = Offset(SIDE_MARGIN, 105f))
        val poetLayout = measurer.measure(poet, style(font, 31, theme.muted), maxLines = 1, constraints = Constraints.fixedWidth(contentWidth))
        drawText(poetLayout, topLeft = Offset(SIDE_MARGIN, 105f + titleLayout.size.height + 14f))

        when (layout) {
            ShareLayout.Single -> drawSingleColumn(measurer, lines, theme, font, contentWidth)
            ShareLayout.Couplet -> drawCouplets(measurer, lines, theme, font, contentWidth)
        }

        val brand = measurer.measure("ganjoorak.ir", style(font, 26, theme.brand), constraints = Constraints.fixedWidth(contentWidth))
        drawText(brand, topLeft = Offset(SIDE_MARGIN, height - 150f))
    }
    return bitmap
}

private fun style(font: FontFamily, size: Int, color: Color, weight: FontWeight = FontWeight.Normal, lineHeightRatio: Float = 1.75f) =
    TextStyle(
        fontFamily = font,
        fontSize = size.sp,
        lineHeight = (size * lineHeightRatio).sp,
        fontWeight = weight,
        color = color,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl,
    )

private fun DrawScope.drawSingleColumn(measurer: TextMeasurer, lines: List<String>, theme: ShareTheme, font: FontFamily, width: Int) {
    val maxHeight = TEXT_BOTTOM - TEXT_TOP
    val text = lines.joinToString("\n")
    var result: TextLayoutResult? = null
    for (size in 42 downTo 24 step 2) {
        val candidate = measurer.measure(text, style(font, size, theme.text), constraints = Constraints.fixedWidth(width))
        result = candidate
        if (candidate.size.height <= maxHeight) break
    }
    var layout = result ?: return
    if (layout.size.height > maxHeight) {
        val lineHeight = 24 * 1.75f
        layout = measurer.measure(
            text,
            style(font, 24, theme.text),
            maxLines = maxOf(1, (maxHeight / lineHeight).toInt()),
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints.fixedWidth(width),
        )
    }
    val top = maxOf(TEXT_TOP, size.height / 2 - layout.size.height / 2)
    drawText(layout, topLeft = Offset(SIDE_MARGIN, top))
}

private fun DrawScope.drawCouplets(measurer: TextMeasurer, lines: List<String>, theme: ShareTheme, font: FontFamily, width: Int) {
    val maxHeight = TEXT_BOTTOM - TEXT_TOP
    val gap = 48
    val columnWidth = (width - gap) / 2
    val pairs = lines.chunked(2)

    fun rows(size: Int) = pairs.map { pair ->
        val textStyle = style(font, size, theme.text, lineHeightRatio = 1.62f)
        val right = measurer.measure(pair[0], textStyle, constraints = Constraints.fixedWidth(columnWidth))
        val left = pair.getOrNull(1)?.let { measurer.measure(it, textStyle, constraints = Constraints.fixedWidth(columnWidth)) }
        Triple(right, left, maxOf(right.size.height, left?.size?.height ?: 0))
    }

    var chosen = rows(38)
    var pairGap = 38 * 1.62f * 0.52f
    for (size in 38 downTo 22 step 2) {
        val candidate = rows(size)
        val candidateGap = size * 1.62f * 0.52f
        chosen = candidate
        pairGap = candidateGap
        if (candidate.sumOf { it.third } + candidateGap * (candidate.size - 1).coerceAtLeast(0) <= maxHeight) break
    }
    var used = 0f
    val visible = chosen.takeWhile { row ->
        val extra = if (used > 0f) pairGap else 0f
        (used + extra + row.third <= maxHeight || used == 0f).also { fits -> if (fits) used += extra + row.third }
    }
    var y = maxOf(TEXT_TOP, size.height / 2 - used / 2)
    val rightX = SIDE_MARGIN + columnWidth + gap
    visible.forEach { (right, left, rowHeight) ->
        drawText(right, topLeft = Offset(rightX, y))
        left?.let { drawText(it, topLeft = Offset(SIDE_MARGIN, y)) }
        y += rowHeight + pairGap
    }
}
