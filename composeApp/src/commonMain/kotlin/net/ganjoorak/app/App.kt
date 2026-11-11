package net.ganjoorak.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import net.ganjoorak.app.data.account.StoryPerson
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.ui.auth.AuthScreen
import net.ganjoorak.app.ui.components.AppActions
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.explore.ExploreScreen
import net.ganjoorak.app.ui.feed.FeedScreen
import net.ganjoorak.app.ui.menu.AppFloatingBottomNav
import net.ganjoorak.app.ui.menu.AppFloatingSideNav
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.menu.FloatingNavMargin
import net.ganjoorak.app.ui.menu.FloatingNavOccupiedBottom
import net.ganjoorak.app.ui.menu.LocalPersistentNavPadding
import net.ganjoorak.app.ui.menu.MenuTarget
import net.ganjoorak.app.ui.navigation.PredictiveBackTransitions
import net.ganjoorak.app.ui.player.MiniPlayer
import net.ganjoorak.app.ui.poem.LocalPoemChromeHost
import net.ganjoorak.app.ui.poem.PoemChromeHost
import net.ganjoorak.app.ui.poem.PoemScreen
import net.ganjoorak.app.ui.poets.CategoryScreen
import net.ganjoorak.app.ui.poets.PoetScreen
import net.ganjoorak.app.ui.poets.PoetsScreen
import net.ganjoorak.app.ui.profile.ProfileScreen
import net.ganjoorak.app.ui.saved.FavoritesScreen
import net.ganjoorak.app.ui.saved.FavoritesSection
import net.ganjoorak.app.ui.search.SearchScreen
import net.ganjoorak.app.ui.settings.SettingsScreen
import net.ganjoorak.app.ui.stories.StoryViewer
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.theme.GanjoorakTheme

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
 * Persistent destination chrome, shared across platforms: a floating vertical
 * bar on the physical right of desktop windows, and a floating bottom bar on
 * the phone. Search is a compact top action on desktop and lives on Explore
 * on the phone. Poem play/like/share move into the desktop bar when a poem
 * is on screen; the phone keeps its bottom pill.
 */
@Composable
private fun AppShell() {
    val graph = AppGraph.instance
    val backStack = rememberNavBackStack(appNavConfiguration, FeedRoute)
    val current = backStack.lastOrNull() as? AppRoute
    var pickingPoets by rememberSaveable { mutableStateOf(false) }
    var storyViewer by remember { mutableStateOf<Pair<List<StoryPerson>, Int>?>(null) }
    val storyState by graph.stories.state.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val poemChromeHost = remember { PoemChromeHost() }

    val goTo: (AppRoute) -> Unit = { route -> backStack.switchTo(route) }
    val openSearch = { backStack.open(SearchRoute, singleTop = true) }
    val selectMenu: (MenuTarget) -> Unit = { target ->
        when (target) {
            MenuTarget.Search -> openSearch()
            MenuTarget.Home -> goTo(FeedRoute)
            MenuTarget.Explore -> goTo(ExploreRoute)
            MenuTarget.Poets -> goTo(PoetsRoute)
            MenuTarget.Favorites -> if (current !is SavedRoute && current !is FollowingRoute) goTo(SavedRoute)
            MenuTarget.Profile -> goTo(ProfileRoute)
            MenuTarget.Settings -> goTo(SettingsRoute)
        }
    }
    val actions = AppActions(
        openSearch = openSearch,
        openStories = { people, index -> storyViewer = people to index },
    )

    val persistentPadding = if (usesSideNavigation) {
        PaddingValues(0.dp)
    } else {
        PaddingValues(bottom = FloatingNavOccupiedBottom)
    }

    CompositionLocalProvider(
        LocalAppActions provides actions,
        LocalPoemChromeHost provides poemChromeHost,
        LocalPersistentNavPadding provides persistentPadding,
    ) {
        if (usesSideNavigation) {
            // Pin the floating bar to the physical right; keep content RTL.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Box(Modifier.weight(1f)) {
                                AppNavDisplay(backStack, onEditFollowing = { pickingPoets = true })
                            }
                            MiniPlayer(
                                graph.player,
                                onOpenPoem = { backStack.open(it.route()) },
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            )
                        }
                    }
                    Box(Modifier.padding(FloatingNavMargin)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            AppFloatingSideNav(
                                current = current?.menuTarget(),
                                favoritesCount = settings.followedPoets.size,
                                hasUnseenStories = storyState.hasUnseen,
                                poemChrome = poemChromeHost.current,
                                onSelect = selectMenu,
                                onOpenSearch = openSearch,
                            )
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        AppNavDisplay(backStack, onEditFollowing = { pickingPoets = true })
                    }
                    MiniPlayer(
                        graph.player,
                        onOpenPoem = { backStack.open(it.route()) },
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = FloatingNavOccupiedBottom),
                    )
                }
                AppFloatingBottomNav(
                    current = current?.menuTarget(),
                    favoritesCount = settings.followedPoets.size,
                    hasUnseenStories = storyState.hasUnseen,
                    onSelect = selectMenu,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = FloatingNavMargin, vertical = FloatingNavMargin),
                )
            }
        }

        LaunchedEffect(Unit) { graph.stories.refresh() }
    }

    if (pickingPoets) FeedPoetsSheet(onDismiss = { pickingPoets = false })

    val viewer = storyViewer
    if (viewer != null) {
        StoryViewer(
            people = viewer.first,
            startIndex = viewer.second,
            seen = storyState.seen,
            onSeen = { username, quoteId -> graph.stories.markSeen(username, quoteId) },
            onClose = { storyViewer = null },
            onOpenPoem = { ref ->
                storyViewer = null
                backStack.open(ref.route())
            },
        )
    }
}

