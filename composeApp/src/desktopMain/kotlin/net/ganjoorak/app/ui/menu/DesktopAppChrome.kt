package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.ui.player.DesktopPlayerBar

/**
 * Desktop window chrome: full-width RTL content, with a wide navigation rail
 * flush to the physical right (RTL leading) edge, expanding over the content.
 * Content (and the wide player bar) reserves the collapsed rail width on the
 * physical right so lists are not clipped under the rail. Poem actions live in
 * a small floating group at the bottom of the rail column (see
 * [AppFloatingSideNav]). The phone MiniPlayer pill is replaced here by the
 * wide [DesktopPlayerBar]; the shared `miniPlayer` slot is only used by the
 * phone-chrome fallback (see [AppLayoutChrome]).
 */
@Composable
fun DesktopAppChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    navContent: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().absolutePadding(right = DesktopRailColumnWidth)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.weight(1f)) { navContent() }
                    DesktopPlayerBar(player = AppGraph.instance.player)
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
