package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Phone (and the desktop `GANJOORAK_NAV=bar` QA hook): no persistent destination bar.
 * Destinations open from the v0.1.2 menu chip on the feed/poem, via [AppMenu].
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
