package net.ganjoorak.app.data.account

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountApiTest {
    @Test
    fun loginStoresSessionCookieAndUser() = runTest {
        var stored: String? = null
        val api = api(
            token = { stored },
            onCookie = { stored = it },
            status = HttpStatusCode.OK,
            body = """{"user":{"username":"hadi","displayName":"هادی"}}""",
            setCookie = "ganjoorak_session=tok123; Path=/; HttpOnly; Secure; SameSite=Lax",
        )
        val user = api.logIn("hadi", "longenough")
        assertEquals("hadi", user.username)
        assertEquals("هادی", user.displayName)
        assertEquals("tok123", stored)
    }

    @Test
    fun signupReturnsRecoveryCode() = runTest {
        val api = api(
            status = HttpStatusCode.Created,
            body = """{"user":{"username":"newuser","displayName":"newuser"},"recoveryCode":"ABCD-EFGH-IJKM-NPQR"}""",
            setCookie = "ganjoorak_session=newtok; Path=/",
        )
        val result = api.signUp("newuser", "longenough")
        assertEquals("newuser", result.user.username)
        assertEquals("ABCD-EFGH-IJKM-NPQR", result.recoveryCode)
    }

    @Test
    fun loginSurfacesPersianErrorFromJson() = runTest {
        val api = api(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"نام کاربری یا گذرواژه درست نیست."}""",
        )
        val error = assertFailsWith<AccountException> { api.logIn("hadi", "nope-nope") }
        assertEquals("نام کاربری یا گذرواژه درست نیست.", error.message)
        assertEquals(401, error.status)
    }

    @Test
    fun htmlErrorFallsBackToNetworkCopy() = runTest {
        val api = api(status = HttpStatusCode.InternalServerError, body = "<html>boom</html>")
        val error = assertFailsWith<AccountException> { api.me() }
        assertEquals(AuthValidation.NetworkError, error.message)
        assertTrue(error.status >= 500)
    }

    @Test
    fun meSendsStoredSessionCookie() = runTest {
        var seenCookie: String? = null
        val engine = MockEngine { request ->
            seenCookie = request.headers[HttpHeaders.Cookie]
            respond(
                """{"user":{"username":"hadi","displayName":"هادی"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = AccountApi(HttpClient(engine), token = { "tok123" }, onSessionCookie = {})
        assertEquals("hadi", api.me().username)
        assertEquals("ganjoorak_session=tok123", seenCookie)
    }

    @Test
    fun logoutClearsTheCookie() = runTest {
        var stored: String? = "tok123"
        val engine = MockEngine {
            respond("{}", HttpStatusCode.NoContent)
        }
        val api = AccountApi(HttpClient(engine), token = { stored }, onSessionCookie = { stored = it })
        api.logOut()
        assertNull(stored)
    }

    private fun api(
        token: () -> String? = { null },
        onCookie: (String?) -> Unit = {},
        status: HttpStatusCode,
        body: String,
        setCookie: String? = null,
    ): AccountApi {
        val engine = MockEngine {
            val headers = mutableListOf(HttpHeaders.ContentType to listOf("application/json"))
            if (setCookie != null) headers += HttpHeaders.SetCookie to listOf(setCookie)
            respond(body, status, headersOf(*headers.toTypedArray()))
        }
        return AccountApi(HttpClient(engine), token, onCookie)
    }
}
