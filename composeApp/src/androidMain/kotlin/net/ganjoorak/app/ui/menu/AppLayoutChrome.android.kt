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
    // The nav params are consumed through LocalTinyNav (provided by the
    // shell in App.kt), not by the phone chrome: there is no bottom bar any
    // more — the tiny nav lives in the poem controls row and the list bars.
    PhoneAppChrome(
        navContent = navContent,
        miniPlayer = miniPlayer,
    )
}
