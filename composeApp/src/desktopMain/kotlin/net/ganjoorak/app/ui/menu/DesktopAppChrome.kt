package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Desktop window chrome: full-width RTL content, with the destination rail
 * on the physical right edge and the poem-action group just to its left.
 * Both are vertically centered and visually separate. There is no bottom
 * poem pill and no poem chrome in the header.
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
            // The row stays LTR so the rail sits on the physical right edge
            // and the poem group is just to its left, both vertically centered.
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(FloatingNavMargin),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FloatingChromeGap),
            ) {
                DesktopPoemActionGroup()
                AppFloatingSideNav(
                    current = current,
                    favoritesCount = favoritesCount,
                    hasUnseenStories = hasUnseenStories,
                    onSelect = onSelect,
                    onOpenSearch = onOpenSearch,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}
