package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Space the shell reserves under content for the persistent bottom chrome.
 * The floating destination pill was deleted (Hadi 2026-11-27: "delete the
 * bottom menu completely"), so only the system navigation-bars inset
 * remains. The mini-player sits in flow below the content and pads itself,
 * so lists and poems need nothing more. Measured from the insets, not
 * guessed — change the chrome and the reserve follows.
 */
@Composable
fun persistentBottomReserve(): Dp {
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * Phone shell: the back-stack content and, below it in flow, the
 * mini-player. There is NO bottom navigation bar any more — navigation
 * lives in the tiny [TinyNav] mini-pill in the poem controls row and in
 * the top bars of the Poets / Explore / Saved screens. Desktop keeps its
 * side rail (see AppLayoutChrome.desktop).
 */
@Composable
fun PhoneAppChrome(
    navContent: @Composable () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { navContent() }
        miniPlayer()
    }
}
