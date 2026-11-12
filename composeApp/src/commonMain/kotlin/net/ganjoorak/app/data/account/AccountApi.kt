package net.ganjoorak.app.data.account

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.setCookie
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.ganjoorak.app.APP_VERSION
import net.ganjoorak.app.data.remote.GanjoorJson

/**
 * The same Go accounts service the website talks to through the website's /api/account/ routes
 * (rewritten on ganjoorak.ir). Session is a `ganjoorak_session` cookie, 30 days,
 * HttpOnly+Secure+SameSite=Lax on the wire; the app stores the token and sends it back.
 */
const val ACCOUNT_API_BASE_URL = "https://ganjoorak.ir"
const val SESSION_COOKIE = "ganjoorak_session"

fun createAccountHttpClient(engine: HttpClientEngineFactory<*>): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
}

@Serializable
private data class LoginBody(val username: String, val password: String)

@Serializable
private data class SignupBody(val username: String, val password: String, val displayName: String = "")

@Serializable
private data class ResetBody(val username: String, val recoveryCode: String, val newPassword: String)

@Serializable
private data class CreateQuoteBody(val key: String, val from: Int, val to: Int)

@Serializable
private data class CreateQuoteResponse(val quote: Quote)

@Serializable
private data class FollowingQuotesResponse(val people: List<StoryPerson> = emptyList())

@Serializable
private data class FollowStateResponse(val following: Boolean = false)

@Serializable
private data class FollowListResponse(val users: List<FollowedUser> = emptyList())

@Serializable
data class FollowedUser(
    val username: String,
    val displayName: String = "",
    val avatar: String = "",
    val followedAt: Long = 0,
)

@Serializable
private data class UserQuotesResponse(val quotes: List<Quote> = emptyList(), val next: Long? = null)

@Serializable
private data class LoginResponse(val user: AccountUser)

@Serializable
private data class SignupResponse(val user: AccountUser, val recoveryCode: String = "")

@Serializable
private data class MeResponse(val user: AccountUser)

@Serializable
private data class SaveDataBody(val data: AccountDataDto, val merge: Boolean)

@Serializable
private data class SaveDataResponse(val data: AccountDataDto)

class AccountApi(
    private val client: HttpClient,
    private val token: () -> String?,
    private val onSessionCookie: (String?) -> Unit,
    private val baseUrl: String = ACCOUNT_API_BASE_URL,
) {
    suspend fun logIn(username: String, password: String): AccountUser =
        post("login", LoginBody(username, password)).parse<LoginResponse>().user

    /** Empty [displayName] falls back to the username on the server, as on the website. */
    suspend fun signUp(username: String, password: String, displayName: String = ""): SignupResult {
        val body = post("signup", SignupBody(username, password, displayName)).parse<SignupResponse>()
        return SignupResult(body.user, body.recoveryCode)
    }

    suspend fun resetPassword(username: String, recoveryCode: String, newPassword: String): SignupResult {
        val body = post("reset", ResetBody(username, recoveryCode, newPassword)).parse<SignupResponse>()
        return SignupResult(body.user, body.recoveryCode)
    }

    suspend fun logOut() {
        try {
            send(HttpMethod.Post, "logout", json = "{}")
        } finally {
            onSessionCookie(null)
        }
    }

    /** 401 when signed out. */
    suspend fun me(): AccountUser = get("me").parse<MeResponse>().user

    /**
     * Replace or union the signed-in account library. [merge] true unions the
     * payload with what is already stored, so a first login never drops data.
     * [merge] false replaces the stored snapshot; callers must include
     * lovedCollections and interestKeys they want to keep.
     */
    suspend fun saveAccountData(data: AccountDataDto, merge: Boolean): AccountDataDto =
        put("data", SaveDataBody(data, merge)).parse<SaveDataResponse>().data

    /** Followed people who shared a fragment in the last 7 days. 401 when signed out. */
    suspend fun followingQuotes(): List<StoryPerson> =
        get("following/quotes").parse<FollowingQuotesResponse>().people

    /** 401 when signed out. */
    suspend fun followState(username: String): Boolean =
        get("follow/$username").parse<FollowStateResponse>().following

    suspend fun followUser(username: String): Boolean =
        send(HttpMethod.Put, "follow/$username", json = null).parse<FollowStateResponse>().following

    suspend fun unfollowUser(username: String): Boolean =
        send(HttpMethod.Delete, "follow/$username", json = null).parse<FollowStateResponse>().following

    /** The signed-in user's own lists; other people's are not public. */
    suspend fun followList(followers: Boolean = false): List<FollowedUser> =
        get(if (followers) "followers" else "following").parse<FollowListResponse>().users

    suspend fun createQuote(key: String, from: Int, to: Int): Quote =
        post("quotes", CreateQuoteBody(key, from, to)).parse<CreateQuoteResponse>().quote

    suspend fun deleteQuote(id: Long) {
        val response = send(HttpMethod.Delete, "quotes/$id", json = null)
        if (response.status.value in 200..299) return
        throw errorFrom(response)
    }

    suspend fun userQuotes(username: String, before: Long? = null): QuotesPage {
        val path = buildString {
            append("users/")
            // Usernames are [a-z0-9_]{3,32}; no encoding is required.
            append(username)
            append("/quotes")
            if (before != null) append("?before=$before")
        }
        val body = get(path).parse<UserQuotesResponse>()
        return QuotesPage(body.quotes, body.next)
    }

    private suspend fun get(path: String): HttpResponse = send(HttpMethod.Get, path, json = null)

    private suspend inline fun <reified B> post(path: String, body: B): HttpResponse =
        send(HttpMethod.Post, path, json = GanjoorJson.encodeToString(body))

    private suspend inline fun <reified B> put(path: String, body: B): HttpResponse =
        send(HttpMethod.Put, path, json = GanjoorJson.encodeToString(body))

    private suspend fun send(method: HttpMethod, path: String, json: String?): HttpResponse {
        val response = try {
            client.request("$baseUrl/api/account/$path") {
                this.method = method
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Ganjoorak/$APP_VERSION")
                token()?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Cookie, "$SESSION_COOKIE=$it") }
                if (json != null) {
                    setBody(TextContent(json, ContentType.Application.Json))
                }
            }
        } catch (e: AccountException) {
            throw e
        } catch (_: Exception) {
            throw AccountException(AuthValidation.NetworkError, 0)
        }
        captureSession(response)
        return response
    }

    private fun captureSession(response: HttpResponse) {
        val cookie = response.setCookie().firstOrNull { it.name == SESSION_COOKIE }
        if (cookie != null) {
            onSessionCookie(cookie.value.ifBlank { null })
        }
    }

    private suspend inline fun <reified T> HttpResponse.parse(): T {
        val text = bodyAsText()
        if (status.value in 200..299) {
            return try {
                GanjoorJson.decodeFromString<T>(text)
            } catch (_: Exception) {
                throw AccountException(AuthValidation.NetworkError, status.value)
            }
        }
        throw errorFrom(text, status.value)
    }

    private suspend fun errorFrom(response: HttpResponse): AccountException =
        errorFrom(response.bodyAsText(), response.status.value)

    private fun errorFrom(text: String, status: Int): AccountException {
        val message = try {
            GanjoorJson.decodeFromString<JsonObject>(text)["error"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
        return AccountException(message?.takeIf { it.isNotBlank() } ?: AuthValidation.NetworkError, status)
    }
}

data class SignupResult(val user: AccountUser, val recoveryCode: String)
