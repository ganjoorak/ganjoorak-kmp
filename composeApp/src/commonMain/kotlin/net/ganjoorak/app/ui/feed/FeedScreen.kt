package net.ganjoorak.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.poem.PoemView
import net.ganjoorak.app.ui.poem.RoundIconButton
import net.ganjoorak.app.ui.share.ShareSheet

/** The home page: one full-screen poem at a time, swiped vertically, with the site's floating controls. */
@Composable
fun FeedScreen(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val vm = viewModel { FeedViewModel(graph.repository, graph.settings) }
    val state by vm.state.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf<Poem?>(null) }

    val pager = rememberPagerState(pageCount = { state.poems.size + 1 })
    LaunchedEffect(state.generation) { pager.scrollToPage(0) }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { vm.onPageVisible(it) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { index -> state.poems.getOrNull(index)?.ref?.key ?: "loader" },
        ) { index ->
            val poem = state.poems.getOrNull(index)
            when {
                poem != null -> PoemView(
                    poem = poem,
                    isSaved = saved.any { it.ref == poem.ref },
                    player = graph.player,
                    onToggleSave = { graph.library.toggleSaved(poem) },
                    onShare = { sharing = poem },
                    onOpenPoet = onOpenPoet,
                    onOpenCategory = onOpenCategory,
                )
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::retry)
                else -> LoadingBox()
            }
        }

        Column(
            Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(Icons.Filled.Menu, "منو", onOpenMenu, size = 40.dp)
            RoundIconButton(Icons.Filled.Search, "جستجو", onOpenSearch, size = 40.dp)
        }

        if (wide) {
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RoundIconButton(
                    Icons.Filled.KeyboardArrowUp,
                    "شعر قبلی",
                    { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    size = 40.dp,
                    enabled = pager.currentPage > 0,
                )
                RoundIconButton(
                    Icons.Filled.KeyboardArrowDown,
                    "شعر بعدی",
                    { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    size = 40.dp,
                    enabled = pager.currentPage < state.poems.size,
                )
            }
        }
    }

    sharing?.let { poem -> ShareSheet(poem, onDismiss = { sharing = null }) }
}
