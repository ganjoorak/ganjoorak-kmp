package net.ganjoorak.app.data.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthValidationTest {
    @Test
    fun acceptsWebsiteUsernameRules() {
        assertTrue(AuthValidation.isValidUsername("hadi"))
        assertTrue(AuthValidation.isValidUsername("Hadi_01")) // folded to lowercase
        assertEquals("hadi_01", AuthValidation.normalizeUsername("  Hadi_01 "))
        assertFalse(AuthValidation.isValidUsername("ha"))
        assertFalse(AuthValidation.isValidUsername("هادی"))
        assertFalse(AuthValidation.isValidUsername("ha-di"))
        assertFalse(AuthValidation.isValidUsername("a".repeat(33)))
    }

    @Test
    fun countsPasswordRunesLikeTheBackend() {
        assertFalse(AuthValidation.isValidPassword("short"))
        assertTrue(AuthValidation.isValidPassword("longenough"))
        // 8 emoji = 8 runes (each is a surrogate pair in UTF-16)
        assertTrue(AuthValidation.isValidPassword("🌸".repeat(8)))
        assertFalse(AuthValidation.isValidPassword("🌸".repeat(7)))
        assertNull(AuthValidation.signupError("hadi", "longenough"))
        assertEquals(AuthValidation.BadUsername, AuthValidation.signupError("ha", "longenough"))
        assertEquals(AuthValidation.BadPassword, AuthValidation.signupError("hadi", "short"))
    }
}
