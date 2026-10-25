package net.ganjoorak.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.settings.SettingsContent
import net.ganjoorak.app.util.toPersianDigits

/** The profile tab: the reader's library (saved poems, home-feed poets) followed by settings. */
@Composable
fun ProfileScreen(onOpenSaved: () -> Unit) {
    val graph = AppGraph.instance
    val saved by graph.library.savedPoems.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    var pickPoets by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("پروفایل") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                SectionTitle("کتابخانه")
                LibraryRow(
                    icon = Icons.Outlined.Bookmarks,
                    title = "نشان‌شده‌ها",
                    subtitle = if (saved.isEmpty()) "هنوز شعری نشان نکرده‌اید" else "${saved.size.toPersianDigits()} شعر",
                    onClick = onOpenSaved,
                )
                LibraryRow(
                    icon = Icons.Outlined.People,
                    title = "شاعران صفحهٔ اصلی",
                    subtitle = settings.followedPoets.takeIf { it.isNotEmpty() }?.joinToString("، ") { it.name }
                        ?: "همهٔ شاعران",
                    onClick = { pickPoets = true },
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle("تنظیمات")
                SettingsContent()
            }
        }
    }

    if (pickPoets) FeedPoetsSheet(onDismiss = { pickPoets = false })
}

@Composable
private fun LibraryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
