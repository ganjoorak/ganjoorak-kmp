package net.ganjoorak.app.ui.poem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
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

/** Space reserved under a poem so verses are not hidden by the pill and/or the phone destination bar. */
@Composable
fun poemChromeBottomSpace(): Dp {
    val persistent = LocalPersistentNavPadding.current.calculateBottomPadding()
    return if (usesSideNavigation) 16.dp else PoemControlsHeight + persistent
}

/**
 * The poem's play / like / share / poet controls.
 * On the phone they stay a floating pill at the bottom of the poem (the menu
 * button is gone now that the destination bar is always on screen). On desktop
 * they move into the floating side nav and this pill is not drawn.
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
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    PublishPoemChrome(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet)
    if (usesSideNavigation) return

    val colors = MaterialTheme.colorScheme
    val persistentBottom = LocalPersistentNavPadding.current.calculateBottomPadding()
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp + persistentBottom),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (poem != null) {
            if (poem.recitations.isNotEmpty()) {
                Pill { PlayCapsule(isPlaying, onPlay) }
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

/** Play/pause as the pill's highlighted capsule, the way the selected tab shows its label. */
@Composable
private fun PlayCapsule(isPlaying: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (isPlaying) colors.primary else colors.primaryContainer)
    val content by animateColorAsState(if (isPlaying) colors.onPrimary else colors.onPrimaryContainer)
    Row(
        Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(container)
            .selectable(selected = isPlaying, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "توقف خوانش" else "پخش خوانش",
            tint = content,
        )
        AnimatedVisibility(visible = isPlaying, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Text(
                "در حال پخش",
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
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
