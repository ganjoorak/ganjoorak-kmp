package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetGroup
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.util.hijriLifespan
import net.ganjoorak.app.util.normalizeForSearch
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun PoetsScreen(onOpenPoet: (Int) -> Unit) {
    val graph = AppGraph.instance
    val loader = rememberLoader("poet-groups") { graph.repository.poetGroups() }
    val state by loader.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("شاعران") }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("جستجوی شاعر") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "پاک کردن") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val s = state) {
                is Loadable.Loading -> LoadingBox()
                is Loadable.Error -> ErrorBox(s.message, onRetry = loader::reload)
                is Loadable.Ready -> {
                    val needle = query.normalizeForSearch()
                    val groups = remember(s.value, needle) { filterGroups(s.value, needle) }
                    if (groups.isEmpty()) {
                        MessageBox(Icons.Filled.SearchOff, "شاعری با این نام پیدا نشد")
                    } else {
                        PoetGrid(groups, onOpenPoet)
                    }
                }
            }
        }
    }
}

private fun filterGroups(groups: List<PoetGroup>, needle: String): List<PoetGroup> {
    if (needle.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val poets = group.poets.filter {
            needle in it.name.normalizeForSearch() || needle in it.nickname.orEmpty().normalizeForSearch()
        }
        if (poets.isEmpty()) null else group.copy(poets = poets)
    }
}

@Composable
private fun PoetGrid(groups: List<PoetGroup>, onOpenPoet: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        groups.forEach { group ->
            item(key = "h-${group.title}", span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("${group.title} · ${group.poets.size.toPersianDigits()} شاعر")
            }
            items(group.poets, key = { "${group.title}-${it.id}" }) { poet ->
                PoetRow(poet, onClick = { onOpenPoet(poet.id) })
            }
        }
    }
}

@Composable
fun PoetRow(poet: Poet, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(poet.name) },
        supportingContent = hijriLifespan(poet.birthYear, poet.deathYear)?.let { { Text(it) } },
        leadingContent = { PoetAvatar(poet.name, poet.imageUrl, 48.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun SupportingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
