package net.ganjoorak.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.ui.feed.FeedScreen
import net.ganjoorak.app.ui.player.MiniPlayer
import net.ganjoorak.app.ui.poem.PoemScreen
import net.ganjoorak.app.ui.poets.CategoryScreen
import net.ganjoorak.app.ui.poets.PoetScreen
import net.ganjoorak.app.ui.poets.PoetsScreen
import net.ganjoorak.app.ui.profile.ProfileScreen
import net.ganjoorak.app.ui.saved.SavedScreen
import net.ganjoorak.app.ui.search.SearchScreen
import net.ganjoorak.app.ui.theme.GanjoorakTheme

@Serializable data object FeedRoute
@Serializable data object PoetsRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object ProfileRoute
@Serializable data class PoemRoute(val id: Int, val source: PoetSource = PoetSource.Ganjoor, val poet: String = "") {
    val ref: PoemRef get() = PoemRef(source, id, poet)
}
@Serializable data class PoetRoute(val key: String)

private fun PoemRef.route() = PoemRoute(id, source, poetSlug)
@Serializable data class CategoryRoute(val id: Int)

private enum class Tab(
    val route: Any,
    val matches: (NavDestination) -> Boolean,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Feed(FeedRoute, { it.hasRoute<FeedRoute>() }, "خانه", Icons.Outlined.Home, Icons.Filled.Home),
    Poets(PoetsRoute, { it.hasRoute<PoetsRoute>() }, "شاعران", Icons.Outlined.People, Icons.Filled.People),
    Search(SearchRoute, { it.hasRoute<SearchRoute>() }, "جستجو", Icons.Filled.Search, Icons.Filled.Search),
    // Saved poems open from the profile page, so the profile tab stays lit there.
    Profile(
        ProfileRoute,
        { it.hasRoute<ProfileRoute>() || it.hasRoute<SavedRoute>() },
        "پروفایل",
        Icons.Outlined.Person,
        Icons.Filled.Person,
    ),
}

/** Root composable shared by every platform. */
@Composable
fun App() {
    val graph = AppGraph.instance
    val settings by graph.settings.settings.collectAsState()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { AppGraph.instance.imageHttp })) }
            .crossfade(true)
            .build()
    }
    GanjoorakTheme(settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppShell()
        }
    }
}

@Composable
private fun AppShell() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    var currentTab by rememberSaveable { mutableStateOf(Tab.Feed) }
    LaunchedEffect(entry) {
        val destination = entry?.destination ?: return@LaunchedEffect
        Tab.entries.firstOrNull { it.matches(destination) }?.let { currentTab = it }
    }
    val selectTab: (Tab) -> Unit = { tab ->
        currentTab = tab
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val player = AppGraph.instance.player
    val openPoem: (PoemRef) -> Unit = { nav.navigate(it.route()) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    Tab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = tab == currentTab,
                            onClick = { selectTab(tab) },
                            icon = { Icon(if (tab == currentTab) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
                Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
                    Box(Modifier.weight(1f)) { AppNavHost(nav, openPoem) }
                    MiniPlayer(player, openPoem)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { AppNavHost(nav, openPoem) }
                MiniPlayer(player, openPoem)
                FloatingTabBar(currentTab, selectTab)
            }
        }
    }
}

/**
 * Material 3 Expressive style floating bar: the tabs sit in a pill where the selected one grows
 * into a labelled capsule, and search gets its own round button beside it.
 */
@Composable
private fun FloatingTabBar(current: Tab, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = colors.surfaceContainerHigh, shadowElevation = 6.dp) {
            Row(
                Modifier.height(BarHeight).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.filter { it != Tab.Search }.forEach { tab ->
                    PillTab(tab, selected = tab == current, onClick = { onSelect(tab) })
                }
            }
        }
        val searching = current == Tab.Search
        Surface(
            selected = searching,
            onClick = { onSelect(Tab.Search) },
            shape = CircleShape,
            color = if (searching) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (searching) colors.onPrimaryContainer else colors.onSurfaceVariant,
            shadowElevation = 6.dp,
            modifier = Modifier.size(BarHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Tab.Search.icon, contentDescription = Tab.Search.label)
            }
        }
    }
}

@Composable
private fun PillTab(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent)
    val content by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
    Row(
        Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) tab.selectedIcon else tab.icon,
            // When selected the label is spelled out next to the icon.
            contentDescription = tab.label.takeUnless { selected },
            tint = content,
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(
                tab.label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private val BarHeight = 64.dp

@Composable
private fun AppNavHost(nav: NavHostController, openPoem: (PoemRef) -> Unit) {
    val openPoet: (String) -> Unit = { nav.navigate(PoetRoute(it)) }
    val openCategory: (Int) -> Unit = { nav.navigate(CategoryRoute(it)) }
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = FeedRoute) {
        composable<FeedRoute> {
            FeedScreen(
                onOpenPoem = openPoem,
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
            )
        }
        composable<PoetsRoute> { PoetsScreen(onOpenPoet = openPoet) }
        composable<SearchRoute> { SearchScreen(onOpenPoem = openPoem) }
        composable<ProfileRoute> { ProfileScreen(onOpenSaved = { nav.navigate(SavedRoute) }) }
        composable<SavedRoute> { SavedScreen(onBack = back, onOpenPoem = openPoem) }
        composable<PoemRoute> { entry ->
            PoemScreen(
                ref = entry.toRoute<PoemRoute>().ref,
                onBack = back,
                // Stepping through next/previous replaces the current poem instead of growing the stack.
                onOpenPoem = { ref -> nav.navigate(ref.route()) { popUpTo<PoemRoute> { inclusive = true } } },
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
            )
        }
        composable<PoetRoute> { entry ->
            PoetScreen(
                poetKey = entry.toRoute<PoetRoute>().key,
                onBack = back,
                onOpenPoem = openPoem,
                onOpenCategory = openCategory,
            )
        }
        composable<CategoryRoute> { entry ->
            CategoryScreen(
                categoryId = entry.toRoute<CategoryRoute>().id,
                onBack = back,
                onOpenPoem = openPoem,
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
            )
        }
    }
}
