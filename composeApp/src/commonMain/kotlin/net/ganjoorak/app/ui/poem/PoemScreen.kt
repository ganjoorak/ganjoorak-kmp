package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.ui.share.ShareSheet

@Composable
fun PoemScreen(
    ref: PoemRef,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("poem-${ref.key}") { graph.repository.poem(ref) }
    val state by loader.state.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    var sharing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is Loadable.Loading -> LoadingBox()
            is Loadable.Error -> ErrorBox(s.message, onRetry = loader::reload)
            is Loadable.Ready -> {
                val poem = s.value
                PoemView(
                    poem = poem,
                    isSaved = saved.any { it.ref == poem.ref },
                    player = graph.player,
                    onToggleSave = { graph.library.toggleSaved(poem) },
                    onShare = { sharing = true },
                    onOpenPoet = onOpenPoet,
                    onOpenCategory = onOpenCategory.takeIf { poem.source == PoetSource.Ganjoor },
                    footer = if (poem.previousId != null || poem.nextId != null) {
                        {
                            item(key = "nav") {
                                Row(
                                    Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 24.dp, vertical = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val previous = poem.previousId
                                    val next = poem.nextId
                                    OutlinedButton(onClick = { previous?.let { onOpenPoem(PoemRef(PoetSource.Ganjoor, it)) } }, enabled = previous != null) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("شعر پیشین", modifier = Modifier.padding(start = 6.dp))
                                    }
                                    OutlinedButton(onClick = { next?.let { onOpenPoem(PoemRef(PoetSource.Ganjoor, it)) } }, enabled = next != null) {
                                        Text("شعر بعدی", modifier = Modifier.padding(end = 6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    },
                )
                if (sharing) ShareSheet(poem, onDismiss = { sharing = false })
            }
        }

        Column(
            Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", onBack, size = 40.dp)
            RoundIconButton(Icons.Filled.Search, "جستجو", onOpenSearch, size = 40.dp)
        }
    }
}
