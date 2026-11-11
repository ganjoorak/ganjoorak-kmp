package net.ganjoorak.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.account.AccountException
import net.ganjoorak.app.data.account.AccountRepository
import net.ganjoorak.app.data.account.AuthValidation

enum class AuthMode { Login, Signup }

enum class AuthView { Form, Reset, RecoveryCode }

data class AuthUiState(
    val mode: AuthMode = AuthMode.Login,
    val view: AuthView = AuthView.Form,
    val username: String = "",
    val password: String = "",
    val recoveryInput: String = "",
    val issuedCode: String = "",
    val error: String = "",
    val isSubmitting: Boolean = false,
) {
    val title: String
        get() = when {
            view == AuthView.RecoveryCode -> "کد بازیابی شما"
            view == AuthView.Reset -> "بازیابی گذرواژه"
            mode == AuthMode.Signup -> "ساخت حساب"
            else -> "ورود به حساب"
        }

    val description: String?
        get() = if (view == AuthView.Reset) {
            "نام کاربری و کد بازیابی‌ای را که هنگام ساخت حساب گرفتید وارد کنید و گذرواژهٔ تازه بگذارید."
        } else {
            null
        }
}

class AuthViewModel(
    private val accounts: AccountRepository,
    startSignup: Boolean,
    private val onSignedIn: () -> Unit,
) : ViewModel() {
    private val state = MutableStateFlow(AuthUiState(mode = if (startSignup) AuthMode.Signup else AuthMode.Login))
    val ui: StateFlow<AuthUiState> = state.asStateFlow()

    fun setUsername(value: String) = state.update { it.copy(username = value, error = "") }
    fun setPassword(value: String) = state.update { it.copy(password = value, error = "") }
    fun setRecoveryInput(value: String) = state.update { it.copy(recoveryInput = value, error = "") }

    fun show(mode: AuthMode) = state.update {
        it.copy(mode = mode, view = AuthView.Form, error = "", issuedCode = "")
    }

    fun showReset() = state.update { it.copy(view = AuthView.Reset, error = "", password = "") }

    fun backToLogin() = state.update {
        it.copy(view = AuthView.Form, mode = AuthMode.Login, error = "", password = "")
    }

    fun submit() {
        val current = state.value
        if (current.isSubmitting) return
        val error = when (current.view) {
            AuthView.Reset -> AuthValidation.resetError(current.username, current.recoveryInput, current.password)
            AuthView.Form -> if (current.mode == AuthMode.Signup) {
                AuthValidation.signupError(current.username, current.password)
            } else {
                if (current.username.isBlank() || current.password.isBlank()) "نام کاربری یا گذرواژه درست نیست." else null
            }
            AuthView.RecoveryCode -> return
        }
        if (error != null) {
            state.update { it.copy(error = error) }
            return
        }
        state.update { it.copy(isSubmitting = true, error = "") }
        viewModelScope.launch {
            try {
                when (current.view) {
                    AuthView.Reset -> {
                        val code = accounts.resetPassword(current.username, current.recoveryInput, current.password)
                        state.update { it.copy(isSubmitting = false, view = AuthView.RecoveryCode, issuedCode = code) }
                    }
                    AuthView.Form -> if (current.mode == AuthMode.Signup) {
                        val code = accounts.signUp(current.username, current.password)
                        state.update { it.copy(isSubmitting = false, view = AuthView.RecoveryCode, issuedCode = code) }
                    } else {
                        accounts.logIn(current.username, current.password)
                        onSignedIn()
                    }
                    AuthView.RecoveryCode -> Unit
                }
            } catch (e: AccountException) {
                state.update { it.copy(isSubmitting = false, error = e.message ?: AuthValidation.GenericError) }
            } catch (_: Exception) {
                state.update { it.copy(isSubmitting = false, error = AuthValidation.GenericError) }
            }
        }
    }

    fun finishRecovery() = onSignedIn()
}
