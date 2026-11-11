package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.util.toPersianDigits

private data class RailDestination(
    val target: MenuTarget,
    val icon: ImageVector,
    val label: String,
)

private val RailItems = listOf(
    RailDestination(MenuTarget.Home, Icons.Filled.Home, "صفحهٔ اصلی"),
    RailDestination(MenuTarget.Explore, Icons.Filled.Explore, "کاوش"),
    RailDestination(MenuTarget.Poets, Icons.Filled.People, "شاعران"),
    RailDestination(MenuTarget.Saved, Icons.Filled.FavoriteBorder, "نشان‌شده‌ها"),
    RailDestination(MenuTarget.Following, Icons.Filled.HowToReg, "دنبال‌شده‌ها"),
    RailDestination(MenuTarget.Profile, Icons.Filled.AccountCircle, "حساب"),
    RailDestination(MenuTarget.Settings, Icons.Filled.Settings, "تنظیمات"),
)

/**
 * Persistent right-edge destinations for wide (desktop) windows, replacing the
 * floating menu button and the bottom sheet.
 */
@Composable
fun AppNavigationRail(
    current: MenuTarget?,
    hasStories: Boolean,
    hasUnseenStories: Boolean,
    followedCount: Int,
    onSelect: (MenuTarget) -> Unit,
    onOpenStories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = {
            Column(
                Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavigationRailItem(
                    selected = current == MenuTarget.Search,
                    onClick = { onSelect(MenuTarget.Search) },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "جست‌وجو") },
                    label = { Text("جست‌وجو") },
                )
                if (hasStories) {
                    NavigationRailItem(
                        selected = false,
                        onClick = onOpenStories,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (hasUnseenStories) {
                                        Badge(Modifier.clip(CircleShape).size(8.dp))
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.AutoStories, contentDescription = "تکه‌ها")
                            }
                        },
                        label = { Text("تکه‌ها") },
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        },
    ) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
            for (item in RailItems) {
                val count = followedCount.takeIf { item.target == MenuTarget.Following && it > 0 }
                NavigationRailItem(
                    selected = current == item.target,
                    onClick = { onSelect(item.target) },
                    icon = {
                        if (count != null) {
                            BadgedBox(badge = { Badge { Text(count.toPersianDigits()) } }) {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = { Text(item.label) },
                )
            }
        }
    }
}
