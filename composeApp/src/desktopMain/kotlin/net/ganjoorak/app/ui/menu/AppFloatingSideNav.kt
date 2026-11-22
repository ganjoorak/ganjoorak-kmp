package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.poem.LocalPoemChromeHost
import net.ganjoorak.app.util.toPersianDigits

private data class NavDestination(
    val target: MenuTarget?,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val label: String,
)

private val PrimaryDestinations = listOf(
    NavDestination(MenuTarget.Home, Icons.Filled.Home, label = "صفحهٔ اصلی"),
    NavDestination(MenuTarget.Search, Icons.Filled.Search, label = "جست‌وجو"),
    NavDestination(
        MenuTarget.Favorites,
        Icons.Filled.FavoriteBorder,
        Icons.Filled.Favorite,
        "علاقه‌مندی‌ها",
    ),
    NavDestination(MenuTarget.Profile, Icons.Filled.AccountCircle, label = "حساب"),
)

private val SecondaryDestinations = listOf(
    NavDestination(MenuTarget.Explore, Icons.Filled.Explore, label = "کاوش"),
    NavDestination(MenuTarget.Poets, Icons.Filled.People, label = "شاعران"),
    NavDestination(MenuTarget.Settings, Icons.Filled.Settings, label = "تنظیمات"),
)

/**
 * Desktop: a Material 3 Expressive [WideNavigationRail] on the leading (right, in RTL)
 * edge. Collapsed it shows the essential destinations; the header menu expands it over
 * the content with a scrim (no reserved gutter) to reveal secondary destinations and
 * stories.
 *
 * [androidx.compose.material3.ModalWideNavigationRail] is the usual component for this
 * overlay, but its expanded pane is hosted in a Compose Desktop [androidx.compose.ui.window.Dialog]
 * that lays out on the left of the window. We keep the official [WideNavigationRail] in an
 * overlay [Box] and add a scrim so expansion stays on the right and does not push content.
 */
