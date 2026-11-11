package net.ganjoorak.app.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.local.FollowedPoet
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetKey
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.AppTabs
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.util.toPersianDigits

enum class FavoritesSection(val label: String) {
    Poems("شعرها"),
    Poets("شاعران"),
    // Users ("کاربران") lands with item 3 (following people); do not add it yet.
}

private enum class PoemLibraryTab(val label: String) {
    Saved("ذخیره‌شده‌ها"),
    History("اخیراً دیده‌شده"),
}

/**
 * One «علاقه‌مندی‌ها» place for the site's /saved poems and /preferred-poets,
 * with a segmented control so the rail and the phone bar only need one destination.
 * The feed switch «همه / علاقه‌مندی‌ها» still reads the same preferred-poet list.
 */
@Composable
fun FavoritesScreen(
    initialSection: FavoritesSection = FavoritesSection.Poems,
    onBack: (() -> Unit)? = null,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenFeed: () -> Unit,
    onOpenPoets: () -> Unit,
    onEditFollowing: () -> Unit,
    onReadFavorites: () -> Unit,
) {
    val library = AppGraph.instance.library
    val store = AppGraph.instance.settings
    val saved by library.savedPoems.collectAsState()
    val history by library.history.collectAsState()
    val settings by store.settings.collectAsState()
    val poets = settings.followedPoets
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var poemTab by rememberSaveable { mutableStateOf(PoemLibraryTab.Saved) }

    AppPage(
        title = "علاقه‌مندی‌ها",
        subtitle = when (section) {
            FavoritesSection.Poems -> "شعرهایی که ذخیره کرده‌اید و آن‌هایی که اخیراً خوانده‌اید."
            FavoritesSection.Poets -> if (poets.isNotEmpty()) {
                "${poets.size.toPersianDigits()} شاعر. حالت «علاقه‌مندی‌ها» در صفحهٔ اصلی از همین شاعران شعر می‌آورد."
            } else {
                "شاعرانی که به علاقه‌مندی‌ها می‌افزایید اینجا جمع می‌شوند و حالت «علاقه‌مندی‌ها» در صفحهٔ اصلی از آن‌ها شعر می‌آورد."
            }
        },
        onBack = onBack,
        actions = {
            when {
                section == FavoritesSection.Poems && poemTab == PoemLibraryTab.History && history.isNotEmpty() -> {
                    TextButton(onClick = library::clearHistory) { Text("پاک کردن") }
                }
                section == FavoritesSection.Poets && poets.isNotEmpty() -> {
                    IconButton(onClick = onOpenPoets) { Icon(Icons.Filled.PersonAdd, contentDescription = "افزودن شاعر به علاقه‌مندی‌ها") }
                }
            }
        },
        headerExtra = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTabs(
                    options = FavoritesSection.entries,
                    selected = section,
                    label = { it.label },
                    onSelect = { section = it },
                    count = { if (it == FavoritesSection.Poems) saved.size else poets.size },
                )
                if (section == FavoritesSection.Poems) {
                    AppTabs(
                        options = PoemLibraryTab.entries,
                        selected = poemTab,
                        label = { it.label },
                        onSelect = { poemTab = it },
                        count = { if (it == PoemLibraryTab.Saved) saved.size else history.size },
                    )
                } else if (poets.isNotEmpty()) {
                    Button(onClick = onReadFavorites, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("خواندن از علاقه‌مندی‌ها")
                    }
                }
            }
        },
    ) {
        when (section) {
            FavoritesSection.Poems -> poemsContent(
                tab = poemTab,
                entries = if (poemTab == PoemLibraryTab.Saved) saved else history,
                onOpenPoem = onOpenPoem,
                onOpenFeed = onOpenFeed,
                onRemove = if (poemTab == PoemLibraryTab.Saved) { poem -> library.remove(poem.ref) } else null,
            )
            FavoritesSection.Poets -> poetsContent(
                poets = poets,
                onOpenPoet = onOpenPoet,
                onOpenPoets = onOpenPoets,
                onEditFollowing = onEditFollowing,
                onUnfollow = { store.unfollow(it) },
            )
        }
    }
}

private fun LazyListScope.poemsContent(
    tab: PoemLibraryTab,
    entries: List<SavedPoem>,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenFeed: () -> Unit,
    onRemove: ((SavedPoem) -> Unit)?,
) {
    if (entries.isEmpty()) {
        item(key = "empty-$tab") {
            if (tab == PoemLibraryTab.Saved) {
                MessageBox(
                    Icons.Filled.FavoriteBorder,
                    "هنوز شعری ذخیره نکرده‌اید",
                    Modifier.height(320.dp),
                    body = "روی آیکن قلب در صفحهٔ شعر بزنید تا اینجا بماند.",
                    action = { Button(onClick = onOpenFeed) { Text("رفتن به فید") } },
                )
            } else {
                MessageBox(Icons.Filled.History, "هنوز شعری نخوانده‌اید", Modifier.height(320.dp))
            }
        }
    } else {
        items(entries, key = { "$tab-${it.ref.key}" }) { poem ->
            AppRow(
                title = poem.title,
                meta = listOf(poem.poetName, poem.excerpt).filter { it.isNotBlank() }.joinToString(" · "),
                trailing = onRemove?.let { remove ->
                    @Composable { IconButton(onClick = { remove(poem) }) { Icon(Icons.Outlined.Delete, contentDescription = "حذف") } }
                },
                onClick = { onOpenPoem(poem.ref) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.poetsContent(
    poets: List<FollowedPoet>,
    onOpenPoet: (String) -> Unit,
    onOpenPoets: () -> Unit,
    onEditFollowing: () -> Unit,
    onUnfollow: (String) -> Unit,
) {
    if (poets.isEmpty()) {
        item {
            MessageBox(
                Icons.Filled.Groups,
                "هنوز شاعری به علاقه‌مندی‌ها اضافه نکرده‌اید",
                Modifier.height(320.dp),
                body = "در فهرست شاعران یا صفحهٔ هر شاعر «علاقه‌مندم» را بزنید.",
                action = {
                    Button(onClick = onOpenPoets) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("دیدن شاعران")
                    }
                },
            )
        }
        return
    }
    item { SectionLabel("شاعران") }
    items(poets, key = { it.key }) { poet ->
        val source = PoetKey.parse(poet.key)?.source
        AppRow(
            title = poet.name,
            meta = when (source) {
                PoetSource.Echolalia -> "اکولالیا"
                PoetSource.Local -> "گنجورک"
                else -> "گنجور"
            },
            leading = { PoetAvatar(poet.name, null, 40.dp) },
            trailing = {
                OutlinedButton(onClick = { onUnfollow(poet.key) }) { Text("حذف") }
            },
            onClick = { onOpenPoet(poet.key) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item { SectionLabel("بیشتر") }
    item {
        AppRow(
            title = "پیدا کردن شاعر تازه",
            meta = "فهرست همهٔ شاعران با جست‌وجو",
            leading = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
            onClick = onOpenPoets,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item {
        AppRow(
            title = "ویرایش گروهی",
            meta = "انتخاب چند شاعر یکجا",
            leading = { Icon(Icons.Filled.Tune, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
            onClick = onEditFollowing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
