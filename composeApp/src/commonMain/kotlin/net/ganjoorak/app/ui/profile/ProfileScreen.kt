package net.ganjoorak.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.stories.StoriesRow
import net.ganjoorak.app.util.toPersianDigits

private const val FAVORITES_SHOWN = 5

/**
 * The site's «حساب من»: a hero card, the last poem read and the liked poems.
 * Sign-in uses the same accounts API as the website; unread data still lives on the device.
 */
@Composable
fun ProfileScreen(
    onBack: (() -> Unit)? = null,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogin: (signup: Boolean) -> Unit,
) {
    val graph = AppGraph.instance
    val saved by graph.library.savedPoems.collectAsState()
    val history by graph.library.history.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val account by graph.accounts.session.collectAsState()
    val storyState by graph.stories.state.collectAsState()
    val scope = rememberCoroutineScope()
    val user = account.user

    AppPage(
        title = "حساب من",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "تنظیمات") }
        },
        headerExtra = {
            BorderedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                user?.displayName?.ifBlank { user.username } ?: "خوانندهٔ گنجورک",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (user != null) {
                                    "@${user.username} · ${saved.size.toPersianDigits()} پسندیده · ${settings.followedPoets.size.toPersianDigits()} شاعر مورد علاقه"
                                } else {
                                    "${saved.size.toPersianDigits()} پسندیده · ${settings.followedPoets.size.toPersianDigits()} شاعر مورد علاقه"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (user == null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenLogin(true) }, modifier = Modifier.weight(1f)) { Text("ساخت حساب") }
                            OutlinedButton(onClick = { onOpenLogin(false) }, modifier = Modifier.weight(1f)) { Text("ورود") }
                        }
                    } else {
                        TextButton(onClick = { scope.launch { graph.accounts.logOut() } }) { Text("خروج از حساب") }
                    }
                }
            }
        },
    ) {
        section("آخرین شعر")
        val last = history.firstOrNull()
        if (last == null) {
            empty("هنوز شعری نخوانده یا نشنیده‌اید.")
        } else {
            item(key = "last") { PoemRow(last) { onOpenPoem(last.ref) } }
        }

        section(
            if (saved.isEmpty()) "پسندیده‌ها" else "پسندیده‌ها (${saved.size.toPersianDigits()})",
            action = if (saved.size > FAVORITES_SHOWN) ("همه" to onOpenSaved) else null,
        )
        if (saved.isEmpty()) {
            empty("هنوز شعری نپسندیده‌اید. روی آیکن قلب در صفحهٔ شعر بزنید.")
        } else {
            items(saved.take(FAVORITES_SHOWN), key = { "fav-${it.ref.key}" }) { poem -> PoemRow(poem) { onOpenPoem(poem.ref) } }
        }

        if (storyState.people.isNotEmpty()) {
            item(key = "stories") {
                val openStories = LocalAppActions.current.openStories
                StoriesRow(
                    people = storyState.people,
                    isUnseen = { storyState.isUnseen(it) },
                    onOpen = { index -> openStories(storyState.people, index) },
                )
            }
        }

        if (user != null) {
            section("تکه‌ها")
            ownerQuotesSection(user.username, onOpenPoem)
        }

        section("شاعران")
        item(key = "following") {
            AppRow(
                title = "شاعران مورد علاقه",
                meta = settings.followedPoets.takeIf { it.isNotEmpty() }?.joinToString("، ") { it.name } ?: "هنوز شاعری به علاقه‌مندی‌ها اضافه نکرده‌اید",
                leading = { Icon(Icons.Filled.HowToReg, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onOpenFollowing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.section(title: String, action: Pair<String, () -> Unit>? = null) {
    item(key = "section-$title") {
        Row(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) {
                TextButton(onClick = action.second) {
                    Text(action.first)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun LazyListScope.empty(message: String) {
    item(key = "empty-$message") {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PoemRow(poem: SavedPoem, onClick: () -> Unit) {
    AppRow(title = poem.title, meta = poem.poetName, onClick = onClick, modifier = Modifier.fillMaxWidth())
}
