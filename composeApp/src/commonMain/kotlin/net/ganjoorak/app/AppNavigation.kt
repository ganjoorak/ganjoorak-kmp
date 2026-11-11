package net.ganjoorak.app

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource

@Serializable sealed interface AppRoute : NavKey

@Serializable data object FeedRoute : AppRoute
@Serializable data object PoetsRoute : AppRoute
@Serializable data object ExploreRoute : AppRoute
@Serializable data object SearchRoute : AppRoute
@Serializable data object SavedRoute : AppRoute
@Serializable data object FollowingRoute : AppRoute
@Serializable data object ProfileRoute : AppRoute
@Serializable data object SettingsRoute : AppRoute
@Serializable data class LoginRoute(val signup: Boolean = false) : AppRoute
@Serializable data class PoemRoute(
    val id: Int,
    val source: PoetSource = PoetSource.Ganjoor,
    val poet: String = "",
) : AppRoute {
    val ref: PoemRef get() = PoemRef(source, id, poet)
}
@Serializable data class PoetRoute(val key: String) : AppRoute
@Serializable data class CategoryRoute(val id: Int) : AppRoute

internal fun PoemRef.route() = PoemRoute(id, source, poetSlug)

/**
 * Polymorphic serializers for every [AppRoute], including [PoemRoute.source]
 * ([PoetSource] is a @Serializable enum). Navigation 3 restores the back stack
 * with this module instead of Navigation 2's NavType map, which could not
 * resolve `PoetSource` on desktop and showed:
 * "Route net.ganjoorak.app.PoemRoute could not find any NavType for argument source".
 */
val appNavSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(FeedRoute::class, FeedRoute.serializer())
        subclass(PoetsRoute::class, PoetsRoute.serializer())
        subclass(ExploreRoute::class, ExploreRoute.serializer())
        subclass(SearchRoute::class, SearchRoute.serializer())
        subclass(SavedRoute::class, SavedRoute.serializer())
        subclass(FollowingRoute::class, FollowingRoute.serializer())
        subclass(ProfileRoute::class, ProfileRoute.serializer())
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(LoginRoute::class, LoginRoute.serializer())
        subclass(PoemRoute::class, PoemRoute.serializer())
        subclass(PoetRoute::class, PoetRoute.serializer())
        subclass(CategoryRoute::class, CategoryRoute.serializer())
    }
}

val appNavConfiguration = SavedStateConfiguration {
    serializersModule = appNavSerializersModule
}

/** Push [route], optionally skipping a duplicate at the top of the stack. */
internal fun MutableList<NavKey>.open(route: AppRoute, singleTop: Boolean = false) {
    if (singleTop && lastOrNull() == route) return
    add(route)
}

/** Pop back to the feed, then show [route] (or just the feed). */
internal fun MutableList<NavKey>.switchTo(route: AppRoute) {
    while (size > 1) removeAt(size - 1)
    if (route != FeedRoute) add(route)
}

/** Replace the current top entry, used when stepping between poems. */
internal fun MutableList<NavKey>.replaceTop(route: AppRoute) {
    if (isNotEmpty()) removeAt(size - 1)
    add(route)
}

/** @return false if the feed is already the only entry. */
internal fun MutableList<NavKey>.pop(): Boolean {
    if (size <= 1) return false
    removeAt(size - 1)
    return true
}
