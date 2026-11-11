package net.ganjoorak.app.data.account

import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PoemKeysTest {
    @Test
    fun libraryKeyUsesCustomForLocalPoems() {
        assertEquals("ganjoor:hafez:123", poemLibraryKey(PoemRef(PoetSource.Ganjoor, 123, "hafez")))
        assertEquals("echolalia:nima:9", poemLibraryKey(PoemRef(PoetSource.Echolalia, 9, "nima")))
        assertEquals("custom:farrokhzad:4", poemLibraryKey(PoemRef(PoetSource.Local, 4, "farrokhzad")))
    }

    @Test
    fun parseLibraryKeyAndHref() {
        assertEquals(PoemRef(PoetSource.Ganjoor, 123, "hafez"), parsePoemLibraryKey("ganjoor:hafez:123"))
        assertEquals(PoemRef(PoetSource.Local, 4, "farrokhzad"), parsePoemLibraryKey("custom:farrokhzad:4"))
        assertNull(parsePoemLibraryKey("local:farrokhzad:4"))
        assertEquals(PoemRef(PoetSource.Ganjoor, 42), parseQuoteHref("/poem/42?single=1"))
        assertEquals(PoemRef(PoetSource.Local, 4, "farrokhzad"), parseQuoteHref("/farrokhzad/4"))
        val quote = Quote(id = 1, key = "ganjoor:hafez:123", href = "/poem/999")
        assertEquals(PoemRef(PoetSource.Ganjoor, 123, "hafez"), quote.poemRef())
    }
}
