package net.ganjoorak.app.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.poem.LocalPoemChromeHost

private val GroupWidth = 52.dp
private val GroupShape = RoundedCornerShape(26.dp)

/**
 * Desktop-only poem actions: a compact vertical floating group, sitting
 * just to the left of the destination rail. Poet, like and share always;
 * play only when the poem has audio. Hidden when no poem is on screen.
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
        color = colors.surfaceContainerHigh,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (poetKey != null && chrome.onOpenPoet != null) {
                GroupItem(onClick = { chrome.onOpenPoet.invoke(poetKey) }, description = poem.poetName) {
                    PoetAvatar(poem.poetName, poem.poetImageUrl, 28.dp)
                }
            }
            GroupItem(
                onClick = chrome.onToggleSave,
                description = if (chrome.isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
            ) {
                Icon(
                    if (chrome.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (chrome.isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
                    tint = if (chrome.isSaved) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            GroupItem(onClick = chrome.onShare, description = "هم‌رسانی") {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "هم‌رسانی",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (poem.recitations.isNotEmpty()) {
                val container by animateColorAsState(if (chrome.isPlaying) colors.primary else Color.Transparent)
                val content by animateColorAsState(
                    if (chrome.isPlaying) colors.onPrimary else colors.onSurfaceVariant,
                )
                GroupItem(
                    onClick = chrome.onPlay,
                    description = if (chrome.isPlaying) "توقف خوانش" else "پخش خوانش",
                    indicator = container,
                ) {
                    Icon(
                        if (chrome.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (chrome.isPlaying) "توقف خوانش" else "پخش خوانش",
                        tint = content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupItem(
    onClick: () -> Unit,
    description: String,
    indicator: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(indicator)
            .semantics { contentDescription = description }
            .selectable(selected = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
