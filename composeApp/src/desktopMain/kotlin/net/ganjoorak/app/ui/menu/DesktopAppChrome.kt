package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Desktop window chrome: full-width RTL content, with a wide navigation rail
 * flush to the physical right (RTL leading) edge, expanding over the content.
 * There is no reserved gutter and no bottom poem pill; poem actions live in a
 * small floating group at the bottom of the rail column (see
 * [AppFloatingSideNav]).
 */
@Composable
fun DesktopAppChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.weight(1f)) { navContent() }
                    miniPlayer()
                }
            }
            // RTL so the rail's leading edge and the expanded modal sit on the right.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppFloatingSideNav(
                    current = current,
                    favoritesCount = favoritesCount,
                    hasUnseenStories = hasUnseenStories,
                    onSelect = onSelect,
                    onOpenSearch = onOpenSearch,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
