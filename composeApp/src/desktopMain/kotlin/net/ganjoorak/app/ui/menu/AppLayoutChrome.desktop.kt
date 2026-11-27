package net.ganjoorak.app.ui.menu

import androidx.compose.runtime.Composable
import net.ganjoorak.app.platform.usesSideNavigation

@Composable
actual fun AppLayoutChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    if (usesSideNavigation) {
        DesktopAppChrome(
            current = current,
            favoritesCount = favoritesCount,
            hasUnseenStories = hasUnseenStories,
            onSelect = onSelect,
            onOpenSearch = onOpenSearch,
            navContent = navContent,
        )
    } else {
        PhoneAppChrome(
            navContent = navContent,
            miniPlayer = miniPlayer,
        )
    }
}
