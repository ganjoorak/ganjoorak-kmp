package net.ganjoorak.app.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.util.toPersianDigits

/** Gap between the floating nav card and the window edge. */
val FloatingNavMargin = 12.dp

/** Width of the desktop vertical bar, not counting [FloatingNavMargin] on either side. */
val FloatingNavWidth = 64.dp

/** Height of the phone bar, not counting [FloatingNavMargin] on either side. */
val FloatingNavHeight = 72.dp

val FloatingNavOccupiedEnd: Dp = FloatingNavWidth + FloatingNavMargin * 2
val FloatingNavOccupiedBottom: Dp = FloatingNavHeight + FloatingNavMargin * 2

private val FloatingNavShape = RoundedCornerShape(32.dp)
private val FloatingNavElevation = 6.dp

/**
 * Extra padding the shell reserves so content is not hidden under the floating
 * destination bar. The phone sets a bottom inset; on desktop the rail overlays
 * the content and no end inset is reserved, so poems can use the full width.
 */
val LocalPersistentNavPadding = compositionLocalOf { PaddingValues(0.dp) }

data class NavDestination(
    val target: MenuTarget,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val label: String,
)

/** The five primary destinations shared by the desktop side bar and the phone bottom bar. */
val PrimaryDestinations = listOf(
    NavDestination(MenuTarget.Home, Icons.Filled.Home, label = "صفحهٔ اصلی"),
    NavDestination(MenuTarget.Explore, Icons.Filled.Explore, label = "کاوش"),
    NavDestination(MenuTarget.Poets, Icons.Filled.People, label = "شاعران"),
    NavDestination(
        MenuTarget.Favorites,
        Icons.Filled.FavoriteBorder,
        Icons.Filled.Favorite,
        "علاقه‌مندی‌ها",
    ),
    NavDestination(MenuTarget.Profile, Icons.Filled.AccountCircle, label = "حساب"),
)

/**
 * Desktop: a narrow icon-only vertical pill, wrapped to its content and
 * centered on the physical right edge — not stretched to the window height.
 * The column scrolls if the window is too short. Poem play/like/share live
 * in a separate floating pill on the poem itself, not in this rail.
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
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompactRailItem(
                    selected = current == MenuTarget.Search,
                    onClick = onOpenSearch,
                    description = "جست‌وجو",
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "جست‌وجو", modifier = Modifier.size(20.dp))
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
                            badge = destinationBadge(item.target, favoritesCount, hasUnseenStories),
                        )
                    }
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
            .padding(vertical = 1.dp),
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


/**
 * Phone: a horizontal rounded card of the same five destinations, floating
 * above the system navigation bar with the same margins and elevation.
 */
@Composable
fun AppFloatingBottomNav(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val itemColors = NavigationBarItemDefaults.colors(
        indicatorColor = colors.secondaryContainer,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FloatingNavShape,
        color = colors.surfaceContainerHigh,
        shadowElevation = FloatingNavElevation,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().height(FloatingNavHeight).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in PrimaryDestinations) {
                NavigationBarItem(
                    selected = current == item.target,
                    onClick = { onSelect(item.target) },
                    colors = itemColors,
                    modifier = Modifier.weight(1f),
                    icon = {
                        DestinationIcon(
                            item = item,
                            selected = current == item.target,
                            badge = destinationBadge(item.target, favoritesCount, hasUnseenStories),
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = true,
                )
            }
        }
    }
}

private sealed interface DestBadge {
    data object Dot : DestBadge
    data class Count(val text: String) : DestBadge
}

@Composable
private fun DestinationIcon(item: NavDestination, selected: Boolean, badge: DestBadge?) {
    val icon = @Composable { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp)) }
    if (badge != null) {
        BadgedBox(
            badge = {
                when (badge) {
                    DestBadge.Dot -> Badge(Modifier.clip(CircleShape).size(8.dp))
                    is DestBadge.Count -> Badge { Text(badge.text) }
                }
            },
        ) { icon() }
    } else {
        icon()
    }
}

private fun destinationBadge(target: MenuTarget, favoritesCount: Int, hasUnseenStories: Boolean): DestBadge? = when (target) {
    MenuTarget.Favorites -> favoritesCount.takeIf { it > 0 }?.toPersianDigits()?.let { DestBadge.Count(it) }
    MenuTarget.Profile -> if (hasUnseenStories) DestBadge.Dot else null
    else -> null
}
