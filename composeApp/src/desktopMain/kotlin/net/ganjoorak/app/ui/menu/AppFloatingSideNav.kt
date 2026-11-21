package net.ganjoorak.app.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.util.toPersianDigits

/** Gap between the floating rail and the window edge. Desktop only. */
val FloatingNavMargin = 12.dp

/** Width of the desktop vertical bar, not counting [FloatingNavMargin] on either side. */
val FloatingNavWidth = 64.dp

/** Space between the poem-action group and the destination rail. */
val FloatingChromeGap = 10.dp

private val FloatingNavShape = RoundedCornerShape(32.dp)
private val FloatingNavElevation = 6.dp

private data class NavDestination(
    val target: MenuTarget,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val label: String,
)

/** The essential desktop destinations. Explore, poets, settings and stories live in the overflow. */
private val PrimaryDestinations = listOf(
    NavDestination(MenuTarget.Home, Icons.Filled.Home, label = "صفحهٔ اصلی"),
    NavDestination(
        MenuTarget.Favorites,
        Icons.Filled.FavoriteBorder,
        Icons.Filled.Favorite,
        "علاقه‌مندی‌ها",
    ),
    NavDestination(MenuTarget.Profile, Icons.Filled.AccountCircle, label = "حساب"),
)

private val OverflowRouteItems = listOf(
    NavDestination(MenuTarget.Explore, Icons.Filled.Explore, label = "کاوش"),
    NavDestination(MenuTarget.Poets, Icons.Filled.People, label = "شاعران"),
    NavDestination(MenuTarget.Settings, Icons.Filled.Settings, label = "تنظیمات"),
)

private val OverflowTargets = OverflowRouteItems.map { it.target }.toSet()

/**
 * Desktop: a narrow icon-only vertical pill of the essential destinations,
 * wrapped to its content and centered on the physical right edge.
 * Search, home, favorites and account stay on the rail; rarer destinations
 * and stories open from the overflow at the bottom.
 */
@Composable
fun AppFloatingSideNav(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val overflowSelected = current in OverflowTargets
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(FloatingNavWidth).heightIn(max = maxHeight),
            shape = FloatingNavShape,
            color = colors.surfaceContainerHigh,
            shadowElevation = FloatingNavElevation,
            tonalElevation = 2.dp,
        ) {
            Column(
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompactRailItem(
                    selected = current == MenuTarget.Search,
                    onClick = onOpenSearch,
                    description = "جست‌وجو",
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "جست‌وجو", modifier = Modifier.size(22.dp))
                }
                for (item in PrimaryDestinations) {
                    CompactRailItem(
                        selected = current == item.target,
                        onClick = { onSelect(item.target) },
                        description = item.label,
                    ) {
                        DestinationIcon(
                            item = item,
                            selected = current == item.target,
                            badge = if (item.target == MenuTarget.Favorites) {
                                favoritesCount.takeIf { it > 0 }?.toPersianDigits()
                            } else {
                                null
                            },
                        )
                    }
                }
                OverflowRailItem(
                    selected = overflowSelected,
                    hasUnseenStories = hasUnseenStories,
                    current = current,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun OverflowRailItem(
    selected: Boolean,
    hasUnseenStories: Boolean,
    current: MenuTarget?,
    onSelect: (MenuTarget) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val graph = AppGraph.instance
    val storyState by graph.stories.state.collectAsState()
    val openStories = LocalAppActions.current.openStories
    // RTL so the overflow menu opens inward, away from the window edge.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Box {
        CompactRailItem(
            selected = selected || open,
            onClick = { open = true },
            description = "بیشتر",
        ) {
            val icon = @Composable {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "بیشتر", modifier = Modifier.size(22.dp))
            }
            if (hasUnseenStories) {
                BadgedBox(badge = { Badge(Modifier.clip(CircleShape).size(8.dp)) }) { icon() }
            } else {
                icon()
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset((-8).dp, 0.dp),
        ) {
            for (item in OverflowRouteItems) {
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = {
                        Icon(
                            if (current == item.target) item.selectedIcon else item.icon,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(item.target)
                    },
                )
            }
            if (storyState.people.isNotEmpty()) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("تکه‌ها") },
                    leadingIcon = {
                        if (hasUnseenStories) {
                            BadgedBox(badge = { Badge(Modifier.clip(CircleShape).size(8.dp)) }) {
                                Icon(Icons.Filled.AutoStories, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Filled.AutoStories, contentDescription = null)
                        }
                    },
                    onClick = {
                        open = false
                        val start = storyState.people.indexOfFirst { storyState.isUnseen(it) }.let { if (it < 0) 0 else it }
                        openStories(storyState.people, start)
                    },
                )
            }
        }
    }
    }
}

@Composable
private fun CompactRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    icon: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val indicator by animateColorAsState(if (selected) colors.secondaryContainer else Color.Transparent)
    Box(
        Modifier
            .width(FloatingNavWidth)
            .semantics { contentDescription = description }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
}

@Composable
private fun DestinationIcon(item: NavDestination, selected: Boolean, badge: String?) {
    val icon = @Composable { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) }
    if (badge != null) {
        BadgedBox(
            badge = { Badge { Text(badge) } },
        ) { icon() }
    } else {
        icon()
    }
}
