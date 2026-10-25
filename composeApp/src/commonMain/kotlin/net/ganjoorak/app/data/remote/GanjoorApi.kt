package net.ganjoorak.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val GANJOOR_API_BASE_URL = "https://api.ganjoor.net"
const val SEMANTIC_API_BASE_URL = "https://ganjgah.ir"

val GanjoorJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}

fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    install(ContentNegotiation) { json(GanjoorJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
    defaultRequest {
        headers.append("Accept", "application/json")
    }
}

data class SearchResponse(val poems: List<PoemDto>, val paging: PagingHeaderDto?)

class GanjoorApi(
    private val client: HttpClient,
    private val baseUrl: String = GANJOOR_API_BASE_URL,
    private val semanticBaseUrl: String = SEMANTIC_API_BASE_URL,
) {
    suspend fun poets(): List<PoetDto> = client.get("$baseUrl/api/ganjoor/poets").body()

    suspend fun centuries(): List<CenturyDto> = client.get("$baseUrl/api/ganjoor/centuries").body()

    suspend fun poet(id: Int): PoetCompleteDto = client.get("$baseUrl/api/ganjoor/poet/$id") {
        parameter("catPoems", true)
    }.body()

    suspend fun category(id: Int): PoetCompleteDto = client.get("$baseUrl/api/ganjoor/cat/$id") {
        parameter("poems", true)
        parameter("mainSections", false)
    }.body()

    suspend fun poem(id: Int): PoemDto = client.get("$baseUrl/api/ganjoor/poem/$id") {
        parameter("catInfo", true)
        parameter("catPoems", false)
        parameter("rhymes", false)
        parameter("recitations", true)
        parameter("images", false)
        parameter("songs", false)
        parameter("comments", false)
        parameter("verseDetails", true)
        parameter("navigation", true)
        parameter("relatedpoems", false)
    }.body()

    suspend fun randomPoem(poetId: Int? = null): PoemDto = client.get("$baseUrl/api/ganjoor/poem/random") {
        if (poetId != null && poetId > 0) parameter("poetId", poetId)
    }.body()

    suspend fun search(term: String, page: Int, pageSize: Int, poetId: Int? = null): SearchResponse {
        val response = client.get("$baseUrl/api/ganjoor/poems/search") {
            parameter("term", term)
            parameter("PageNumber", page)
            parameter("PageSize", pageSize)
            if (poetId != null && poetId > 0) parameter("poetId", poetId)
        }
        val paging = response.headers["paging-headers"]?.let {
            runCatching { GanjoorJson.decodeFromString<PagingHeaderDto>(it) }.getOrNull()
        }
        return SearchResponse(response.body(), paging)
    }

    suspend fun semanticSearch(query: String, topK: Int): SemanticSearchResponseDto =
        client.post("$semanticBaseUrl/api/ganjoor/search/semantic") {
            contentType(ContentType.Application.Json)
            setBody(SemanticSearchRequest(query = query, topK = topK))
        }.body()

    suspend fun recitationTimings(recitationId: Int): List<VerseSyncDto> =
        client.get("$baseUrl/api/audio/verses/$recitationId").body()
}