@Composable
private fun AppNavDisplay(backStack: MutableList<NavKey>, onEditFollowing: () -> Unit) {
    val graph = AppGraph.instance
    val openPoem: (PoemRef) -> Unit = { backStack.open(it.route()) }
    val openPoet: (String) -> Unit = { backStack.open(PoetRoute(it)) }
    val openCategory: (Int) -> Unit = { backStack.open(CategoryRoute(it)) }
    val back: () -> Unit = { if (!backStack.pop()) backStack.switchTo(FeedRoute) }
    val home: () -> Unit = { backStack.switchTo(FeedRoute) }
    val route = backStack.lastOrNull() as? AppRoute
    val tabBack: (() -> Unit)? = if (route != null && route.isPrimaryDestination() && backStack.size <= 2) null else back

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
            entry<ExploreRoute> { ExploreScreen(onBack = tabBack, onOpenPoem = openPoem, onOpenPoet = openPoet) }
            entry<PoetsRoute> { PoetsScreen(onBack = tabBack, onOpenPoet = openPoet) }
            entry<SearchRoute> { SearchScreen(onBack = back, onOpenPoem = openPoem, onOpenPoet = openPoet) }
            entry<SavedRoute> {
                FavoritesScreen(
                    initialSection = FavoritesSection.Poems,
                    onBack = tabBack,
                    onOpenPoem = openPoem,
                    onOpenPoet = openPoet,
                    onOpenFeed = home,
                    onOpenPoets = { backStack.open(PoetsRoute) },
                    onEditFollowing = onEditFollowing,
                    onReadFavorites = {
                        graph.settings.setFeedMode(FeedMode.Following)
                        home()
                    },
                )
            }
            entry<FollowingRoute> {
                FavoritesScreen(
                    initialSection = FavoritesSection.Poets,
                    onBack = tabBack,
                    onOpenPoem = openPoem,
                    onOpenPoet = openPoet,
                    onOpenFeed = home,
                    onOpenPoets = { backStack.open(PoetsRoute) },
                    onEditFollowing = onEditFollowing,
                    onReadFavorites = {
                        graph.settings.setFeedMode(FeedMode.Following)
                        home()
                    },
                )
            }
            entry<ProfileRoute> {
                ProfileScreen(
                    onBack = tabBack,
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
                    onOpenLogin = { backStack.open(LoginRoute(false), singleTop = true) },
                    onOpenProfile = { backStack.open(ProfileRoute, singleTop = true) },
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
