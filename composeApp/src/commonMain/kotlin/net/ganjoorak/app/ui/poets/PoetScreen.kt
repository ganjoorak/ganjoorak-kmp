package net.ganjoorak.app.ui.poets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.PoemSummary
import net.ganjoorak.app.data.PoetDetail
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.util.hijriLifespan

@Composable
fun PoetScreen(
    poetId: Int,
    onBack: () -> Unit,
    onOpenPoem: (Int) -> Unit,
    onOpenCategory: (Int) -> Unit,
    onReadInFeed: () -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("poet-$poetId") { graph.repository.poet(poetId) }
    val state by loader.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var loadingRandom by remember { mutableStateOf(false) }
    val detail = (state as? Loadable.Ready)?.value

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(detail?.poet?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (val s = state) {
                is Loadable.Loading -> LoadingBox()
                is Loadable.Error -> ErrorBox(s.message, onRetry = loader::reload)
                is Loadable.Ready -> PoetContent(
                    detail = s.value,
                    loadingRandom = loadingRandom,
                    onRandom = {
                        loadingRandom = true
                        scope.launch {
                            try {
                                onOpenPoem(graph.repository.randomPoem(s.value.poet.id).id)
                            } catch (e: GanjoorException) {
                                snackbar.showSnackbar(e.message ?: "")
                            } finally {
                                loadingRandom = false
                            }
                        }
                    },
                    onReadInFeed = {
                        graph.settings.update { it.copy(feedPoetId = s.value.poet.id, feedPoetName = s.value.poet.name) }
                        onReadInFeed()
                    },
                    onOpenPoem = onOpenPoem,
                    onOpenCategory = onOpenCategory,
                )
            }
        }
    }
}

@Composable
private fun PoetContent(
    detail: PoetDetail,
    loadingRandom: Boolean,
    onRandom: () -> Unit,
    onReadInFeed: () -> Unit,
    onOpenPoem: (Int) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val poet = detail.poet
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PoetAvatar(poet.name, poet.imageUrl, 104.dp)
                Text(poet.name, style = MaterialTheme.typography.headlineSmall)
                hijriLifespan(poet.birthYear, poet.deathYear)?.let { SupportingText(it) }
                val places = listOfNotNull(
                    poet.birthPlace?.let { "زادگاه: $it" },
                    poet.deathPlace?.let { "درگذشت در: $it" },
                ).joinToString(" · ")
                if (places.isNotEmpty()) SupportingText(places)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    FilledTonalButton(onClick = onRandom, enabled = !loadingRandom) {
                        if (loadingRandom) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text("شعر تصادفی", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = onReadInFeed) {
                        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("خواندن در خانه", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        poet.description?.let { description ->
            item(key = "description") { ExpandableText(description) }
        }
        categoryItems(detail.root.children, onOpenCategory)
        poemItems(detail.root.poems, onOpenPoem)
    }
}

@Composable
private fun ExpandableText(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Justify,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

fun LazyListScope.categoryItems(categories: List<CategoryRef>, onOpenCategory: (Int) -> Unit) {
    if (categories.isEmpty()) return
    item(key = "cats-title") { SectionTitle("آثار") }
    items(categories, key = { "c${it.id}" }) { category ->
        ListItem(
            headlineContent = { Text(category.title) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable { onOpenCategory(category.id) },
        )
    }
}

fun LazyListScope.poemItems(poems: List<PoemSummary>, onOpenPoem: (Int) -> Unit) {
    if (poems.isEmpty()) return
    item(key = "poems-title") { SectionTitle("اشعار") }
    items(poems, key = { "p${it.id}" }) { poem ->
        ListItem(
            headlineContent = { Text(poem.title) },
            supportingContent = poem.excerpt?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable { onOpenPoem(poem.id) },
        )
    }
}
