package net.ganjoorak.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.ui.feed.FeedScreen
import net.ganjoorak.app.ui.menu.AppDrawer
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.menu.MenuDestination
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
@Serializable data class CategoryRoute(val id: Int)

private fun PoemRef.route() = PoemRoute(id, source, poetSlug)

private fun NavDestination.menuDestination(): MenuDestination? = when {
    hasRoute<FeedRoute>() -> MenuDestination.Home
    hasRoute<SearchRoute>() -> MenuDestination.Search
    hasRoute<PoetsRoute>() -> MenuDestination.Poets
    hasRoute<SavedRoute>() -> MenuDestination.Saved
    else -> null
}

private fun MenuDestination.route(): Any = when (this) {
    MenuDestination.Home -> FeedRoute
    MenuDestination.Search -> SearchRoute
    MenuDestination.Poets -> PoetsRoute
    MenuDestination.Saved -> SavedRoute
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
    val destination = entry?.destination
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pickingFeedPoets by remember { mutableStateOf(false) }

    val openMenu: () -> Unit = { scope.launch { drawer.open() } }
    val closeMenuThen: (() -> Unit) -> Unit = { action ->
        scope.launch { drawer.close() }
        action()
    }
    val goTo: (Any) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            AppDrawer(
                current = destination?.menuDestination(),
                onNavigate = { closeMenuThen { goTo(it.route()) } },
                onOpenFeedPoets = { closeMenuThen { pickingFeedPoets = true } },
                onOpenSettings = { closeMenuThen { goTo(SettingsRoute) } },
                onClose = { scope.launch { drawer.close() } },
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AppNavHost(nav, openMenu = openMenu, openSearch = { goTo(SearchRoute) })
            }
            val onPoemPage = destination?.let { it.hasRoute<FeedRoute>() || it.hasRoute<PoemRoute>() } ?: true
            if (!onPoemPage) {
                MiniPlayer(
                    AppGraph.instance.player,
                    onOpenPoem = { nav.navigate(it.route()) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }
    }

    if (pickingFeedPoets) FeedPoetsSheet(onDismiss = { pickingFeedPoets = false })
}

@Composable
private fun AppNavHost(nav: NavHostController, openMenu: () -> Unit, openSearch: () -> Unit) {
    val openPoem: (PoemRef) -> Unit = { nav.navigate(it.route()) }
    val openPoet: (String) -> Unit = { nav.navigate(PoetRoute(it)) }
    val openCategory: (Int) -> Unit = { nav.navigate(CategoryRoute(it)) }
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = FeedRoute) {
        composable<FeedRoute> {
            FeedScreen(onOpenMenu = openMenu, onOpenSearch = openSearch, onOpenPoet = openPoet, onOpenCategory = openCategory)
        }
        composable<PoetsRoute> { PoetsScreen(onOpenMenu = openMenu, onOpenPoet = openPoet) }
        composable<SearchRoute> { SearchScreen(onOpenMenu = openMenu, onOpenPoem = openPoem) }
        composable<SavedRoute> { SavedScreen(onOpenMenu = openMenu, onOpenPoem = openPoem) }
        composable<SettingsRoute> { SettingsScreen(onOpenMenu = openMenu) }
        composable<PoemRoute> { entry ->
            PoemScreen(
                ref = entry.toRoute<PoemRoute>().ref,
                onBack = back,
                onOpenSearch = openSearch,
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
