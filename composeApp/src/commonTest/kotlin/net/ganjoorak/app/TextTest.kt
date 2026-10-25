package net.ganjoorak.app

import net.ganjoorak.app.util.hijriLifespan
import net.ganjoorak.app.util.normalizeForSearch
import net.ganjoorak.app.util.stripHtml
import net.ganjoorak.app.util.toPersianDigits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextTest {
    @Test
    fun convertsDigits() {
        assertEquals("۱۲۳۴۵۶۷۸۹۰", "1234567890".toPersianDigits())
        assertEquals("غزل ۱۲", "غزل 12".toPersianDigits())
    }

    @Test
    fun normalizesArabicVariants() {
        assertEquals("علی کرد", "عليٌ  كرد".normalizeForSearch())
        assertEquals("می رود", "می‌رود".normalizeForSearch())
    }

    @Test
    fun stripsHtml() {
        assertEquals("یک\nدو", "<div class=\"b\"><p>یک</p><p>دو&nbsp;</p></div>".stripHtml())
    }

    @Test
    fun formatsLifespan() {
        assertEquals("۷۲۷ – ۷۹۲ ه‍.ق", hijriLifespan(727, 792))
        assertNull(hijriLifespan(null, null))
    }
}
