package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Extra padding the shell reserves under content for the persistent
 * bottom chrome: only the navigation-bars inset on phone (the floating
 * destination pill was deleted 2026-11-27; see persistentBottomReserve),
 * zero where the side rail is used instead. The value is provided by the
 * shell in App.kt; list screens and the poem chrome read it from here so
 * no screen has to branch on the platform.
 */
val LocalPersistentNavPadding = compositionLocalOf { PaddingValues(0.dp) }
