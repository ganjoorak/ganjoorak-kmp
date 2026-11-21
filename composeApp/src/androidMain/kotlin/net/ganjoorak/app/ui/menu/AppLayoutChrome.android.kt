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
    PhoneAppChrome(navContent = navContent, miniPlayer = miniPlayer)
}
