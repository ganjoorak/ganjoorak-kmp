package net.ganjoorak.app.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.poem.PoemHeader
import net.ganjoorak.app.ui.share.ShareSheet
import net.ganjoorak.app.util.toPersianDigits
import net.ganjoorak.app.ui.poem.coupletNumbers
import net.ganjoorak.app.ui.poem.poemBlocks
import net.ganjoorak.app.ui.poem.sideBySide
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.normalizeForSearch

@Composable
fun FeedScreen(
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val graph = AppGraph.instance
    val vm = viewModel { FeedViewModel(graph.repository, graph.settings) }
    val state by vm.state.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    val player by graph.player.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pickPoet by rememberSaveable { mutableStateOf(false) }
    var sharing by remember { mutableStateOf<Poem?>(null) }

    val pager = rememberPagerState(pageCount = { state.poems.size + 1 })
    LaunchedEffect(state.generation) { pager.scrollToPage(0) }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { vm.onPageVisible(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("گنجورک", style = MaterialTheme.typography.titleLarge)
                        Text(
                            when (state.followed.size) {
                                0 -> "شعر تصادفی از همهٔ شاعران"
                                1 -> state.followed.first().name
                                else -> "${state.followed.size.toPersianDigits()} شاعر برگزیده"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.followed.isNotEmpty()) {
                        IconButton(onClick = { graph.settings.update { it.copy(followedPoets = emptyList()) } }) {
                            Icon(Icons.Filled.Close, contentDescription = "همهٔ شاعران")
                        }
                    }
                    IconButton(onClick = { pickPoet = true }) {
                        Icon(Icons.Filled.PersonSearch, contentDescription = "شاعران صفحه اصلی")
                    }
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "شعرهای تازه")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                },
            )
        },
    ) { padding ->
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().padding(padding),
            beyondViewportPageCount = 1,
            // The loader's key must change with its position: with a fixed key the pager keeps the
            // loader in view as poems are inserted before it, so the first poem never shows.
            key = { index -> state.poems.getOrNull(index)?.ref?.key ?: "loader-$index" },
        ) { index ->
            val poem = state.poems.getOrNull(index)
            when {
                poem != null -> FeedPage(
                    poem = poem,
                    isSaved = saved.any { it.ref == poem.ref },
                    isPlaying = player.poemRef == poem.ref && player.isPlaying,
                    activeVerse = player.activeVerseOrder.takeIf { player.poemRef == poem.ref },
                    onToggleSave = { graph.library.toggleSaved(poem) },
                    onShare = { sharing = poem },
                    onPlay = {
                        if (player.poemRef == poem.ref) graph.player.toggle() else graph.player.play(poem)
                    },
                    onOpen = { onOpenPoem(poem.ref) },
                    onOpenPoet = onOpenPoet,
                    onOpenCategory = { crumb: CategoryRef -> onOpenCategory(crumb.id) }.takeIf { poem.source == PoetSource.Ganjoor },
                )
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::retry)
                else -> LoadingBox()
            }
        }
    }

    if (pickPoet) FeedPoetsSheet(onDismiss = { pickPoet = false })
    sharing?.let { poem -> ShareSheet(poem, onDismiss = { sharing = null }) }
}

@Composable
private fun FeedPage(
    poem: Poem,
    isSaved: Boolean,
    isPlaying: Boolean,
    activeVerse: Int?,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: ((CategoryRef) -> Unit)?,
) {
    val style = LocalPoemStyle.current
    val numbers = remember(poem) { coupletNumbers(poem.blocks) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val sideBySide = style.sideBySide(minOf(maxWidth, 900.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                PoemHeader(poem, onPoetClick = onOpenPoet, onCategoryClick = onOpenCategory)
            }
            poemBlocks(poem, numbers, sideBySide, activeVerse, onVerseClick = null)
            item(key = "actions") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    FilledTonalIconButton(onClick = onToggleSave) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "حذف از نشان‌ها" else "نشان کردن",
                        )
                    }
                    FilledTonalIconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "هم‌رسانی")
                    }
                    if (poem.recitations.isNotEmpty()) {
                        FilledTonalIconButton(onClick = onPlay) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "توقف خوانش" else "پخش خوانش",
                            )
                        }
                    }
                    FilledTonalIconButton(onClick = onOpen) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "باز کردن شعر")
                    }
                }
            }
            item(key = "hint") {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    Text(
                        "برای شعر بعدی بکشید",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
