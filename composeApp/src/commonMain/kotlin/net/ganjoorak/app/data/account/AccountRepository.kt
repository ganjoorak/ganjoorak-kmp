package net.ganjoorak.app.data.account

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.remote.GanjoorJson

data class AccountSession(
    val user: AccountUser? = null,
    val isLoading: Boolean = false,
) {
    val isSignedIn: Boolean get() = user != null
}

/**
 * Session cookie plus the last-known user, persisted so a restart stays signed in.
 * `/me` is asked once at start (and after login) against the same backend as the website.
 */
class AccountRepository(
    private val settings: Settings,
    private val apiFactory: (token: () -> String?, onCookie: (String?) -> Unit) -> AccountApi,
    scope: CoroutineScope,
) {
    private val tokenState = MutableStateFlow(settings.getStringOrNull(TOKEN_KEY)?.takeIf { it.isNotBlank() })
    private val sessionState = MutableStateFlow(
        AccountSession(
            user = settings.getStringOrNull(USER_KEY)?.let { runCatching { GanjoorJson.decodeFromString(AccountUser.serializer(), it) }.getOrNull() },
            isLoading = tokenState.value != null,
        ),
    )
    val session: StateFlow<AccountSession> = sessionState.asStateFlow()

    private val api = apiFactory({ tokenState.value }, this::persistToken)

    init {
        if (tokenState.value != null) {
            scope.launch { refresh() }
        }
    }

    suspend fun logIn(username: String, password: String) {
        val user = api.logIn(AuthValidation.normalizeUsername(username), password)
        persistUser(user)
    }

    suspend fun signUp(username: String, password: String): String {
        val result = api.signUp(AuthValidation.normalizeUsername(username), password, "")
        persistUser(result.user)
        return result.recoveryCode
    }

    suspend fun resetPassword(username: String, recoveryCode: String, newPassword: String): String {
        val result = api.resetPassword(AuthValidation.normalizeUsername(username), recoveryCode, newPassword)
        persistUser(result.user)
        return result.recoveryCode
    }

    suspend fun logOut() {
        runCatching { api.logOut() }
        persistToken(null)
        persistUser(null)
    }

    suspend fun refresh() {
        if (tokenState.value.isNullOrBlank()) {
            persistUser(null)
            sessionState.value = AccountSession()
            return
        }
        sessionState.value = sessionState.value.copy(isLoading = true)
        try {
            persistUser(api.me())
        } catch (e: AccountException) {
            if (e.status == 401) {
                persistToken(null)
                persistUser(null)
            } else {
                sessionState.value = sessionState.value.copy(isLoading = false)
            }
        }
    }

    private fun persistToken(token: String?) {
        tokenState.value = token?.takeIf { it.isNotBlank() }
        if (tokenState.value == null) settings.remove(TOKEN_KEY) else settings.putString(TOKEN_KEY, tokenState.value!!)
        if (tokenState.value == null) persistUser(null)
    }

    private fun persistUser(user: AccountUser?) {
        if (user == null) {
            settings.remove(USER_KEY)
            sessionState.value = AccountSession()
        } else {
            settings.putString(USER_KEY, GanjoorJson.encodeToString(AccountUser.serializer(), user))
            sessionState.value = AccountSession(user = user, isLoading = false)
        }
    }

    private companion object {
        const val TOKEN_KEY = "account_session_token"
        const val USER_KEY = "account_user"
    }
}
