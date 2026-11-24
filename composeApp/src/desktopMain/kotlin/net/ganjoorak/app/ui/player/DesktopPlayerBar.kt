package net.ganjoorak.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.player.PlayerController
import net.ganjoorak.app.player.PlayerState

/**
 * Wide desktop now-playing bar (Spotify-style), rendered at the bottom of the
 * desktop window instead of the phone [MiniPlayer] pill. Desktop only.
 *
 * RTL layout: track info on the leading (right) side, transport + seek in the
 * center, recitation list on the trailing (left) side. Reads the exact same
 * [PlayerController] state and the same [LocalRecitationListHost] as the phone
 * pill, so the poem screen's long-press on play opens the recitation list here
 * too. No volume control: [net.ganjoorak.app.platform.AudioPlayer] has no
 * volume API, so adding one needs new platform plumbing, not just UI.
 */
@Composable
fun DesktopPlayerBar(player: PlayerController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    val listHost = LocalRecitationListHost.current
    if (!state.isActive) listHost.open = false

    AnimatedVisibility(
        visible = state.isActive,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                val expanded = listHost.open && state.recitations.isNotEmpty()
                AnimatedVisibility(visible = expanded) {
                    RecitationQueue(
                        recitations = state.recitations,
                        currentId = state.recitation?.id,
                        isPlaying = state.isPlaying,
                        onSelect = {
                            player.playRecitation(it)
                            listHost.open = false
                        },
                    )
                }
                // RecitationQueue already ends with a divider when expanded.
                if (!expanded) HorizontalDivider()
                DesktopPlayerBarRow(
                    player = player,
                    state = state,
                    listOpen = listHost.open,
                    onToggleList = { listHost.open = !listHost.open },
                    onOpenList = { listHost.open = true },
                )
            }
        }
    }
}

@Composable
private fun DesktopPlayerBarRow(
    player: PlayerController,
    state: PlayerState,
    listOpen: Boolean,
    onToggleList: () -> Unit,
    onOpenList: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val settingsStore = AppGraph.instance.settings
    val settings by settingsStore.settings.collectAsState()
    val followVerses = settings.autoScrollWithAudio

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading (right in RTL): poem title + poet + current recitation.
        TrackInfo(state = state, modifier = Modifier.weight(1f).padding(end = 16.dp))

        // Center: transport controls + seek slider.
        Column(
            Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Media controls stay LTR, like the phone pill.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = player::playPrevious,
                        enabled = state.hasPreviousRecitation,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "خوانش قبلی")
                    }
                    DesktopPlayButton(
                        isPlaying = state.isPlaying,
                        isBuffering = state.isBuffering,
                        onClick = player::toggle,
                        onLongClick = onOpenList,
                    )
                    IconButton(
                        onClick = player::playNext,
                        enabled = state.hasNextRecitation,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "خوانش بعدی")
                    }
                    val eye = @Composable {
                        Icon(
                            if (followVerses) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (followVerses) "خاموش کردن دنبال کردن بیت‌ها" else "دنبال کردن بیت‌ها هنگام خوانش",
                        )
                    }
                    if (followVerses) {
                        FilledTonalIconButton(
                            onClick = { settingsStore.update { it.copy(autoScrollWithAudio = false) } },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) { eye() }
                    } else {
                        IconButton(
                            onClick = { settingsStore.update { it.copy(autoScrollWithAudio = true) } },
                            modifier = Modifier.size(40.dp),
                        ) { eye() }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatClock(state.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    SeekSlider(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onSeek = player::seekTo,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(
                        formatClock(state.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // Trailing (left in RTL): recitation list toggle.
        val listButton = @Composable {
            Icon(
                Icons.Filled.FormatListBulleted,
                contentDescription = if (listOpen) "بستن فهرست خوانش‌ها" else "خوانش‌ها",
            )
        }
        if (listOpen) {
            FilledTonalIconButton(onClick = onToggleList, modifier = Modifier.size(40.dp)) { listButton() }
        } else {
            IconButton(onClick = onToggleList, modifier = Modifier.size(40.dp)) { listButton() }
        }
    }
}

/** Poem title + poet + current recitation name, ellipsized to one line each. */
@Composable
private fun TrackInfo(state: PlayerState, modifier: Modifier = Modifier) {
    val recitationName = state.recitation?.let { recitation ->
        recitation.artist.ifBlank { recitation.title }
    }.orEmpty()
    val subtitle = listOfNotNull(
        state.poetName.ifBlank { null },
        recitationName.ifBlank { null },
    ).joinToString(" • ")
    Column(modifier) {
        Text(
            state.poemTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Play/pause, styled like the poem screen's PlayCapsule. Long-press opens the recitation list. */
@Composable
private fun DesktopPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (isPlaying) colors.primary else colors.primaryContainer)
    val content by animateColorAsState(if (isPlaying) colors.onPrimary else colors.onPrimaryContainer)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(container)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering && isPlaying) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = content,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "توقف خوانش" else "پخش خوانش",
                tint = content,
            )
        }
    }
}
