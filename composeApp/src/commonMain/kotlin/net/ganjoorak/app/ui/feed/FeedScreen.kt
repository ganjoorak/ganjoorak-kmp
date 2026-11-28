package net.ganjoorak.app.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.SegmentedSwitch
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.ui.components.tabSwipe
import net.ganjoorak.app.ui.player.LocalRecitationListHost
import net.ganjoorak.app.ui.poem.PoemFloatingControls
import net.ganjoorak.app.ui.poem.poemChromeBottomSpace
import net.ganjoorak.app.ui.poem.PoemHeader
import net.ganjoorak.app.ui.poem.coupletNumbers
import net.ganjoorak.app.ui.poem.poemBlocks
import net.ganjoorak.app.ui.poem.sideBySide
import net.ganjoorak.app.ui.share.ShareSheet
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.ui.theme.MotionTokens

/**
 * The home feed, as on the site: one poem per page, swipe up for the next. Above each title sit the
 * «همه / علاقه‌مندی‌ها / دنبال‌شده‌ها» switch. The last tab shows stories and posts
 * from followed users instead of poems.
 */
@Composable
fun FeedScreen(
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
    onOpenPoets: () -> Unit,
    onOpenLogin: () -> Unit = {},
) {
    val graph = AppGraph.instance
    val vm = viewModel { FeedViewModel(graph.repository, graph.settings) }
    val state by vm.state.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    val player by graph.player.state.collectAsState()
    val recitationListHost = LocalRecitationListHost.current
    var sharing by remember { mutableStateOf<Poem?>(null) }

    val eyebrow: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SegmentedSwitch(
                options = listOf(FeedMode.All, FeedMode.Following, FeedMode.Followed),
                selected = settings.feedMode,
                label = {
                    when (it) {
                        FeedMode.All -> "همه"
                        FeedMode.Following -> "علاقه‌مندی‌ها"
                        FeedMode.Followed -> "دنبال‌شده‌ها"
                    }
                },
                onSelect = graph.settings::setFeedMode,
            )
        }
    }

    var visiblePage by remember { mutableStateOf(0) }
    LaunchedEffect(state.source.mode) { visiblePage = 0 }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.fillMaxSize()) {
        // The mode switch sits centred at the top; the menu sheet opens from
        // the menu chip in the poem pill again (as before Phase A).
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
            eyebrow()
        }
        val modes = FeedMode.entries
        val modeIndex = modes.indexOf(settings.feedMode)
        Box(
            Modifier.weight(1f).fillMaxWidth().tabSwipe(
                onNext = { modes.getOrNull(modeIndex + 1)?.let(graph.settings::setFeedMode) },
                onPrevious = { modes.getOrNull(modeIndex - 1)?.let(graph.settings::setFeedMode) },
            ),
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it.source.mode },
                modifier = Modifier.fillMaxSize(),
                label = "feedMode",
                transitionSpec = {
                    // RTL: a higher tab index sits further left, so going to a higher tab
                    // brings the new pane in from the left and pushes the old one out right.
                    val from = initialState.source.mode.ordinal
                    val to = targetState.source.mode.ordinal
                    if (to > from) {
                        (fadeIn(MotionTokens.fast()) +
                            slideInHorizontally(MotionTokens.fast()) { -it }) togetherWith
                            (fadeOut(MotionTokens.fast()) +
                                slideOutHorizontally(MotionTokens.fast()) { it })
                    } else {
                        (fadeIn(MotionTokens.fast()) +
                            slideInHorizontally(MotionTokens.fast()) { it }) togetherWith
                            (fadeOut(MotionTokens.fast()) +
                                slideOutHorizontally(MotionTokens.fast()) { -it })
                    }
                },
            ) { pane ->
                when {
                    pane.isFollowedMode -> FollowedFeed(
                        onOpenLogin = onOpenLogin,
                        onOpenPoem = onOpenPoem,
                    )
                    pane.isFollowingEmpty -> FeedEmpty(
                        title = "هنوز شاعری به علاقه‌مندی‌ها اضافه نکرده‌اید",
                        body = "شاعران را به علاقه‌مندی‌ها اضافه کنید تا این فید فقط از شعرهای آن‌ها پر شود. فید «همه» همچنان از همهٔ شاعران است.",
                        primary = "دیدن شاعران" to onOpenPoets,
                        secondary = "بازگشت به همه" to { graph.settings.setFeedMode(FeedMode.All) },
                    )
                    pane.poems.isEmpty() && pane.error != null -> ErrorBox(pane.error!!, onRetry = vm::retry)
                    else -> {
                        val pager = rememberPagerState(pageCount = { pane.poems.size + 1 })
                        LaunchedEffect(pane.generation) { pager.scrollToPage(0) }
                        val current = pane.poems.getOrNull(pager.currentPage)
                        LaunchedEffect(current?.ref) { current?.let(graph.library::recordRead) }
                        LaunchedEffect(pager) {
                            snapshotFlow { pager.currentPage }.collect {
                                vm.onPageVisible(it)
                                if (pane.source.mode == state.source.mode) visiblePage = it
                            }
                        }
                        VerticalPager(
                            state = pager,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            // The loader's key must change with its position: with a fixed key the pager keeps the
                            // loader in view as poems are inserted before it, so the first poem never shows.
                            key = { index -> pane.poems.getOrNull(index)?.ref?.key ?: "loader-$index" },
                        ) { index ->
                            val poem = pane.poems.getOrNull(index)
                            when {
                                poem != null -> FeedPage(
                                    poem = poem,
                                    activeVerse = player.activeVerseOrder.takeIf { player.poemRef == poem.ref },
                                    onOpenPoet = onOpenPoet,
                                    onOpenCategory = { crumb: CategoryRef -> onOpenCategory(crumb.id) }.takeIf { poem.source == PoetSource.Ganjoor },
                                    onOpen = { onOpenPoem(poem.ref) },
                                )
                                pane.error != null -> ErrorBox(pane.error!!, onRetry = vm::retry)
                                else -> LoadingBox()
                            }
                        }
                    }
                }
            }
        }
        }

        // The controls follow the incoming tab's state; the exiting tab owns its pager
        // inside AnimatedContent and never feeds the actions shown here.
        val hidePoemActions = state.isFollowedMode || state.isFollowingEmpty
        val showPoemChrome = !usesSideNavigation || !hidePoemActions
        val current = state.poems.getOrNull(visiblePage)
        if (showPoemChrome) PoemFloatingControls(
            poem = current.takeUnless { hidePoemActions },
            isSaved = current != null && saved.any { it.ref == current.ref },
            isPlaying = current != null && player.poemRef == current.ref && player.isPlaying,
            onToggleSave = { current?.let(graph.library::toggleSaved) },
            onShare = { sharing = current },
            onPlay = {
                current?.let { poem -> if (player.poemRef == poem.ref) graph.player.toggle() else graph.player.play(poem) }
            },
            onPlayLongPress = {
                current?.let { poem ->
                    if (player.poemRef != poem.ref) graph.player.play(poem)
                    recitationListHost.open = true
                }
            },
            onOpenPoet = onOpenPoet,
            onOpenPoem = onOpenPoem,
        )
    }

    sharing?.let { poem -> ShareSheet(poem, onDismiss = { sharing = null }) }
}

@Composable
private fun FeedPage(
    poem: Poem,
    activeVerse: Int?,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: ((CategoryRef) -> Unit)?,
    onOpen: () -> Unit,
) {
    val style = LocalPoemStyle.current
    val numbers = remember(poem) { coupletNumbers(poem.blocks) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Title, poet and book trail stay pinned; only the verses scroll.
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.TopCenter,
        ) {
            PoemHeader(poem, onPoetClick = onOpenPoet, onCategoryClick = onOpenCategory, modifier = Modifier.widthIn(max = 900.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val sideBySide = style.sideBySide(minOf(maxWidth, 900.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
                contentPadding = PaddingValues(bottom = poemChromeBottomSpace()),
            ) {
                poemBlocks(poem, numbers, sideBySide, activeVerse, onVerseClick = null)
            }
        }
    }
}

/** The site's FeedModeEmpty: the switch on top, then an icon, a message and two actions. */
@Composable
internal fun FeedEmpty(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(20.dp))
        if (primary != null) {
            Button(onClick = primary.second) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(primary.first)
            }
        }
        if (secondary != null) {
            TextButton(onClick = secondary.second) { Text(secondary.first) }
        }
    }
}
