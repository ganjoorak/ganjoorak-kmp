package net.ganjoorak.app.data.account

/**
 * Client-side checks that match backend/account.go and the website login form:
 * username 3–32 Latin letters, digits or `_` (folded to lowercase);
 * password 8–128 Unicode characters on sign-up and reset.
 */
object AuthValidation {
    const val MinUsernameLen = 3
    const val MaxUsernameLen = 32
    const val MinPasswordLen = 8
    const val MaxPasswordLen = 128
    const val MaxRecoveryCodeLen = 64

    const val BadUsername = "نام کاربری باید ۳ تا ۳۲ نویسهٔ لاتین کوچک، رقم یا _ باشد."
    const val BadPassword = "گذرواژه باید ۸ تا ۱۲۸ نویسه باشد."
    const val NetworkError = "ارتباط با سرور برقرار نشد. دوباره امتحان کنید."
    const val GenericError = "خطایی رخ داد."

    private val usernamePattern = Regex("^[a-z0-9_]{3,32}$")

    fun normalizeUsername(value: String): String = value.trim().lowercase()

    fun isValidUsername(value: String): Boolean = usernamePattern.matches(normalizeUsername(value))

    fun isValidPassword(value: String): Boolean {
        val n = runeCount(value)
        return n in MinPasswordLen..MaxPasswordLen
    }

    /** Unicode scalar count, same as Go's utf8.RuneCountInString. */
    internal fun runeCount(value: String): Int {
        var n = 0
        var i = 0
        while (i < value.length) {
            val c = value[i]
            i += if (c.isHighSurrogate() && i + 1 < value.length && value[i + 1].isLowSurrogate()) 2 else 1
            n++
        }
        return n
    }

    /** Website signup/reset: reject before the request when the field is clearly wrong. */
    fun signupError(username: String, password: String): String? = when {
        !isValidUsername(username) -> BadUsername
        !isValidPassword(password) -> BadPassword
        else -> null
    }

    fun resetError(username: String, recoveryCode: String, newPassword: String): String? = when {
        username.isBlank() -> BadUsername
        recoveryCode.isBlank() -> "نام کاربری یا کد بازیابی درست نیست."
        !isValidPassword(newPassword) -> BadPassword
        else -> null
    }
}
