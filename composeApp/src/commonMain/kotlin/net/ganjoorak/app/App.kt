package net.ganjoorak.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import net.ganjoorak.app.ui.saved.SavedScreen
import net.ganjoorak.app.ui.search.SearchScreen
import net.ganjoorak.app.ui.settings.SettingsScreen
import net.ganjoorak.app.ui.theme.GanjoorakTheme

@Serializable data object FeedRoute
@Serializable data object PoetsRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object SettingsRoute
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
    Saved(SavedRoute, { it.hasRoute<SavedRoute>() }, "نشان‌ها", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
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
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { selectTab(tab) },
                            icon = { Icon(if (tab == currentTab) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        }
    }
}

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
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<PoetsRoute> { PoetsScreen(onOpenPoet = openPoet) }
        composable<SearchRoute> { SearchScreen(onOpenPoem = openPoem) }
        composable<SavedRoute> { SavedScreen(onOpenPoem = openPoem) }
        composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
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
