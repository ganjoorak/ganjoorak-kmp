package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.poem.LocalPoemChromeHost

private val GroupWidth = 64.dp
private val GroupShape = RoundedCornerShape(50)

/**
 * Desktop-only poem actions, styled like a low-emphasis M3E vertical floating
 * toolbar. Poet, like and share always; play is featured when the poem has
 * audio. Hidden when no poem is on screen. Sits on the right, separate from
 * the destination rail.
 */
@Composable
fun DesktopPoemActionGroup(modifier: Modifier = Modifier) {
    val chrome = LocalPoemChromeHost.current.current ?: return
    val poem = chrome.poem
    val colors = MaterialTheme.colorScheme
    val poetKey = poem.poetKey
    Surface(
        modifier = modifier.width(GroupWidth),
        shape = GroupShape,
        color = colors.surfaceContainer,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (poetKey != null && chrome.onOpenPoet != null) {
                IconButton(onClick = { chrome.onOpenPoet.invoke(poetKey) }) {
                    PoetAvatar(poem.poetName, poem.poetImageUrl, 28.dp)
                }
            }
            IconButton(onClick = chrome.onToggleSave) {
                Icon(
                    if (chrome.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (chrome.isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
                    tint = if (chrome.isSaved) colors.error else colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = chrome.onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "هم‌رسانی",
                    tint = colors.onSurfaceVariant,
                )
            }
            if (poem.recitations.isNotEmpty()) {
                FilledIconButton(
                    onClick = chrome.onPlay,
                    modifier = Modifier.size(width = 48.dp, height = 56.dp),
                    colors = if (chrome.isPlaying) {
                        IconButtonDefaults.filledIconButtonColors()
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    },
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(
                        if (chrome.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (chrome.isPlaying) "توقف خوانش" else "پخش خوانش",
                    )
                }
            }
        }
    }
}
