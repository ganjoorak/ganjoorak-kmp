package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.ui.theme.PoemStyle
import net.ganjoorak.app.util.toPersianDigits

/** Minimum content width at which couplets are laid out as two columns in [CoupletLayout.Auto]. */
val SideBySideMinWidth: Dp = 600.dp

fun PoemStyle.sideBySide(width: Dp): Boolean = when (layout) {
    CoupletLayout.Auto -> width >= SideBySideMinWidth
    CoupletLayout.Stacked -> false
    CoupletLayout.SideBySide -> true
}

/** 1-based couplet numbers per block (0 for blocks that aren't couplets). */
fun coupletNumbers(blocks: List<PoemBlock>): IntArray {
    var n = 0
    return IntArray(blocks.size) { i -> if (blocks[i] is PoemBlock.Couplet) ++n else 0 }
}

/**
 * The website's title block: large centered title, a short fading rule, then the poet and the
 * book (each tappable) in a muted line.
 */
@Composable
fun PoemTitleHeader(
    poem: Poem,
    wide: Boolean,
    onPoetClick: (() -> Unit)?,
    onCategoryClick: ((CategoryRef) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 64.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            poem.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (wide) 34.sp else 27.sp,
                lineHeight = if (wide) 44.sp else 36.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.onBackground.copy(alpha = 0.95f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 640.dp),
        )
        Box(
            Modifier
                .padding(vertical = 12.dp)
                .width(80.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.onBackground.copy(alpha = 0.3f), Color.Transparent),
                    ),
                ),
        )
        val crumbs = poem.breadcrumbs.drop(1).filter { it.title != poem.title }
        FlowRow(horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.Center) {
            val lineStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
            val muted = colors.onBackground.copy(alpha = 0.75f)
            Text(
                poem.poetName,
                style = lineStyle,
                color = muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (onPoetClick != null) Modifier.clickable(onClick = onPoetClick) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            crumbs.forEach { crumb ->
                Text("،", style = lineStyle, color = muted, modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    crumb.title,
                    style = lineStyle,
                    color = colors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (onCategoryClick != null && crumb.id > 0) Modifier.clickable { onCategoryClick(crumb) } else Modifier,
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        poem.summary?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).widthIn(max = 640.dp),
            )
        }
    }
}

/** Adds one lazy item per [PoemBlock]; keys are `"b<index>"`. */
fun LazyListScope.poemBlocks(
    poem: Poem,
    numbers: IntArray,
    sideBySide: Boolean,
    activeVerse: Int?,
    onVerseClick: ((Verse) -> Unit)?,
) {
    itemsIndexed(poem.blocks, key = { index, _ -> "b$index" }) { index, block ->
        PoemBlockView(block, numbers.getOrElse(index) { 0 }, sideBySide, activeVerse, onVerseClick)
    }
}

@Composable
fun PoemBlockView(
    block: PoemBlock,
    number: Int,
    sideBySide: Boolean,
    activeVerse: Int?,
    onVerseClick: ((Verse) -> Unit)?,
) {
    if (block is PoemBlock.Break) {
        Spacer(Modifier.height(20.dp))
        return
    }
    val style = LocalPoemStyle.current
    val fontSize = if (sideBySide) style.fontSize * 1.15f else style.fontSize
    val base = TextStyle(
        fontFamily = style.fontFamily,
        fontSize = fontSize,
        lineHeight = fontSize * 1.9f,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (sideBySide) 6.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val showNumber = style.showVerseNumbers
        if (showNumber) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                if (number > 0) {
                    Text(
                        number.toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            when (block) {
                is PoemBlock.Couplet -> if (sideBySide && block.second != null) {
                    Row(Modifier.fillMaxWidth().widthIn(max = 1100.dp), verticalAlignment = Alignment.CenterVertically) {
                        VerseText(block.first, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.weight(1f))
                        Spacer(Modifier.width(48.dp))
                        VerseText(block.second, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.weight(1f))
                    }
                } else {
                    VerseText(block.first, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.fillMaxWidth())
                    block.second?.let { VerseText(it, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.fillMaxWidth()) }
                    Spacer(Modifier.height(14.dp))
                }
                is PoemBlock.Centered -> block.verses.forEach {
                    VerseText(it, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.fillMaxWidth())
                }
                is PoemBlock.Line -> VerseText(block.verse, base, TextAlign.Center, activeVerse, onVerseClick, Modifier.fillMaxWidth())
                is PoemBlock.Prose -> VerseText(
                    block.verse,
                    base.copy(lineHeight = fontSize * 1.7f),
                    TextAlign.Justify,
                    activeVerse,
                    onVerseClick,
                    Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(vertical = 6.dp),
                )
                is PoemBlock.Note -> VerseText(
                    block.verse,
                    base.copy(fontSize = fontSize * 0.85f, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    TextAlign.Center,
                    activeVerse,
                    onVerseClick,
                    Modifier.fillMaxWidth(),
                )
                PoemBlock.Break -> Unit
            }
        }
        if (showNumber) Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun VerseText(
    verse: Verse,
    style: TextStyle,
    align: TextAlign,
    activeVerse: Int?,
    onVerseClick: ((Verse) -> Unit)?,
    modifier: Modifier,
) {
    val active = activeVerse == verse.order
    val dimmed = activeVerse != null && !active
    val onBackground = MaterialTheme.colorScheme.onBackground
    Text(
        verse.text,
        style = style.copy(
            color = when {
                active -> onBackground
                dimmed -> style.color.copy(alpha = 0.42f)
                else -> style.color
            },
            fontWeight = if (active) FontWeight.Medium else style.fontWeight,
        ),
        textAlign = align,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (active) Modifier.background(onBackground.copy(alpha = 0.08f)) else Modifier)
            .then(if (onVerseClick != null) Modifier.clickable { onVerseClick(verse) } else Modifier)
            .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}
