package net.ganjoorak.app

import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.LocalSource
import net.ganjoorak.app.data.PersianCollator
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.PoetKey
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.freeVerse
import net.ganjoorak.app.data.remote.WpCategory
import net.ganjoorak.app.data.selectEcholaliaPoets
import net.ganjoorak.app.util.decodeHtmlEntities
import net.ganjoorak.app.util.wordpressToText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SourcesTest {
    @Test
    fun parsesPoetKeys() {
        assertEquals(PoetKey(PoetSource.Ganjoor, "2"), PoetKey.parse("ganjoor:2"))
        assertEquals(PoetKey(PoetSource.Local, "rahmani"), PoetKey.parse("local:rahmani"))
        assertEquals("echolalia:pablo-neruda", PoetKey(PoetSource.Echolalia, "pablo-neruda").value)
        assertNull(PoetKey.parse("other:1"))
        assertNull(PoetKey.parse("ganjoor:"))
    }

    @Test
    fun picksEcholaliaLeafPoetsUnderRoot() {
        val categories = listOf(
            WpCategory(id = 1, slug = "sher", parent = 0),
            WpCategory(id = 2, name = "شاعران عرب", slug = "arab", parent = 1),
            WpCategory(id = 3, name = "نزار قبانی", slug = "nizar", count = 5, parent = 2),
            WpCategory(id = 4, name = "آدونیس", slug = "adunis", count = 2, parent = 2),
            WpCategory(id = 5, name = "خالی", slug = "empty", count = 0, parent = 2),
            WpCategory(id = 6, name = "فروغ", slug = "forough-farrokhzad", count = 9, parent = 2),
            WpCategory(id = 7, name = "خبر", slug = "news", count = 9, parent = 0),
            WpCategory(id = 8, name = "پل الوار", slug = "%d9%be", count = 1, parent = 2),
        )
        val poets = selectEcholaliaPoets(categories, rootId = 1)
        assertEquals(listOf("آدونیس", "پل الوار", "نزار قبانی"), poets.map { it.name })
        assertEquals("شاعران عرب", poets.first().group)
        assertEquals("پ", poets[1].slug)
        assertEquals("echolalia:adunis", poets.first().key)
    }

    @Test
    fun convertsWordpressHtml() {
        val html = "<p>سطر&nbsp;یک<br />سطر دو</p>\n<p>بند &#8220;دوم&#8221;</p><div class=\"sing-block\"><p>پخش</p></div>"
        assertEquals("سطر یک\nسطر دو\n\nبند “دوم”", html.wordpressToText())
        assertEquals("a & b « c »", "a &amp; b &laquo; c &raquo;".decodeHtmlEntities())
    }

    @Test
    fun freeVerseKeepsStanzaBreaks() {
        val (verses, blocks) = freeVerse("\nیک\n \nدو\nسه\n\n")
        assertEquals(listOf(1, 2, 3), verses.map { it.order })
        assertEquals(4, blocks.size)
        assertIs<PoemBlock.Break>(blocks[1])
    }

    @Test
    fun sortsByPersianAlphabet() {
        assertEquals(listOf("آدونیس", "بیدل", "پابلو", "گوته", "یانیس"), listOf("گوته", "یانیس", "پابلو", "آدونیس", "بیدل").sortedWith(PersianCollator))
    }

    @Test
    fun readsBundledCollections() = runTest {
        val source = LocalSource { slug ->
            when (slug) {
                "rahmani" -> """{"poet": "نصرت رحمانی", "poems": [{"id": 1, "title": "کفر", "text": "خدایا\nتو\n \nبند", "source": "https://x"}]}"""
                else -> """{"poet": "فروغ فرخزاد", "poems": [{"id": 2, "title": "تولدی دیگر", "text": "همهٔ هستی من", "collection": "تولدی دیگر"}]}"""
            }
        }
        assertEquals(listOf("local:rahmani", "local:farrokhzad"), source.poets().map { it.key })
        val poem = source.poem("rahmani", 1)
        assertEquals("نصرت رحمانی » کفر", poem.fullTitle)
        assertEquals("https://x", poem.link)
        assertEquals("https://ganjoorak.ir/rahmani/1", poem.shareUrl)
        assertEquals(3, poem.verses.size)
        assertEquals("تولدی دیگر", source.poem("farrokhzad", 2).breadcrumbs.last().title)
        assertEquals(listOf(2), source.search("هستی").map { it.ref.id })
    }
}
