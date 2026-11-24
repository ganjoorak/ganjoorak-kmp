package net.ganjoorak.app.platform

import androidx.compose.runtime.Composable

/**
 * Android-only daily-poem hooks. The widget, the daily worker and the
 * notification permission live in androidMain; desktop gets no-ops.
 */

/** Rebuilds every daily-poem widget from the stored poem (Android only). */
expect fun refreshDailyPoemWidget()

/** Re-enqueues the daily worker for the next configured notification time (Android only). */
expect fun rescheduleDailyPoemWork()

/**
 * Returns a callback that asks for the POST_NOTIFICATIONS runtime permission
 * (Android 13+). On older Android — and on desktop — it does nothing.
 */
@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit
