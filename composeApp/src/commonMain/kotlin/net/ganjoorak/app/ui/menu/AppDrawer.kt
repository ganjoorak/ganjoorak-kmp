package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.app_icon
import net.ganjoorak.app.util.toPersianDigits
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

enum class MenuDestination(val label: String, val icon: ImageVector) {
    Home("صفحه اصلی", Icons.Filled.Home),
    Search("جستجو", Icons.Filled.Search),
    Poets("شاعران", Icons.Filled.People),
    Saved("نشان‌شده‌ها", Icons.Filled.Favorite),
}

private val THEME_OPTIONS = listOf(
    Triple(ThemeMode.Dark, "تاریک", Icons.Filled.DarkMode),
    Triple(ThemeMode.Light, "روشن", Icons.Filled.LightMode),
    Triple(ThemeMode.Paper, "کاغذی", Icons.Filled.Description),
)

@Composable
fun AppDrawer(
    current: MenuDestination?,
    onNavigate: (MenuDestination) -> Unit,
    onOpenFeedPoets: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp),
    ) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(Res.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("گنجورک", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "راحت‌تر شعر بخوانیم و شعر گوش دهیم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "بستن منو") }
            }

            MenuDestination.entries.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(destination.label) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    selected = destination == current,
                    onClick = { onNavigate(destination) },
                    colors = itemColors,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }

            Text(
                "بیشتر",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
            )
            NavigationDrawerItem(
                label = { Text("شاعران صفحه اصلی") },
                badge = {
                    if (settings.followedPoets.isNotEmpty()) Text(settings.followedPoets.size.toPersianDigits())
                },
                icon = { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null) },
                selected = false,
                onClick = onOpenFeedPoets,
                colors = itemColors,
            )
            NavigationDrawerItem(
                label = { Text("تنظیمات") },
                icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                selected = false,
                onClick = onOpenSettings,
                colors = itemColors,
            )

            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                THEME_OPTIONS.forEachIndexed { index, (mode, label, icon) ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { store.update { it.copy(theme = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(index, THEME_OPTIONS.size),
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    ) { Text(label) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("اندازهٔ متن شعر", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { store.update { it.copy(fontScale = stepScale(it.fontScale, -1)) } },
                        enabled = settings.fontScale > MIN_SCALE,
                    ) { Icon(Icons.Filled.Remove, contentDescription = "کوچک‌تر کردن متن") }
                    Text("${(settings.fontScale * 100).roundToInt().toPersianDigits()}٪", style = MaterialTheme.typography.labelLarge)
                    IconButton(
                        onClick = { store.update { it.copy(fontScale = stepScale(it.fontScale, 1)) } },
                        enabled = settings.fontScale < MAX_SCALE,
                    ) { Icon(Icons.Filled.Add, contentDescription = "بزرگ‌تر کردن متن") }
                }
            }
        }
    }
}

const val MIN_SCALE = 0.8f
const val MAX_SCALE = 1.6f

private fun stepScale(scale: Float, direction: Int): Float =
    ((scale * 20).roundToInt() + direction * 2).div(20f).coerceIn(MIN_SCALE, MAX_SCALE)
