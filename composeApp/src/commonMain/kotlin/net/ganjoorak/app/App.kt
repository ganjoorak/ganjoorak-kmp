package net.ganjoorak.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.ui.auth.AuthScreen
import net.ganjoorak.app.ui.components.AppActions
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.explore.ExploreScreen
import net.ganjoorak.app.ui.feed.FeedScreen
import net.ganjoorak.app.ui.menu.AppMenu
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.menu.MenuTarget
import net.ganjoorak.app.ui.navigation.PredictiveBackTransitions
import net.ganjoorak.app.ui.player.MiniPlayer
import net.ganjoorak.app.ui.poem.PoemScreen
import net.ganjoorak.app.ui.poets.CategoryScreen
import net.ganjoorak.app.ui.poets.FollowingScreen
import net.ganjoorak.app.ui.poets.PoetScreen
import net.ganjoorak.app.ui.poets.PoetsScreen
import net.ganjoorak.app.ui.profile.ProfileScreen
import net.ganjoorak.app.ui.saved.SavedScreen
import net.ganjoorak.app.ui.search.SearchScreen
import net.ganjoorak.app.ui.settings.SettingsScreen
import net.ganjoorak.app.ui.theme.GanjoorakTheme

private fun AppRoute.menuTarget(): MenuTarget? = when (this) {
    FeedRoute -> MenuTarget.Home
    SearchRoute -> MenuTarget.Search
    ExploreRoute -> MenuTarget.Explore
    PoetsRoute -> MenuTarget.Poets
    SavedRoute -> MenuTarget.Saved
    FollowingRoute -> MenuTarget.Following
    ProfileRoute -> MenuTarget.Profile
    SettingsRoute -> MenuTarget.Settings
    is LoginRoute -> MenuTarget.Profile
    else -> null
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

/**
 * Like the site: no tab bar. Every screen reaches the right-side drawer menu and search from its
 * own controls; the drawer lists the destinations.
 */
@Composable
private fun AppShell() {
    val graph = AppGraph.instance
    val backStack = rememberNavBackStack(appNavConfiguration, FeedRoute)
    val current = backStack.lastOrNull() as? AppRoute
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var pickingPoets by rememberSaveable { mutableStateOf(false) }

    val goTo: (AppRoute) -> Unit = { route -> backStack.switchTo(route) }
    val actions = remember(backStack) {
        AppActions(
            openMenu = { menuOpen = true },
            openSearch = { backStack.open(SearchRoute, singleTop = true) },
        )
    }

    CompositionLocalProvider(LocalAppActions provides actions) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AppNavDisplay(backStack, onEditFollowing = { pickingPoets = true })
            }
            MiniPlayer(
                graph.player,
                onOpenPoem = { backStack.open(it.route()) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }

        if (menuOpen) {
            ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                AppMenu(
                    current = current?.menuTarget(),
                    onSelect = { target ->
                        menuOpen = false
                        when (target) {
                            MenuTarget.Search -> backStack.open(SearchRoute, singleTop = true)
                            MenuTarget.Home -> goTo(FeedRoute)
                            MenuTarget.Explore -> goTo(ExploreRoute)
                            MenuTarget.Poets -> goTo(PoetsRoute)
                            MenuTarget.Saved -> goTo(SavedRoute)
                            MenuTarget.Following -> goTo(FollowingRoute)
                            MenuTarget.Profile -> goTo(ProfileRoute)
                            MenuTarget.Settings -> goTo(SettingsRoute)
                        }
                    },
                    onClose = { menuOpen = false },
                )
            }
        }
    }

    if (pickingPoets) FeedPoetsSheet(onDismiss = { pickingPoets = false })
}

@Composable
private fun AppNavDisplay(backStack: MutableList<NavKey>, onEditFollowing: () -> Unit) {
    val graph = AppGraph.instance
    val openPoem: (PoemRef) -> Unit = { backStack.open(it.route()) }
    val openPoet: (String) -> Unit = { backStack.open(PoetRoute(it)) }
    val openCategory: (Int) -> Unit = { backStack.open(CategoryRoute(it)) }
    val back: () -> Unit = { if (!backStack.pop()) backStack.switchTo(FeedRoute) }
    val home: () -> Unit = { backStack.switchTo(FeedRoute) }

    PredictiveBackTransitions.TrackGesture()
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            remember { PredictiveBackTransitions.entryDecorator<NavKey>() },
        ),
        transitionSpec = { PredictiveBackTransitions.push() },
        popTransitionSpec = { PredictiveBackTransitions.pop() },
        predictivePopTransitionSpec = { swipeEdge -> PredictiveBackTransitions.predictivePop(swipeEdge) },
        entryProvider = entryProvider {
            entry<FeedRoute> {
                FeedScreen(
                    onOpenPoem = openPoem,
                    onOpenPoet = openPoet,
                    onOpenCategory = openCategory,
                    onOpenPoets = { backStack.open(PoetsRoute) },
                )
            }
            entry<ExploreRoute> { ExploreScreen(onBack = back, onOpenPoem = openPoem, onOpenPoet = openPoet) }
            entry<PoetsRoute> { PoetsScreen(onBack = back, onOpenPoet = openPoet) }
            entry<SearchRoute> { SearchScreen(onBack = back, onOpenPoem = openPoem, onOpenPoet = openPoet) }
            entry<SavedRoute> { SavedScreen(onBack = back, onOpenPoem = openPoem, onOpenFeed = home) }
            entry<FollowingRoute> {
                FollowingScreen(
                    onBack = back,
                    onOpenPoet = openPoet,
                    onOpenPoets = { backStack.open(PoetsRoute) },
                    onEditFollowing = onEditFollowing,
                    onReadFollowing = {
                        graph.settings.setFeedMode(FeedMode.Following)
                        home()
                    },
                )
            }
            entry<ProfileRoute> {
                ProfileScreen(
                    onBack = back,
                    onOpenPoem = openPoem,
                    onOpenSaved = { backStack.open(SavedRoute) },
                    onOpenFollowing = { backStack.open(FollowingRoute) },
                    onOpenSettings = { backStack.open(SettingsRoute) },
                    onOpenLogin = { signup -> backStack.open(LoginRoute(signup), singleTop = true) },
                )
            }
            entry<LoginRoute> { route ->
                AuthScreen(
                    startSignup = route.signup,
                    onBack = back,
                    onSignedIn = {
                        if (!backStack.pop()) backStack.switchTo(ProfileRoute)
                        else if (backStack.lastOrNull() != ProfileRoute) backStack.switchTo(ProfileRoute)
                    },
                )
            }
            entry<SettingsRoute> { SettingsScreen(onBack = back) }
            entry<PoemRoute> { route ->
                PoemScreen(
                    ref = route.ref,
                    onBack = back,
                    // Stepping through next/previous replaces the current poem instead of growing the stack.
                    onOpenPoem = { ref -> backStack.replaceTop(ref.route()) },
                    onOpenPoet = openPoet,
                    onOpenCategory = openCategory,
                )
            }
            entry<PoetRoute> { route ->
                PoetScreen(
                    poetKey = route.key,
                    onBack = back,
                    onOpenPoem = openPoem,
                    onOpenCategory = openCategory,
                )
            }
            entry<CategoryRoute> { route ->
                CategoryScreen(
                    categoryId = route.id,
                    onBack = back,
                    onOpenPoem = openPoem,
                    onOpenPoet = openPoet,
                    onOpenCategory = openCategory,
                )
            }
        },
    )
}
