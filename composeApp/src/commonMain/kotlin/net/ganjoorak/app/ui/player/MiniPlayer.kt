package net.ganjoorak.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.player.PlayerController

/** Compact now-playing bar shown above the navigation while a recitation is loaded. */
@Composable
fun MiniPlayer(player: PlayerController, onOpenPoem: (Int) -> Unit, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    AnimatedVisibility(visible = state.isActive, modifier = modifier) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 2.dp) {
            Column {
                LinearProgressIndicator(
                    progress = { if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    IconButton(onClick = player::toggle) {
                        if (state.isBuffering && state.isPlaying) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(10.dp))
                        } else {
                            Icon(
                                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "توقف" else "پخش",
                            )
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { state.poemId?.let(onOpenPoem) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            "${state.poetName} · ${state.poemTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.recitation?.artist.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = player::stop) { Icon(Icons.Filled.Close, contentDescription = "بستن پخش") }
                }
            }
        }
    }
}
