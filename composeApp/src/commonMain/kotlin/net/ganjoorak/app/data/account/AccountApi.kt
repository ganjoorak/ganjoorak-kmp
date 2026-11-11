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
private data class LoginResponse(val user: AccountUser)

@Serializable
private data class SignupResponse(val user: AccountUser, val recoveryCode: String = "")

@Serializable
private data class MeResponse(val user: AccountUser)

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

    private suspend fun get(path: String): HttpResponse = send(HttpMethod.Get, path, json = null)

    private suspend inline fun <reified B> post(path: String, body: B): HttpResponse =
        send(HttpMethod.Post, path, json = GanjoorJson.encodeToString(body))

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
        val message = try {
            GanjoorJson.decodeFromString<JsonObject>(text)["error"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
        throw AccountException(message?.takeIf { it.isNotBlank() } ?: AuthValidation.NetworkError, status.value)
    }
}

data class SignupResult(val user: AccountUser, val recoveryCode: String)
