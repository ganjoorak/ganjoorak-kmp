package net.ganjoorak.app.ui.poem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.menu.LocalPersistentNavPadding

/** Height the floating bar takes at the bottom; poem lists pad their end by this much. */
val PoemControlsHeight = 80.dp

private val BarHeight = 48.dp

/** Actions for the poem currently on screen, published so the desktop side nav can host them. */
data class PoemChrome(
    val poem: Poem,
    val isSaved: Boolean,
    val isPlaying: Boolean,
    val onToggleSave: () -> Unit,
    val onShare: () -> Unit,
    val onPlay: () -> Unit,
    val onOpenPoet: ((String) -> Unit)?,
)

class PoemChromeHost {
    var current by mutableStateOf<PoemChrome?>(null)
}

val LocalPoemChromeHost = staticCompositionLocalOf { PoemChromeHost() }

/**
 * Space reserved under a poem so verses are not hidden by the floating chip.
 * Desktop has no bottom chrome (actions sit in the rail column), so it only needs
 * the persistent nav padding.
 */
@Composable
fun poemChromeBottomSpace(): Dp {
    val persistent = LocalPersistentNavPadding.current.calculateBottomPadding()
    return if (usesSideNavigation) persistent else PoemControlsHeight + persistent
}

/**
 * Dispatches to the phone chip bar (v0.1.2: menu / play / share-like-poet) or
 * publishes the desktop poem actions for the side rail column (play /
 * share-like-poet; destinations stay in the rail).
 */
@Composable
fun BoxScope.PoemFloatingControls(
    poem: Poem?,
    isSaved: Boolean,
    isPlaying: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpenPoet: ((String) -> Unit)?,
    onPlayLongPress: () -> Unit = {},
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    if (usesSideNavigation) {
        DesktopPoemFloatingControls(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet, onPlayLongPress, extra)
    } else {
        PhonePoemFloatingControls(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet, onPlayLongPress, extra)
    }
}

/**
 * Phone controls, matching tag v0.1.2: a compact bar of groups — «play» (a
 * plain play/pause button; long-press opens the recitation list in the
 * bottom player) and «share, like, poet», right to left. Destinations live
 * in the floating pill nav now, so the menu chip is gone from this bar;
 * the overflow sheet opens from the Feed top row instead.
 */
@Composable
private fun BoxScope.PhonePoemFloatingControls(
    poem: Poem?,
    isSaved: Boolean,
    isPlaying: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpenPoet: ((String) -> Unit)?,
    onPlayLongPress: () -> Unit = {},
    extra: (@Composable RowScope.() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (poem != null) {
            if (poem.recitations.isNotEmpty()) {
                Pill { PlayCapsule(isPlaying, onPlay, onPlayLongPress) }
            }
            Pill {
                extra?.invoke(this)
                BarIcon(Icons.Filled.Share, "هم‌رسانی", onShare)
                BarIcon(
                    if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
                    onToggleSave,
                    tint = if (isSaved) colors.error else null,
                )
                val key = poem.poetKey
                if (key != null && onOpenPoet != null) {
                    BarItem(onClick = { onOpenPoet(key) }, description = poem.poetName) {
                        PoetAvatar(poem.poetName, poem.poetImageUrl, 28.dp)
                    }
                }
            }
        }
    }
}

/**
 * Desktop: publish the current poem's actions so the floating side nav can draw
 * them as a compact vertical group in the rail column. No bottom pill here; that
 * belongs only to the phone chip bar.
 */
@Composable
private fun BoxScope.DesktopPoemFloatingControls(
    poem: Poem?,
    isSaved: Boolean,
    isPlaying: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpenPoet: ((String) -> Unit)?,
    @Suppress("UNUSED_PARAMETER") onPlayLongPress: () -> Unit = {},
    extra: (@Composable RowScope.() -> Unit)?,
) {
    // [extra] is phone-only (the v0.1.2 chip); desktop does not render it here.
    // Long-press on play is phone-only too: the desktop rail has no recitation list.
    PublishPoemChrome(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet)
}

/** One floating group of the bar. */
@Composable
private fun Pill(content: @Composable RowScope.() -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
        Row(
            Modifier.height(BarHeight).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun BarItem(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .selectable(selected = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** An icon-only item of the pill; [description] is what screen readers announce. */
@Composable
fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color? = null) {
    BarItem(onClick, description) {
        Icon(icon, contentDescription = description, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Play/pause as a plain icon button in the pill. Tap only plays/pauses; long-press opens the recitation list (see [onLongClick]). */
@Composable
private fun PlayCapsule(isPlaying: Boolean, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(colors.primary)
    val content by animateColorAsState(colors.onPrimary)
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "توقف خوانش" else "پخش خوانش",
            tint = content,
        )
    }
}

@Composable
private fun PublishPoemChrome(
    poem: Poem?,
    isSaved: Boolean,
    isPlaying: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpenPoet: ((String) -> Unit)?,
) {
    val host = LocalPoemChromeHost.current
    DisposableEffect(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet) {
        host.current = poem?.let {
            PoemChrome(it, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet)
        }
        onDispose { host.current = null }
    }
}
