package net.ganjoorak.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.EcholaliaSource
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.LocalSource
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.remote.EcholaliaApi
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.remote.GanjoorApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiTest {
    private fun repository(
        handler: (path: String, query: String) -> Pair<HttpStatusCode, String>,
        headers: Map<String, String> = emptyMap(),
        local: Map<String, String> = emptyMap(),
    ): PoemRepository {
        val client = HttpClient(MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath, request.url.encodedQuery)
            respond(
                content = body,
                status = status,
                headers = headersOf(
                    *(headers + (HttpHeaders.ContentType to "text/plain")).map { it.key to listOf(it.value) }.toTypedArray(),
                ),
            )
        }) {
            expectSuccess = true
        }
        return PoemRepository(
            api = GanjoorApi(client),
            echolalia = EcholaliaSource(EcholaliaApi(client)),
            local = LocalSource { slug -> local[slug] ?: error("no $slug") },
        )
    }

    @Test
    fun searchReadsPagingHeader() = runTest {
        val repo = repository(
            handler = { path, query ->
                if (path != "/api/ganjoor/poems/search") return@repository HttpStatusCode.NotFound to "{}"
                assertTrue("term=" in query && "PageNumber=2" in query)
                HttpStatusCode.OK to """[{"id": 1, "title": "a", "fullTitle": "حافظ » a", "plainText": "x\ny"}]"""
            },
            headers = mapOf("paging-headers" to """{"totalCount": 41, "pageSize": 20, "currentPage": 2, "totalPages": 3, "hasNextPage": true}"""),
        )
        val page = repo.search("عشق", page = 2)
        assertEquals(1, page.hits.size)
        assertTrue(page.hasNext)
        assertEquals(41, page.total)
    }

    @Test
    fun randomPoemFetchesFullPoemWhenIncomplete() = runTest {
        val calls = mutableListOf<String>()
        val repo = repository(handler = { path, _ ->
            calls += path
            when (path) {
                "/api/ganjoor/poem/random" -> HttpStatusCode.OK to """{"id": 77, "title": "r"}"""
                "/api/ganjoor/poem/77" -> HttpStatusCode.OK to """{"id": 77, "title": "r", "fullTitle": "مولوی » r",
                    "category": {"poet": {"id": 5, "name": "مولوی", "fullUrl": "/moulavi"}},
                    "verses": [{"vOrder": 1, "versePosition": 0, "text": "بشنو"}, {"vOrder": 2, "versePosition": 1, "text": "از نی"}]}"""
                else -> HttpStatusCode.NotFound to "{}"
            }
        })
        val poem = repo.randomPoem()
        assertEquals(listOf("/api/ganjoor/poem/random", "/api/ganjoor/poem/77"), calls)
        assertEquals("مولوی", poem.poetName)
        assertEquals(1, poem.blocks.size)
    }

    @Test
    fun randomPoemRetriesEmptyAnswers() = runTest {
        var randomCalls = 0
        val repo = repository(handler = { path, _ ->
            when (path) {
                "/api/ganjoor/poem/random" -> if (++randomCalls < 3) HttpStatusCode.NoContent to "" else HttpStatusCode.OK to
                    """{"id": 8, "title": "r", "fullTitle": "حافظ » r", "category": {"poet": {"id": 2, "name": "حافظ"}},
                    "verses": [{"vOrder": 1, "versePosition": 0, "text": "الا"}, {"vOrder": 2, "versePosition": 1, "text": "یا"}]}"""
                else -> HttpStatusCode.NotFound to "{}"
            }
        })
        assertEquals("حافظ", repo.randomPoem().poetName)
        assertEquals(3, randomCalls)
    }

    @Test
    fun notFoundBecomesFriendlyError() = runTest {
        val repo = repository(handler = { _, _ -> HttpStatusCode.NotFound to "{}" })
        val error = assertFailsWith<GanjoorException> { repo.poem(PoemRef(PoetSource.Ganjoor, 1)) }
        assertEquals("مورد درخواستی پیدا نشد", error.message)
    }

    @Test
    fun firstSearchPageMergesBundledAndEcholaliaHits() = runTest {
        val repo = repository(
            handler = { path, _ ->
                when (path) {
                    "/api/ganjoor/poems/search" -> HttpStatusCode.OK to """[{"id": 9, "title": "g", "fullTitle": "حافظ » g", "plainText": "باران"}]"""
                    "/wp-json/wp/v2/categories" -> HttpStatusCode.OK to """[{"id": 1, "slug": "sher", "count": 0, "parent": 0}, {"id": 7, "name": "نرودا", "slug": "neruda", "count": 3, "parent": 1}]"""
                    "/wp-json/wp/v2/posts" -> HttpStatusCode.OK to """[{"id": 55, "title": {"rendered": "باران"}, "content": {"rendered": "<p>باران<br>می‌بارد</p>"}, "categories": [7]}]"""
                    else -> HttpStatusCode.NotFound to "{}"
                }
            },
            local = mapOf(
                "rahmani" to """{"poet": "نصرت رحمانی", "poemSlug": "rahmani", "poems": [{"id": 3, "title": "باران", "text": "باران\nآمد"}]}""",
                "farrokhzad" to """{"poet": "فروغ فرخزاد", "poems": []}""",
            ),
        )
        val page = repo.search("باران", page = 1)
        assertEquals(
            listOf(PoemRef(PoetSource.Local, 3, "rahmani"), PoemRef(PoetSource.Echolalia, 55, "neruda"), PoemRef(PoetSource.Ganjoor, 9)),
            page.hits.map { it.ref },
        )
        assertEquals("نرودا", page.hits[1].poetName)
    }

    @Test
    fun loadsEcholaliaPoemAsFreeVerse() = runTest {
        val repo = repository(handler = { path, _ ->
            when (path) {
                "/wp-json/wp/v2/posts/55" -> HttpStatusCode.OK to """{"id": 55, "link": "https://echolalia.ir/p/55",
                    "title": {"rendered": "شعر &#8220;آب&#8221;"}, "categories": [7],
                    "content": {"rendered": "<p>یک<br />دو</p><p>سه</p><div class=\"sing-block\">audio</div>"}}"""
                "/wp-json/wp/v2/categories/7" -> HttpStatusCode.OK to """{"id": 7, "name": "پابلو نرودا", "slug": "pablo-neruda", "count": 3, "parent": 2}"""
                else -> HttpStatusCode.NotFound to "{}"
            }
        })
        val poem = repo.poem(PoemRef(PoetSource.Echolalia, 55))
        assertEquals("شعر \u201cآب\u201d", poem.title)
        assertEquals("پابلو نرودا", poem.poetName)
        assertEquals("echolalia:pablo-neruda", poem.poetKey)
        assertEquals(listOf("یک", "دو", "سه"), poem.verses.map { it.text })
        assertEquals(4, poem.blocks.size)
        assertEquals("https://ganjoorak.ir/pablo-neruda/55", poem.shareUrl)
    }
}
