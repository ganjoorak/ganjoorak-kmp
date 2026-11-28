package net.ganjoorak.app.ui.menu

import androidx.compose.runtime.Composable

/**
 * Platform shell around the back stack and mini-player.
 *
 * Android always uses [PhoneAppChrome]: the floating destination pill at
 * the bottom, the mini-player above it (in flow). Desktop uses the floating
 * side rail, unless `GANJOORAK_NAV=bar` forces the phone chrome.
 * The rail lives only in desktopMain so desktop changes cannot leak into the Android APK.
 */
@Composable
expect fun AppLayoutChrome(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
)
