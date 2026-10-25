package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.player.PlayerController
import net.ganjoorak.app.player.PlayerState
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.toPersianDigits

/** The website's 40dp translucent round button used for every floating control. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    enabled: Boolean = true,
) {
    val color = MaterialTheme.colorScheme.onBackground
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = color.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(size * 0.45f))
    }
}

/**
 * One poem laid out like the website's poem viewer: title header, verses, floating actions at
 * the bottom-left and the recitation bar along the bottom.
 */
@Composable
fun PoemView(
    poem: Poem,
    isSaved: Boolean,
    player: PlayerController,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    footer: (LazyListScope.() -> Unit)? = null,
) {
    val style = LocalPoemStyle.current
    val playerState by player.state.collectAsState()
    val isCurrent = playerState.poemRef == poem.ref
    var highlight by rememberSaveable { mutableStateOf(true) }
    val activeVerse = playerState.activeVerseOrder.takeIf { isCurrent && highlight }
    val numbers = remember(poem) { coupletNumbers(poem.blocks) }
    val hasAudio = poem.recitations.isNotEmpty()
    val seekOnClick: ((Verse) -> Unit)? = if (isCurrent) {
        { verse -> player.seekToVerse(verse.order) }
    } else {
        null
    }
    if (style.autoScroll) FollowActiveVerse(poem, activeVerse, listState, blocksStart = 1)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= SideBySideMinWidth
        val sideBySide = style.sideBySide(maxWidth)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (hasAudio) 190.dp else 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "header") {
                PoemTitleHeader(
                    poem,
                    wide = wide,
                    onPoetClick = poem.poetKey?.let { key -> { onOpenPoet(key) } },
                    onCategoryClick = onOpenCategory?.let { open -> { crumb: CategoryRef -> open(crumb.id) } },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 20.dp, bottom = 12.dp),
                )
            }
            poemBlocks(poem, numbers, sideBySide, activeVerse, seekOnClick)
            footer?.invoke(this)
        }

        PoemActions(
            poem = poem,
            isSaved = isSaved,
            onToggleSave = onToggleSave,
            onShare = onShare,
            onOpenPoet = onOpenPoet,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 14.dp, bottom = if (hasAudio) 84.dp else 24.dp),
        )

        if (hasAudio) {
            PoemAudioBar(
                poem = poem,
                state = playerState.takeIf { isCurrent },
                player = player,
                highlight = highlight,
                onToggleHighlight = { highlight = !highlight },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PoemActions(
    poem: Poem,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onOpenPoet: (String) -> Unit,
    modifier: Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundIconButton(
            if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            if (isSaved) "حذف از ذخیره‌ها" else "ذخیره شعر",
            onToggleSave,
        )
        RoundIconButton(Icons.Filled.Share, "اشتراک‌گذاری", onShare)
        if (poem.link.isNotBlank()) {
            RoundIconButton(Icons.AutoMirrored.Filled.OpenInNew, "مشاهده منبع", { uriHandler.openUri(poem.link) })
        }
        val poetKey = poem.poetKey
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 76.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (poetKey != null) Modifier.clickable { onOpenPoet(poetKey) } else Modifier)
                .padding(4.dp),
        ) {
            PoetAvatar(poem.poetName, poem.poetImageUrl, 52.dp)
            Text(
                poem.poetName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The site's bottom recitation bar: previous/play/next/highlight, a thin progress track and time. */
@Composable
private fun PoemAudioBar(
    poem: Poem,
    state: PlayerState?,
    player: PlayerController,
    highlight: Boolean,
    onToggleHighlight: () -> Unit,
    modifier: Modifier,
) {
    val recitations = poem.recitations
    val currentIndex = recitations.indexOfFirst { it.id == state?.recitation?.id }.coerceAtLeast(0)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { player.play(poem, recitations[currentIndex - 1]) },
                    enabled = state != null && currentIndex > 0,
                ) { Icon(Icons.Filled.FastForward, contentDescription = "خوانش قبلی") }
                IconButton(onClick = { if (state == null) player.play(poem) else player.toggle() }) {
                    when {
                        state?.isBuffering == true && state.isPlaying -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        state?.isPlaying == true -> Icon(Icons.Filled.Pause, contentDescription = "توقف")
                        else -> Icon(Icons.Filled.PlayArrow, contentDescription = "پخش")
                    }
                }
                IconButton(
                    onClick = { player.play(poem, recitations[currentIndex + 1]) },
                    enabled = state != null && currentIndex < recitations.lastIndex,
                ) { Icon(Icons.Filled.FastRewind, contentDescription = "خوانش بعدی") }
                IconButton(onClick = onToggleHighlight) {
                    Icon(
                        if (highlight) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (highlight) "خاموش کردن برجسته‌سازی" else "روشن کردن برجسته‌سازی",
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    val recitation = state?.recitation ?: recitations.first()
                    Text(
                        recitation.artist.ifBlank { "خوانش" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AudioProgress(state, player)
                }
                Text(
                    "${formatTime(state?.positionMs ?: 0)} / ${formatTime(state?.durationMs ?: 0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            if (!player.isSupported) {
                Text(
                    "در این نسخه، خوانش در مرورگر پخش می‌شود",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AudioProgress(state: PlayerState?, player: PlayerController) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state?.durationMs ?: 0
    val fraction = dragging ?: if (duration > 0) (state!!.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val color = MaterialTheme.colorScheme.onBackground
    Slider(
        value = fraction,
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            dragging?.let { if (duration > 0) player.seekTo((it * duration).toLong()) }
            dragging = null
        },
        enabled = duration > 0,
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.15f),
        ),
        modifier = Modifier.fillMaxWidth().height(24.dp),
    )
}

@Composable
private fun FollowActiveVerse(poem: Poem, activeVerse: Int?, listState: LazyListState, blocksStart: Int) {
    LaunchedEffect(activeVerse) {
        val order = activeVerse ?: return@LaunchedEffect
        val blockIndex = poem.blocks.indexOfFirst { block -> block.verses.any { it.order == order } }
        if (blockIndex < 0) return@LaunchedEffect
        val target = blocksStart + blockIndex
        val layout = listState.layoutInfo
        val visible = layout.visibleItemsInfo.firstOrNull { it.index == target }
        val viewport = layout.viewportEndOffset - layout.viewportStartOffset
        val comfortable = visible != null && visible.offset > viewport / 6 && visible.offset + visible.size < viewport * 2 / 3
        if (!comfortable) listState.animateScrollToItem(target, scrollOffset = -viewport / 3)
    }
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}".toPersianDigits()
}
