package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Extra padding the shell used to reserve under content for a persistent
 * destination bar. Phone navigation is the v0.1.2 chip again, and the desktop
 * rail width is reserved by the chrome itself, so this stays zero. Kept so list
 * screens can still read a single inset without branching on the platform.
 */
val LocalPersistentNavPadding = compositionLocalOf { PaddingValues(0.dp) }
