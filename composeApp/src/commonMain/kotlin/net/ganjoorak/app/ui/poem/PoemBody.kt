package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import net.ganjoorak.app.data.account.quoteLineIndex
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.components.PoetAvatar
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
 * The site's title section: an optional eyebrow (the feed switch), the poem title, the poet line
 * and the book trail.
 */
@Composable
fun PoemHeader(
    poem: Poem,
    onPoetClick: ((String) -> Unit)?,
    onCategoryClick: ((CategoryRef) -> Unit)?,
    modifier: Modifier = Modifier,
    eyebrow: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (eyebrow != null) {
            eyebrow()
            Spacer(Modifier.height(6.dp))
        }
        Text(
            poem.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = LocalPoemStyle.current.fontFamily),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        // One muted line: the poet (their portrait is already on the bar) and the book trail.
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val poetClick = poem.poetKey?.let { key -> onPoetClick?.let { { it(key) } } }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            Text(
                poem.poetName,
                style = MaterialTheme.typography.labelLarge,
                color = muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (poetClick != null) Modifier.clickable(onClick = poetClick) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            poem.breadcrumbs.drop(1).forEach { crumb ->
                Text("›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(
                    crumb.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (onCategoryClick != null && crumb.id > 0) Modifier.clickable { onCategoryClick(crumb) } else Modifier)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        if (usesSideNavigation) {
            val chrome = LocalPoemChromeHost.current.current
            if (chrome != null && chrome.poem.ref == poem.ref) {
                Spacer(Modifier.height(6.dp))
                DesktopPoemActionRow(chrome)
            }
        }
    }
}

/**
 * Desktop-only, in-flow under the title: a low-emphasis M3E-style horizontal
 * toolbar (1.9.0 only: [Surface], [IconButton], [FilledIconButton]). Poet, like
 * and share always; play is featured and only shown when the poem has audio.
 */
@Composable
private fun DesktopPoemActionRow(chrome: PoemChrome, modifier: Modifier = Modifier) {
    val poem = chrome.poem
    val colors = MaterialTheme.colorScheme
    val poetKey = poem.poetKey
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.surfaceContainer,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (poetKey != null && chrome.onOpenPoet != null) {
                IconButton(onClick = { chrome.onOpenPoet.invoke(poetKey) }) {
                    PoetAvatar(poem.poetName, poem.poetImageUrl, 28.dp)
                }
            }
            IconButton(onClick = chrome.onToggleSave) {
                Icon(
                    if (chrome.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (chrome.isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
                    tint = if (chrome.isSaved) colors.error else colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = chrome.onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "هم‌رسانی",
                    tint = colors.onSurfaceVariant,
                )
            }
            if (poem.recitations.isNotEmpty()) {
                FilledIconButton(
                    onClick = chrome.onPlay,
                    colors = if (chrome.isPlaying) {
                        IconButtonDefaults.filledIconButtonColors()
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    },
                ) {
                    Icon(
                        if (chrome.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (chrome.isPlaying) "توقف خوانش" else "پخش خوانش",
                    )
                }
            }
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
    selectedLines: IntRange? = null,
    onVerseLongClick: ((Verse) -> Unit)? = null,
) {
    itemsIndexed(poem.blocks, key = { index, _ -> "b$index" }) { index, block ->
        PoemBlockView(
            block,
            numbers.getOrElse(index) { 0 },
            sideBySide,
            activeVerse,
            onVerseClick,
            selectedLines = selectedLines,
            onVerseLongClick = onVerseLongClick,
            isLineSelected = { verse ->
                val line = poem.quoteLineIndex(verse)
                line != null && selectedLines != null && line in selectedLines
            },
        )
    }
}

@Composable
fun PoemBlockView(
    block: PoemBlock,
    number: Int,
    sideBySide: Boolean,
    activeVerse: Int?,
    onVerseClick: ((Verse) -> Unit)?,
    selectedLines: IntRange? = null,
    onVerseLongClick: ((Verse) -> Unit)? = null,
    isLineSelected: (Verse) -> Boolean = { false },
) {
    if (block is PoemBlock.Break) {
        Spacer(Modifier.height(18.dp))
        return
    }
    val style = LocalPoemStyle.current
    val base = TextStyle(
        fontFamily = style.fontFamily,
        fontSize = style.fontSize,
        lineHeight = style.fontSize * 1.9f,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val showNumber = style.showVerseNumbers
        if (showNumber) {
            Text(
                if (number > 0) number.toPersianDigits() else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.weight(1f)) {
            when (block) {
                is PoemBlock.Couplet -> if (sideBySide && block.second != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        VerseText(block.first, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.weight(1f))
                        Spacer(Modifier.width(24.dp))
                        VerseText(block.second, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.weight(1f))
                    }
                } else {
                    VerseText(block.first, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.fillMaxWidth())
                    block.second?.let { VerseText(it, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.fillMaxWidth()) }
                    Spacer(Modifier.padding(bottom = 6.dp))
                }
                is PoemBlock.Centered -> block.verses.forEach {
                    VerseText(it, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.fillMaxWidth())
                }
                is PoemBlock.Line -> VerseText(block.verse, base, TextAlign.Center, activeVerse, onVerseClick, onVerseLongClick, isLineSelected, Modifier.fillMaxWidth())
                is PoemBlock.Prose -> VerseText(
                    block.verse,
                    base.copy(lineHeight = style.fontSize * 1.7f),
                    TextAlign.Justify,
                    activeVerse,
                    onVerseClick,
                    onVerseLongClick,
                    isLineSelected,
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
                is PoemBlock.Note -> VerseText(
                    block.verse,
                    base.copy(fontSize = style.fontSize * 0.85f, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    TextAlign.Center,
                    activeVerse,
                    onVerseClick,
                    onVerseLongClick,
                    isLineSelected,
                    Modifier.fillMaxWidth(),
                )
                PoemBlock.Break -> Unit
            }
        }
        if (showNumber) Spacer(Modifier.width(28.dp))
    }
}

@Composable
private fun VerseText(
    verse: Verse,
    style: TextStyle,
    align: TextAlign,
    activeVerse: Int?,
    onVerseClick: ((Verse) -> Unit)?,
    onVerseLongClick: ((Verse) -> Unit)?,
    isLineSelected: (Verse) -> Boolean,
    modifier: Modifier,
) {
    val active = activeVerse == verse.order
    val selected = isLineSelected(verse)
    val dimmed = activeVerse != null && !active && !selected
    val colors = MaterialTheme.colorScheme
    val interaction = when {
        onVerseLongClick != null && onVerseClick != null ->
            Modifier.combinedClickable(onClick = { onVerseClick(verse) }, onLongClick = { onVerseLongClick(verse) })
        onVerseLongClick != null ->
            Modifier.combinedClickable(onClick = {}, onLongClick = { onVerseLongClick(verse) })
        onVerseClick != null -> Modifier.clickable { onVerseClick(verse) }
        else -> Modifier
    }
    Text(
        verse.text,
        style = style.copy(
            color = when {
                active -> colors.primary
                dimmed -> style.color.copy(alpha = 0.55f)
                else -> style.color
            },
        ),
        textAlign = align,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                when {
                    selected -> Modifier.background(colors.primary.copy(alpha = 0.16f))
                    active -> Modifier.background(colors.primary.copy(alpha = 0.10f))
                    else -> Modifier
                },
            )
            .then(interaction)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
