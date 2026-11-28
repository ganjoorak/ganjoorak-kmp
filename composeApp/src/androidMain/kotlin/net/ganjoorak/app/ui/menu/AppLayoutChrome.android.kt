package net.ganjoorak.app.ui.menu

import androidx.compose.runtime.Composable

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
    // The nav params drive the floating destination pill (restored
    // 2026-11-28 after the TinyNav experiment was reverted).
    PhoneAppChrome(
        current = current,
        favoritesCount = favoritesCount,
        onSelect = onSelect,
        onOpenSearch = onOpenSearch,
        navContent = navContent,
        miniPlayer = miniPlayer,
    )
}
