package net.ganjoorak.app.ui.poem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.menu.LocalPersistentNavPadding
import net.ganjoorak.app.ui.player.LocalRecitationListHost
import net.ganjoorak.app.ui.player.PlayerBar
import net.ganjoorak.app.ui.player.RecitationQueue

/** Height the floating bar takes at the bottom when nothing is playing (controls row only); poem lists pad their end by this much. */
val PoemControlsHeight = 80.dp

/**
 * Height the floating bar takes while a recitation is playing: the player
 * row (48dp) + divider + controls row (48dp) + the 10dp margins, rounded up.
 * [poemChromeBottomSpace] switches to this so verses are never hidden
 * under the attached player.
 */
val PoemControlsHeightPlaying = 124.dp

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
 * Space reserved under a poem so verses are not hidden by the floating bar.
 * Desktop has no bottom chrome (actions sit in the rail column), so it only needs
 * the persistent nav padding. On phone the reserve follows the player: the
 * bar grows by one row when a recitation is playing, so the reserve grows
 * with it (80dp idle, 124dp playing); the recitation queue expanding is a
 * transient overlay on top and is not reserved.
 */
@Composable
fun poemChromeBottomSpace(): Dp {
    val persistent = LocalPersistentNavPadding.current.calculateBottomPadding()
    if (usesSideNavigation) return persistent
    val playerState by AppGraph.instance.player.state.collectAsState()
    val bar = if (playerState.isActive) PoemControlsHeightPlaying else PoemControlsHeight
    return bar + persistent
}

/**
 * Dispatches to the phone unified bar (menu / play / share-like-poet, with
 * the player stuck on top of it in the same container while playing) or
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
    /** Opens the playing poem when the player clock is tapped. */
    onOpenPoem: ((PoemRef) -> Unit)? = null,
) {
    if (usesSideNavigation) {
        DesktopPoemFloatingControls(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet, onPlayLongPress, extra)
    } else {
        PhonePoemFloatingControls(poem, isSaved, isPlaying, onToggleSave, onShare, onPlay, onOpenPoet, onPlayLongPress, extra, onOpenPoem)
    }
}

/**
 * Phone controls: ONE unified floating bar (Hadi 2026-11-28 — the player is
 * stuck to the controls, in the spirit of the reference screenshot): a
 * single [surfaceContainerHigh] container holding, top to bottom, the
 * recitation queue (when open), the player row (while a recitation is
 * playing) and the controls row — menu / play / share-like-poet, right to
 * left, exactly as before round 3. When nothing is playing the bar is just
 * the controls row; the player row attaches on top the moment playback
 * starts, so the play controls are never duplicated. The bottom destination
 * pill (PhoneAppChrome) sits below this bar, in flow.
 *
 * The play capsule in the controls row is the way to start THIS poem's
 * recitation: it shows whenever this poem is not the one playing (idle, or
 * another poem is playing). When this poem is playing, the player row's
 * play/pause is the only play control. Long-press on play opens the
 * recitation list, as before. The menu chip stays visible even when there
 * is no poem (empty or «دنبال‌شده‌ها» feed) — it is the only entry to the
 * menu sheet.
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
    onOpenPoem: ((PoemRef) -> Unit)? = null,
) {
    val app = LocalAppActions.current
    val colors = MaterialTheme.colorScheme
    val player = AppGraph.instance.player
    val playerState by player.state.collectAsState()
    val listHost = LocalRecitationListHost.current
    if (!playerState.isActive) listHost.open = false
    val playingThisPoem = playerState.isActive && poem != null && playerState.poemRef == poem.ref
    val showPlayCapsule = poem != null && poem.recitations.isNotEmpty() && !playingThisPoem
    val queueExpanded = listHost.open && playerState.recitations.isNotEmpty()

    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                AnimatedVisibility(visible = queueExpanded) {
                    RecitationQueue(
                        recitations = playerState.recitations,
                        currentId = playerState.recitation?.id,
                        isPlaying = playerState.isPlaying,
                        onSelect = {
                            player.playRecitation(it)
                            listHost.open = false
                        },
                    )
                }
                if (playerState.isActive) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        PlayerBar(
                            player = player,
                            state = playerState,
                            listOpen = listHost.open,
                            onToggleList = { listHost.open = !listHost.open },
                            onOpenPoem = { ref -> onOpenPoem?.invoke(ref) },
                        )
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(BarHeight)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill { BarIcon(Icons.Filled.Menu, "منو", app.openMenu) }
                    if (poem != null) {
                        if (showPlayCapsule) {
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

/** One floating group of the bar. One tonal step above the bar container so the groups read as controls sitting on the bar, not as flat paint. */
@Composable
private fun Pill(content: @Composable RowScope.() -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 6.dp) {
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