@Composable
fun AppFloatingSideNav(
    current: MenuTarget?,
    favoritesCount: Int,
    hasUnseenStories: Boolean,
    onSelect: (MenuTarget) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberWideNavigationRailState(WideNavigationRailValue.Collapsed)
    val scope = rememberCoroutineScope()
    val expanded = state.targetValue == WideNavigationRailValue.Expanded
    val graph = AppGraph.instance
    val storyState by graph.stories.state.collectAsState()
    val player by graph.player.state.collectAsState()
    val openStories = LocalAppActions.current.openStories
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)

    fun go(target: MenuTarget) {
        if (target == MenuTarget.Search) onOpenSearch() else onSelect(target)
        if (expanded) scope.launch { state.collapse() }
    }

    Box(modifier.fillMaxSize()) {
        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { scope.launch { state.collapse() } },
            )
        }
        // In RTL, CenterStart is the physical right edge, and the rail grows inward.
        WideNavigationRail(
            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(),
            state = state,
            arrangement = Arrangement.Center,
            header = {
                IconButton(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .semantics {
                            stateDescription = if (expanded) "گسترده" else "جمع‌شده"
                        },
                    onClick = {
                        scope.launch {
                            if (expanded) state.collapse() else state.expand()
                        }
                    },
                ) {
                    val icon = @Composable {
                        Icon(
                            if (expanded) Icons.Filled.MenuOpen else Icons.Filled.Menu,
                            contentDescription = if (expanded) "بستن ناوبری" else "باز کردن ناوبری",
                        )
                    }
                    if (hasUnseenStories) {
                        BadgedBox(badge = { Badge(Modifier.clip(CircleShape).size(8.dp)) }) { icon() }
                    } else {
                        icon()
                    }
                }
            },
        ) {
            for (item in PrimaryDestinations) {
                RailDestinationItem(
                    item = item,
                    selected = current == item.target,
                    railExpanded = expanded,
                    badge = if (item.target == MenuTarget.Favorites) {
                        favoritesCount.takeIf { it > 0 }?.toPersianDigits()
                    } else {
                        null
                    },
                    onClick = { item.target?.let(::go) },
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                for (item in SecondaryDestinations) {
                    RailDestinationItem(
                        item = item,
                        selected = current == item.target,
                        railExpanded = true,
                        onClick = { item.target?.let(::go) },
                    )
                }
                RailDestinationItem(
                    item = NavDestination(null, Icons.Filled.AutoStories, label = "تکه‌ها"),
                    selected = false,
                    railExpanded = true,
                    badge = if (hasUnseenStories) "" else null,
                    onClick = {
                        scope.launch { state.collapse() }
                        if (storyState.people.isNotEmpty()) {
                            val start = storyState.people
                                .indexOfFirst { storyState.isUnseen(it) }
                                .let { if (it < 0) 0 else it }
                            openStories(storyState.people, start)
                        }
                    },
                )
            }
        }
        // The current poem's actions sit in the empty space under the centered
        // destinations, still inside the rail's narrow column, so the reading
        // area stays untouched. Hidden while the expanded rail is up.
        if (!expanded) {
            PoemRailActions(
                bottomPad = 16.dp + if (player.isActive) 80.dp else 0.dp,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/** The collapsed [WideNavigationRail] column width (ContainerWidth token: 96dp). */
private val RailColumnWidth = 96.dp

/**
 * The current poem's actions as a small vertical group in the rail column, near
 * the bottom of the window: poet, like, share, download, and a featured play.
 * Download starts a single-poem offline item; when one exists it is removed
 * instead, so a parent book or poet item is never taken apart from here.
 * [bottomPad] keeps the group above the mini player bar while it is visible.
 */
@Composable
private fun PoemRailActions(bottomPad: Dp, modifier: Modifier = Modifier) {
    val chrome = LocalPoemChromeHost.current.current ?: return
    val downloads = AppGraph.instance.downloads
    val items by downloads.items.collectAsState()
    val jobs by downloads.active.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val poem = chrome.poem
    val poemId = "poem:${poem.ref.key}"
    val savedAlone = items.any { it.id == poemId }
    val saved = savedAlone || items.any { poem.ref in it.completedRefs }
    val working = jobs.any { job -> items.any { it.id == job.itemId && poem.ref in it.totalRefs } }
    val poetKey = poem.poetKey

    Box(
        modifier
            .width(RailColumnWidth)
            .padding(bottom = bottomPad),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainer,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                Modifier.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
                    Icon(Icons.Filled.Share, contentDescription = "هم‌رسانی", tint = colors.onSurfaceVariant)
                }
                IconButton(
                    onClick = {
                        when {
                            working -> Unit
                            savedAlone -> downloads.remove(poemId)
                            else -> scope.launch { downloads.downloadPoem(poem) }
                        }
                    },
                ) {
                    if (working) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (saved) Icons.Filled.Check else Icons.Filled.Download,
                            contentDescription = if (saved) {
                                "این شعر برای خواندن آفلاین ذخیره است"
                            } else {
                                "بارگیری این شعر برای خواندن آفلاین"
                            },
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
                if (poem.recitations.isNotEmpty()) {
                    FilledIconButton(
                        onClick = chrome.onPlay,
                        colors = if (chrome.isPlaying) {
                            IconButtonDefaults.filledIconButtonColors()
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        },
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
}

@Composable
private fun RailDestinationItem(
    item: NavDestination,
    selected: Boolean,
    railExpanded: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    WideNavigationRailItem(
        selected = selected,
        onClick = onClick,
        railExpanded = railExpanded,
        icon = {
            val icon = @Composable {
                Icon(
                    if (selected) item.selectedIcon else item.icon,
                    contentDescription = null,
                )
            }
            when {
                badge == null -> icon()
                badge.isEmpty() -> BadgedBox(badge = { Badge(Modifier.clip(CircleShape).size(8.dp)) }) { icon() }
                else -> BadgedBox(badge = { Badge { Text(badge) } }) { icon() }
            }
        },
        label = { Text(item.label) },
    )
}
