package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Category
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.rememberLoader

@Composable
fun CategoryScreen(
    categoryId: Int,
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("cat-$categoryId") { graph.repository.category(categoryId) }
    val state by loader.state.collectAsState()
    val category = (state as? Loadable.Ready)?.value

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(category?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                is Loadable.Ready -> CategoryContent(s.value, onOpenPoem, onOpenPoet, onOpenCategory)
            }
        }
    }
}

@Composable
private fun CategoryContent(
    category: Category,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (category.ancestors.isNotEmpty()) {
            item(key = "crumbs") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    category.ancestors.forEach { ancestor ->
                        val poet = category.poet
                        AssistChip(
                            onClick = {
                                if (poet != null && ancestor.id == poet.rootCatId) onOpenPoet(poet.key) else onOpenCategory(ancestor.id)
                            },
                            label = { Text(ancestor.title) },
                        )
                    }
                }
            }
        }
        category.description?.let {
            item(key = "description") {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        categoryItems(category.children, onOpenCategory)
        poemItems(category.poems, onOpenPoem)
    }
}
