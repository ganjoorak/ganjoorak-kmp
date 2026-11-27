package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.util.toPersianDigits

/** Height of the floating destination pill (Google Photos style). */
val FloatingNavPillHeight = 56.dp

/** Gap between the pill row and the bottom edge, above the system gesture bar. */
val FloatingNavBottomMargin = 12.dp

/** Side margin of the floating pill row. */
val FloatingNavSideMargin = 12.dp

/** Diameter of the separate circular search button next to the pill. */
val FloatingNavSearchSize = 48.dp

/** Gap between the pill and the search circle. */
val FloatingNavSearchGap = 8.dp

/** Inner padding of the pill around its destinations. */
private val PillInnerPadding = 6.dp

/** Height of one destination inside the pill (pill height minus inner padding). */
private val NavItemHeight = FloatingNavPillHeight - PillInnerPadding * 2

/**
 * Space the shell reserves under content for the floating pill nav: pill
 * height + bottom margin + the navigation-bars inset, so no verse or list
 * item is ever hidden behind the pill. Measured from the tokens above, not
 * guessed — change a token and the reserve follows.
 */
@Composable
fun floatingNavBottomReserve(): Dp {
    val navBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return FloatingNavPillHeight + FloatingNavBottomMargin + navBars
}

/**
 * Phone (and the desktop `GANJOORAK_NAV=bar` QA hook): the four primary
 * destinations live in a FLOATING pill above the bottom edge, Google Photos
 * style — a dark [inverseSurface] pill, the selected destination a [primary]
 * pill with icon + label inside it, the rest icon-only, plus a separate
 * circular search button on the END side (left in RTL Persian).
 * Everything else (favourites detail, profile, offline downloads, settings)
 * stays in the menu sheet, opened from the Feed top row.
 * Selected state follows the current route; detail routes (poem, poet,
 * category, search…) leave every item unselected.
 * The mini-player sits above the pill, in flow.
 */
@Composable
fun PhoneAppChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { navContent() }
        miniPlayer()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = FloatingNavSideMargin,
                    end = FloatingNavSideMargin,
                    bottom = FloatingNavBottomMargin + navBarsBottom,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(FloatingNavPillHeight),
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(PillInnerPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavDestination(
                        selected = current == MenuTarget.Poets,
                        label = "شاعران",
                        selectedIcon = Icons.Filled.People,
                        unselectedIcon = Icons.Outlined.People,
                        onClick = { onSelect(MenuTarget.Poets) },
                    )
                    NavDestination(
                        selected = current == MenuTarget.Home,
                        label = "فید",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        onClick = { onSelect(MenuTarget.Home) },
                    )
                    NavDestination(
                        selected = current == MenuTarget.Explore,
                        label = "کاوش",
                        selectedIcon = Icons.Filled.Explore,
                        unselectedIcon = Icons.Outlined.Explore,
                        onClick = { onSelect(MenuTarget.Explore) },
                    )
                    NavDestination(
                        selected = current == MenuTarget.Favorites,
                        label = "ذخیره‌شده‌ها",
                        selectedIcon = Icons.Filled.Favorite,
                        unselectedIcon = Icons.Filled.FavoriteBorder,
                        badgeCount = favoritesCount,
                        onClick = { onSelect(MenuTarget.Favorites) },
                    )
                }
            }
            Spacer(Modifier.width(FloatingNavSearchGap))
            Surface(
                onClick = onOpenSearch,
                modifier = Modifier.size(FloatingNavSearchSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp,
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "جست‌وجو")
                }
            }
        }
    }
}

/**
 * One destination inside the pill. Selected: a [primary] pill with icon +
 * label (the label shows only for the selected item). Unselected: icon only.
 * The followed-count badge shows on the Saved icon while it is unselected;
 * on the selected pill it would crowd the label, and the user is already
 * looking at the list.
 */
@Composable
private fun NavDestination(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    if (selected) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(
                Modifier
                    .height(NavItemHeight)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(selectedIcon, contentDescription = null)
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(NavItemHeight),
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge { Text(badgeCount.toPersianDigits()) }
                    }
                },
            ) {
                Icon(unselectedIcon, contentDescription = label)
            }
        }
    }
}
