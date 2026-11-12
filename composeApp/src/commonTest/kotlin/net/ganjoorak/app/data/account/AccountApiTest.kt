package net.ganjoorak.app.data.account

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
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


    @Test
    fun followingQuotesReturnsPeopleAndSendsCookie() = runTest {
        var seenCookie: String? = null
        val engine = MockEngine { request ->
            seenCookie = request.headers[HttpHeaders.Cookie]
            assertTrue(request.url.encodedPath.endsWith("/api/account/following/quotes"))
            respond(
                """{"people":[{"user":{"username":"sara","displayName":"سارا","avatar":"cypress","accent":"#c45c26"},"quotes":[{"id":7,"key":"ganjoor:hafez:1","href":"/poem/1","poetName":"حافظ","poemTitle":"غزل ۱","from":0,"to":1,"lines":["الا یا ایها الساقی","ادر کأسا و ناولها"],"createdAt":1700000000000}],"latestAt":1700000000000}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = AccountApi(HttpClient(engine), token = { "tok123" }, onSessionCookie = {})
        val people = api.followingQuotes()
        assertEquals("sara", people.single().user.username)
        assertEquals("سارا", people.single().user.displayName)
        assertEquals(7, people.single().quotes.single().id)
        assertEquals("ganjoorak_session=tok123", seenCookie)
    }

    @Test
    fun createQuotePostsRangeAndDeleteAcceptsNoContent() = runTest {
        val engine = MockEngine { request ->
            when {
                request.method.value == "POST" && request.url.encodedPath.endsWith("/api/account/quotes") -> respond(
                    """{"quote":{"id":11,"key":"ganjoor:hafez:1","from":0,"to":1,"lines":["الف","ب"],"createdAt":1}}""",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
                request.method.value == "DELETE" && request.url.encodedPath.endsWith("/api/account/quotes/11") ->
                    respond("", HttpStatusCode.NoContent)
                else -> respond("""{"error":"unexpected"}""", HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val api = AccountApi(HttpClient(engine), token = { "tok" }, onSessionCookie = {})
        val quote = api.createQuote("ganjoor:hafez:1", 0, 1)
        assertEquals(11, quote.id)
        assertEquals(listOf("الف", "ب"), quote.lines)
        api.deleteQuote(11)
    }

    @Test
    fun createQuoteSurfacesPersianDailyLimit() = runTest {
        val api = api(
            status = HttpStatusCode.TooManyRequests,
            body = """{"error":"سقف ۲۰ نقل در روز پر شده است؛ فردا دوباره امتحان کنید."}""",
        )
        val error = assertFailsWith<AccountException> { api.createQuote("ganjoor:hafez:1", 0, 1) }
        assertEquals("سقف ۲۰ نقل در روز پر شده است؛ فردا دوباره امتحان کنید.", error.message)
        assertEquals(429, error.status)
    }

    @Test
    fun saveAccountDataPutsMergeFlagAndReturnsSanitizedUnion() = runTest {
        var method: String? = null
        var path: String? = null
        var cookie: String? = null
        var body: String? = null
        val engine = MockEngine { request ->
            method = request.method.value
            path = request.url.encodedPath
            cookie = request.headers[HttpHeaders.Cookie]
            body = (request.body as TextContent).text
            respond(
                """{"data":{"favorites":[{"key":"custom:farrokhzad:12","id":12,"source":"custom","poetSlug":"farrokhzad","poetName":"فروغ","title":"تولدی دیگر","excerpt":"","href":"/farrokhzad/12","savedAt":8}],"history":[],"lovedCollections":[{"key":"ganjoor:hafez:cat:24","source":"ganjoor","categoryId":24,"poetSlug":"hafez","poetName":"حافظ","title":"غزلیات","href":"/hafez/cat/24","savedAt":5}],"preferredPoetKeys":["ganjoor:hafez","custom:farrokhzad"],"interestKeys":["ghazal"]}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = AccountApi(HttpClient(engine), token = { "tok123" }, onSessionCookie = {})
        val payload = AccountDataDto(
            favorites = listOf(
                LibraryEntryDto("custom:farrokhzad:12", 12, source = "custom", poetSlug = "farrokhzad", href = "/farrokhzad/12", savedAt = 8),
            ),
            preferredPoetKeys = listOf("custom:farrokhzad"),
        )
        val saved = api.saveAccountData(payload, merge = true)
        assertEquals("PUT", method)
        assertTrue(path!!.endsWith("/api/account/data"))
        assertEquals("ganjoorak_session=tok123", cookie)
        assertTrue(body!!.contains("\"merge\":true"))
        assertTrue(body!!.contains("custom:farrokhzad:12"))
        assertEquals("custom:farrokhzad:12", saved.favorites.single().key)
        assertEquals("غزلیات", saved.lovedCollections.single().title)
        assertEquals(listOf("ganjoor:hafez", "custom:farrokhzad"), saved.preferredPoetKeys)
        assertEquals(listOf("ghazal"), saved.interestKeys)
    }


    @Test
    fun followUserPutsAndUnfollowDeletes() = runTest {
        val methods = mutableListOf<String>()
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            methods += request.method.value
            paths += request.url.encodedPath
            val following = if (request.method.value == "PUT") "true" else "false"
            respond(
                """{"following":$following}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = AccountApi(HttpClient(engine), token = { "tok" }, onSessionCookie = {})
        assertEquals(true, api.followUser("sara"))
        assertEquals(false, api.unfollowUser("sara"))
        assertEquals(false, api.followState("sara"))
        assertEquals(listOf("PUT", "DELETE", "GET"), methods)
        assertTrue(paths.all { it.endsWith("/api/account/follow/sara") })
    }

    @Test
    fun followListReturnsUsers() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/api/account/following"))
            respond(
                """{"users":[{"username":"sara","displayName":"سارا","avatar":"cypress","followedAt":1700000000000}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = AccountApi(HttpClient(engine), token = { "tok" }, onSessionCookie = {})
        val users = api.followList()
        assertEquals("sara", users.single().username)
        assertEquals("سارا", users.single().displayName)
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
