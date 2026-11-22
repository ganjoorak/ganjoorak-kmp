package net.ganjoorak.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.Recitation
import net.ganjoorak.app.player.PlayerController
import net.ganjoorak.app.player.PlayerState
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.util.toPersianDigits

/** Compact now-playing pill, matching the website reading bar. Media controls stay LTR. */
@Composable
fun MiniPlayer(player: PlayerController, onOpenPoem: (PoemRef) -> Unit, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    var listOpen by remember { mutableStateOf(false) }
    if (!state.isActive) listOpen = false

    AnimatedVisibility(
        visible = state.isActive,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = if (usesSideNavigation) 16.dp else 8.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val expanded = listOpen && state.recitations.isNotEmpty()
            Surface(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                shape = if (expanded) RoundedCornerShape(28.dp) else RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 1.dp,
            ) {
                Column {
                    AnimatedVisibility(visible = expanded) {
                        RecitationQueue(
                            recitations = state.recitations,
                            currentId = state.recitation?.id,
                            isPlaying = state.isPlaying,
                            onSelect = {
                                player.playRecitation(it)
                                listOpen = false
                            },
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        PlayerBar(
                            player = player,
                            state = state,
                            listOpen = listOpen,
                            onToggleList = { listOpen = !listOpen },
                            onOpenPoem = onOpenPoem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(
    player: PlayerController,
    state: PlayerState,
    listOpen: Boolean,
    onToggleList: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val settingsStore = AppGraph.instance.settings
    val settings by settingsStore.settings.collectAsState()
    val followVerses = settings.autoScrollWithAudio
    val compact = !usesSideNavigation
    val iconSize = if (compact) 36.dp else 40.dp
    val playSize = if (compact) 40.dp else 44.dp

    Row(
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = player::playPrevious,
            enabled = state.hasPreviousRecitation,
            modifier = Modifier.size(iconSize),
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "خوانش قبلی")
        }
        FilledIconButton(
            onClick = player::toggle,
            modifier = Modifier.size(playSize),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.onSurface,
                contentColor = colors.surface,
            ),
        ) {
            if (state.isBuffering && state.isPlaying) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = colors.surface,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "توقف" else "پخش",
                )
            }
        }
        IconButton(
            onClick = player::playNext,
            enabled = state.hasNextRecitation,
            modifier = Modifier.size(iconSize),
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
                modifier = Modifier.size(iconSize),
                shape = RoundedCornerShape(10.dp),
            ) { eye() }
        } else {
            IconButton(
                onClick = { settingsStore.update { it.copy(autoScrollWithAudio = true) } },
                modifier = Modifier.size(iconSize),
            ) { eye() }
        }
        SeekSlider(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = player::seekTo,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text(
            "${formatClock(state.positionMs)} / ${formatClock(state.durationMs)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 11.sp else 12.sp, letterSpacing = 0.01.sp),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clickable(enabled = state.poemRef != null) { state.poemRef?.let(onOpenPoem) }
                .padding(horizontal = 4.dp),
        )
        val listButton = @Composable {
            Icon(Icons.Filled.FormatListBulleted, contentDescription = if (listOpen) "بستن فهرست خوانش‌ها" else "خوانش‌ها")
        }
        if (listOpen) {
            FilledTonalIconButton(onClick = onToggleList, modifier = Modifier.size(iconSize)) { listButton() }
        } else {
            IconButton(onClick = onToggleList, modifier = Modifier.size(iconSize)) { listButton() }
        }
    }
}

/** Native Material 3 Slider. Lives in the LTR media-controls row, so it stays LTR in RTL chrome. */
@Composable
private fun SeekSlider(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Slider(
        value = fraction,
        onValueChange = { value ->
            if (durationMs <= 0) return@Slider
            dragging = value
            onSeek((value * durationMs).toLong())
        },
        onValueChangeFinished = { dragging = null },
        enabled = durationMs > 0,
        modifier = modifier,
    )
}

@Composable
private fun RecitationQueue(
    recitations: List<Recitation>,
    currentId: Int?,
    isPlaying: Boolean,
    onSelect: (Recitation) -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("خوانش‌ها", style = MaterialTheme.typography.titleSmall)
                Text(
                    recitations.size.toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            recitations.forEach { recitation ->
                val selected = recitation.id == currentId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(recitation) }
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            recitation.artist.ifBlank { recitation.title },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (recitation.title.isNotBlank() && recitation.title != recitation.artist) {
                            Text(
                                recitation.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (selected && isPlaying) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

/** Website-style `m:ss`, Western digits, LTR media clocks. */
private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
