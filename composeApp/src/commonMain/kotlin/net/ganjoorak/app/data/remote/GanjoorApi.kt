package net.ganjoorak.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ganjoorak.app.APP_VERSION

const val GANJOOR_API_BASE_URL = "https://api.ganjoor.net"
const val SEMANTIC_API_BASE_URL = "https://ganjgah.ir"
private const val RANDOM_ATTEMPTS = 3

val GanjoorJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}

/** The engine is passed in rather than discovered via ServiceLoader, which R8 can strip from release builds. */
fun createHttpClient(engine: HttpClientEngineFactory<*>): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
    defaultRequest {
        headers.append(HttpHeaders.Accept, "application/json")
        // Without this OkHttp sends "okhttp/…", which some servers treat as a bot.
        headers.append(HttpHeaders.UserAgent, "Ganjoorak/$APP_VERSION")
    }
}

/** Thrown when a response body isn't the JSON we expected; the message carries a prefix of the body. */
class ApiFormatException(message: String, cause: Throwable) : Exception(message, cause)

/**
 * Decodes the body as JSON ourselves. ContentNegotiation's converter lookup failed on Android
 * (NoTransformationFoundException even for application/json), so it isn't used at all.
 */
suspend inline fun <reified T> HttpResponse.json(): T {
    val text = bodyAsText()
    return try {
        GanjoorJson.decodeFromString<T>(text)
    } catch (e: IllegalArgumentException) { // includes SerializationException
        throw ApiFormatException("${request.url} → $status ${headers[HttpHeaders.ContentType]}: ${text.take(160)}", e)
    }
}

data class SearchResponse(val poems: List<PoemDto>, val paging: PagingHeaderDto?)

class GanjoorApi(
    private val client: HttpClient,
    private val baseUrl: String = GANJOOR_API_BASE_URL,
    private val semanticBaseUrl: String = SEMANTIC_API_BASE_URL,
) {
    suspend fun poets(): List<PoetDto> = client.get("$baseUrl/api/ganjoor/poets").json()

    suspend fun centuries(): List<CenturyDto> = client.get("$baseUrl/api/ganjoor/centuries").json()

    suspend fun poet(id: Int): PoetCompleteDto = client.get("$baseUrl/api/ganjoor/poet/$id") {
        parameter("catPoems", true)
    }.json()

    suspend fun category(id: Int): PoetCompleteDto = client.get("$baseUrl/api/ganjoor/cat/$id") {
        parameter("poems", true)
        parameter("mainSections", false)
    }.json()

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
    }.json()

    suspend fun randomPoem(poetId: Int? = null): PoemDto {
        suspend fun fetch() = client.get("$baseUrl/api/ganjoor/poem/random") {
            if (poetId != null && poetId > 0) parameter("poetId", poetId)
        }
        // The endpoint now and then answers 204 with no body; ask again before giving up.
        repeat(RANDOM_ATTEMPTS - 1) {
            val response = fetch()
            if (response.status != HttpStatusCode.NoContent) return response.json()
        }
        return fetch().json()
    }

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
        return SearchResponse(response.json(), paging)
    }

    suspend fun semanticSearch(query: String, topK: Int): SemanticSearchResponseDto =
        client.post("$semanticBaseUrl/api/ganjoor/search/semantic") {
            setBody(TextContent(GanjoorJson.encodeToString(SemanticSearchRequest(query = query, topK = topK)), ContentType.Application.Json))
        }.json()

    suspend fun recitationTimings(recitationId: Int): List<VerseSyncDto> =
        client.get("$baseUrl/api/audio/verses/$recitationId").json()
}
