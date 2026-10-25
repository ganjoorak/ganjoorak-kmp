package net.ganjoorak.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.remote.GanjoorApi
import net.ganjoorak.app.data.remote.GanjoorJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiTest {
    private fun repository(handler: (path: String, query: String) -> Pair<HttpStatusCode, String>, headers: Map<String, String> = emptyMap()) =
        PoemRepository(
            GanjoorApi(
                HttpClient(MockEngine { request ->
                    val (status, body) = handler(request.url.encodedPath, request.url.encodedQuery)
                    respond(
                        content = body,
                        status = status,
                        headers = headersOf(
                            *(headers + (HttpHeaders.ContentType to "application/json")).map { it.key to listOf(it.value) }.toTypedArray(),
                        ),
                    )
                }) {
                    expectSuccess = true
                    install(ContentNegotiation) { json(GanjoorJson) }
                },
            ),
        )

    @Test
    fun searchReadsPagingHeader() = runTest {
        val repo = repository(
            handler = { path, query ->
                assertEquals("/api/ganjoor/poems/search", path)
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
    fun notFoundBecomesFriendlyError() = runTest {
        val repo = repository(handler = { _, _ -> HttpStatusCode.NotFound to "{}" })
        val error = assertFailsWith<GanjoorException> { repo.poem(1) }
        assertEquals("مورد درخواستی پیدا نشد", error.message)
    }
}
