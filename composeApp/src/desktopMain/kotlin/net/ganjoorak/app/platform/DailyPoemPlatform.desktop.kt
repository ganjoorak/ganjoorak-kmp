package net.ganjoorak.app.platform

import androidx.compose.runtime.Composable

actual fun refreshDailyPoemWidget() {
    // No home-screen widget on desktop.
}

actual fun rescheduleDailyPoemWork() {
    // No daily worker on desktop.
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = {
    // No runtime notification permission on desktop.
}
