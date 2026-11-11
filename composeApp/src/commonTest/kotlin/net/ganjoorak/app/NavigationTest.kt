package net.ganjoorak.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import net.ganjoorak.app.data.PoetSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NavigationTest {
    private val json = Json { serializersModule = appNavSerializersModule }
    private val navKeySerializer = PolymorphicSerializer(NavKey::class)

    @Test
    fun poemRouteRoundTripsEveryPoetSourceThroughNavModule() {
        for (source in PoetSource.entries) {
            val route = PoemRoute(id = 42, source = source, poet = "hafez")
            val encoded = json.encodeToString(navKeySerializer, route)
            val decoded = json.decodeFromString(navKeySerializer, encoded)
            val poem = assertIs<PoemRoute>(decoded)
            assertEquals(42, poem.id)
            assertEquals(source, poem.source)
            assertEquals("hafez", poem.poet)
        }
    }

    @Test
    fun backStackOpensPoemRouteWithNonDefaultSource() {
        val stack = mutableListOf<NavKey>(FeedRoute)
        stack.open(PoetRoute("hafez"))
        stack.open(PoemRoute(1, PoetSource.Echolalia, "hafez"))
        val top = assertIs<PoemRoute>(stack.last())
        assertEquals(PoetSource.Echolalia, top.source)
        assertTrue(stack.pop())
        assertEquals(PoetRoute("hafez"), stack.last())
        assertTrue(stack.pop())
        assertFalse(stack.pop())
        assertEquals(FeedRoute, stack.last())
    }

    @Test
    fun otherRoutesOnlyUsePrimitiveArguments() {
        val poet = PoetRoute("saadi")
        val category = CategoryRoute(99)
        assertEquals(poet, json.decodeFromString(navKeySerializer, json.encodeToString(navKeySerializer, poet)))
        assertEquals(category, json.decodeFromString(navKeySerializer, json.encodeToString(navKeySerializer, category)))
        assertEquals(
            SearchRoute,
            json.decodeFromString(navKeySerializer, json.encodeToString(navKeySerializer, SearchRoute)),
        )
        assertEquals(
            LoginRoute(true),
            json.decodeFromString(navKeySerializer, json.encodeToString(navKeySerializer, LoginRoute(signup = true))),
        )
    }
}
