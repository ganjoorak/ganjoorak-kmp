package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.util.toPersianDigits

/** Container height of the phone [ShortNavigationBar] (M3 NavigationBar token). */
val ShortNavBarHeight = 64.dp

/**
 * Phone (and the desktop `GANJOORAK_NAV=bar` QA hook): the four primary
 * destinations live in a [ShortNavigationBar] under the mini-player.
 * Everything else (search, favourites detail, profile, offline downloads,
 * settings) stays in the menu sheet, opened from the Feed top row.
 * Selected state follows the current route; detail routes (poem, poet,
 * category, search…) leave every item unselected.
 */
@Composable
fun PhoneAppChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    onSelect: (MenuTarget) -> Unit,
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { navContent() }
        miniPlayer()
        ShortNavigationBar {
            ShortNavigationBarItem(
                selected = current == MenuTarget.Poets,
                onClick = { onSelect(MenuTarget.Poets) },
                icon = {
                    Icon(
                        if (current == MenuTarget.Poets) Icons.Filled.People else Icons.Outlined.People,
                        contentDescription = null,
                    )
                },
                label = { Text("شاعران") },
            )
            ShortNavigationBarItem(
                selected = current == MenuTarget.Home,
                onClick = { onSelect(MenuTarget.Home) },
                icon = {
                    Icon(
                        if (current == MenuTarget.Home) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = null,
                    )
                },
                label = { Text("فید") },
            )
            ShortNavigationBarItem(
                selected = current == MenuTarget.Explore,
                onClick = { onSelect(MenuTarget.Explore) },
                icon = {
                    Icon(
                        if (current == MenuTarget.Explore) Icons.Filled.Explore else Icons.Outlined.Explore,
                        contentDescription = null,
                    )
                },
                label = { Text("کاوش") },
            )
            ShortNavigationBarItem(
                selected = current == MenuTarget.Favorites,
                onClick = { onSelect(MenuTarget.Favorites) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (favoritesCount > 0) {
                                Badge { Text(favoritesCount.toPersianDigits()) }
                            }
                        },
                    ) {
                        Icon(
                            if (current == MenuTarget.Favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text("ذخیره‌شده‌ها") },
            )
        }
    }
}
