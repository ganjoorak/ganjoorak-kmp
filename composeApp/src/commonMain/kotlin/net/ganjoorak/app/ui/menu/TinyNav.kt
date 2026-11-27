package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.platform.usesSideNavigation

/**
 * The tiny destination nav (Hadi 2026-11-27): the bottom floating pill is
 * deleted, and navigation lives in a very small icon-only mini-pill — the
 * four destinations plus search — in the poem controls row (at the
 * hamburger's old spot, next to the poet pill) and in the top bars of the
 * Poets / Explore / Saved screens.
 *
 * Provided by the shell in App.kt; null wherever the shell does not provide
 * it, and hidden on desktop, where the side rail owns navigation.
 */
data class TinyNavState(
    val current: MenuTarget?,
    val favoritesCount: Int,
    val onSelect: (MenuTarget) -> Unit,
    val onOpenSearch: () -> Unit,
)

val LocalTinyNav = compositionLocalOf<TinyNavState?> { null }

/**
 * One tiny nav item: a 28dp box with an 18dp icon. The ask was ~32dp; 28dp
 * is what lets the poem controls row (mini-pill + hamburger + play) clear
 * the vertical side column on a 360dp screen. Plain Box + clickable on
 * purpose: M3 IconButton and clickable Surface enforce a 48dp minimum
 * touch target, which would blow the mini-pill back up to full size.
 */
private val TinyNavItemSize = 28.dp
private val TinyNavIconSize = 18.dp

@Composable
fun TinyNav() {
    val state = LocalTinyNav.current ?: return
    if (usesSideNavigation) return
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurfaceVariant,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .height(TinyNavItemSize)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TinyNavItem(
                selected = state.current == MenuTarget.Poets,
                selectedIcon = Icons.Filled.People,
                unselectedIcon = Icons.Outlined.People,
                label = "شاعران",
                onClick = { state.onSelect(MenuTarget.Poets) },
            )
            TinyNavItem(
                selected = state.current == MenuTarget.Home,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "فید",
                onClick = { state.onSelect(MenuTarget.Home) },
            )
            TinyNavItem(
                selected = state.current == MenuTarget.Explore,
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                label = "کاوش",
                onClick = { state.onSelect(MenuTarget.Explore) },
            )
            TinyNavItem(
                selected = state.current == MenuTarget.Favorites,
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Filled.FavoriteBorder,
                label = "ذخیره‌شده‌ها",
                badge = state.favoritesCount > 0 && state.current != MenuTarget.Favorites,
                onClick = { state.onSelect(MenuTarget.Favorites) },
            )
            Box(
                Modifier
                    .size(TinyNavItemSize)
                    .clip(CircleShape)
                    .clickable(onClick = state.onOpenSearch, role = Role.Button),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "جست‌وجو",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(TinyNavIconSize),
                )
            }
        }
    }
}

@Composable
private fun TinyNavItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(TinyNavItemSize)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(colors.primary) else Modifier)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.size(TinyNavIconSize),
        )
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(6.dp)
                    .background(colors.error, CircleShape),
            )
        }
    }
}
