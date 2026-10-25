package net.ganjoorak.app

import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.VersePosition
import net.ganjoorak.app.data.groupVerses
import net.ganjoorak.app.data.remote.GanjoorJson
import net.ganjoorak.app.data.remote.PoemDto
import net.ganjoorak.app.data.toPoem
import net.ganjoorak.app.data.toSearchHit
import net.ganjoorak.app.player.activeVerseAt
import net.ganjoorak.app.data.VerseTiming
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MapperTest {
    private val samplePoem = """
        {
          "id": 2130,
          "title": "غزل شمارهٔ ۱",
          "fullTitle": "حافظ » غزلیات » غزل شمارهٔ ۱",
          "fullUrl": "/hafez/ghazal/sh1",
          "unknownField": {"nested": true},
          "category": {
            "poet": {"id": 2, "name": "حافظ", "fullUrl": "/hafez", "imageUrl": "/api/ganjoor/poet/image/hafez.gif", "rootCatId": 24},
            "cat": {"id": 25, "title": "غزلیات", "ancestors": [{"id": 24, "title": "حافظ"}]}
          },
          "next": {"id": 2131, "title": "غزل شمارهٔ ۲"},
          "previous": null,
          "verses": [
            {"vOrder": 2, "versePosition": 1, "text": "که عشق آسان نمود اول ولی افتاد مشکل‌ها"},
            {"vOrder": 1, "versePosition": 0, "text": "الا یا ایها الساقی ادر کأسا و ناولها"},
            {"vOrder": 3, "versePosition": 4, "text": "سطر آزاد"},
            {"vOrder": 4, "versePosition": -1, "text": "نثر"}
          ],
          "recitations": [
            {"id": 9, "audioArtist": "B", "mp3Url": "https://x/b.mp3", "audioOrder": 2, "inSyncWithText": true},
            {"id": 8, "audioArtist": "A", "mp3Url": "https://x/a.mp3", "audioOrder": 1},
            {"id": 7, "audioArtist": "broken", "mp3Url": null, "audioOrder": 0}
          ]
        }
    """.trimIndent()

    @Test
    fun mapsPoemPayload() {
        val poem = GanjoorJson.decodeFromString<PoemDto>(samplePoem).toPoem()
        assertEquals("حافظ", poem.poetName)
        assertEquals(2, poem.poetId)
        assertEquals("https://api.ganjoor.net/api/ganjoor/poet/image/hafez.gif", poem.poetImageUrl)
        assertEquals(listOf("حافظ", "غزلیات"), poem.breadcrumbs.map { it.title })
        assertEquals(2131, poem.nextId)
        assertNull(poem.previousId)
        assertEquals(listOf(1, 2, 3, 4), poem.verses.map { it.order })
        assertEquals(listOf("A", "B"), poem.recitations.map { it.artist })
        assertEquals("https://ganjoor.net/hafez/ghazal/sh1", poem.webUrl)

        assertEquals(3, poem.blocks.size)
        val couplet = assertIs<PoemBlock.Couplet>(poem.blocks[0])
        assertEquals(1, couplet.first.order)
        assertEquals(2, couplet.second?.order)
        assertIs<PoemBlock.Line>(poem.blocks[1])
        assertIs<PoemBlock.Prose>(poem.blocks[2])
    }

    @Test
    fun fallsBackToPlainTextWhenVersesMissing() {
        val dto = PoemDto(id = 1, title = "t", fullTitle = "سعدی » t", htmlText = "<p>یک</p><p>دو</p><p>سه</p>")
        val poem = dto.toPoem()
        assertEquals("سعدی", poem.poetName)
        assertEquals(2, poem.blocks.size)
        assertEquals("سه", assertIs<PoemBlock.Couplet>(poem.blocks[1]).first.text)
    }

    @Test
    fun groupsCenteredBands() {
        val verses = listOf(
            Verse(1, VersePosition.Right, "a"),
            Verse(2, VersePosition.Left, "b"),
            Verse(3, VersePosition.CenteredVerse1, "c"),
            Verse(4, VersePosition.CenteredVerse2, "d"),
            Verse(5, VersePosition.Left, "orphan"),
        )
        val blocks = groupVerses(verses)
        assertEquals(3, blocks.size)
        assertEquals(listOf("c", "d"), assertIs<PoemBlock.Centered>(blocks[1]).verses.map { it.text })
        assertNull(assertIs<PoemBlock.Couplet>(blocks[2]).second)
    }

    @Test
    fun searchHitUsesMatchingCouplet() {
        val dto = PoemDto(
            id = 5,
            title = "غزل",
            fullTitle = "حافظ » غزلیات » غزل",
            plainText = "مصراع یک\nمصراع دو\nدل می‌رود ز دستم\nصاحب‌دلان خدا را",
        )
        val hit = dto.toSearchHit("دستم")
        assertEquals("دل می‌رود ز دستم / صاحب‌دلان خدا را", hit.snippet)
        assertEquals("غزلیات", hit.breadcrumb)
        assertEquals("حافظ", hit.poetName)
    }

    @Test
    fun findsActiveVerse() {
        val timings = listOf(VerseTiming(1, 0), VerseTiming(2, 1000), VerseTiming(3, 2500))
        assertEquals(1, activeVerseAt(timings, 0))
        assertEquals(2, activeVerseAt(timings, 2499))
        assertEquals(3, activeVerseAt(timings, 99_000))
        assertNull(activeVerseAt(emptyList(), 10))
        assertNull(activeVerseAt(listOf(VerseTiming(1, 500)), 100))
    }
}
