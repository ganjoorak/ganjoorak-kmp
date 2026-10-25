package net.ganjoorak.app.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun SavedScreen(onOpenMenu: () -> Unit, onOpenPoem: (PoemRef) -> Unit) {
    val library = AppGraph.instance.library
    val saved by library.savedPoems.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (saved.isEmpty()) "نشان‌شده‌ها" else "نشان‌شده‌ها (${saved.size.toPersianDigits()})") },
                navigationIcon = { IconButton(onClick = onOpenMenu) { Icon(Icons.Filled.Menu, contentDescription = "منو") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (saved.isEmpty()) {
                MessageBox(
                    Icons.Filled.FavoriteBorder,
                    "هنوز شعری ذخیره نکرده‌اید",
                    body = "با زدن قلب روی هر شعر، آن را این‌جا نگه دارید تا بدون جستجو به آن برگردید",
                )
            } else {
                LazyColumn(Modifier.widthIn(max = 840.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(saved, key = { it.ref.key }) { poem ->
                        ListItem(
                            overlineContent = { Text(poem.poetName) },
                            headlineContent = { Text(poem.title) },
                            supportingContent = { Text(poem.excerpt, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                IconButton(onClick = { library.remove(poem.ref) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onOpenPoem(poem.ref) },
                        )
                    }
                }
            }
        }
    }
}
