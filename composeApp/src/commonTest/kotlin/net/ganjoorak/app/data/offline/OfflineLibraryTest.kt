package net.ganjoorak.app.data.offline

import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.VersePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineLibraryTest {

    private fun poem(id: Int, text: String) = Poem(
        ref = PoemRef(PoetSource.Ganjoor, id, "hafez"),
        title = "شعر $id",
        fullTitle = "حافظ » شعر $id",
        link = "https://ganjoor.net/hafez/divan-$id",
        poetId = 2,
        poetKey = "ganjoor:2",
        poetName = "حافظ",
        poetImageUrl = null,
        breadcrumbs = listOf(CategoryRef(1, "غزلیات")),
        verses = listOf(Verse(1, VersePosition.Right, "${text}a")),
        blocks = listOf(PoemBlock.Line(Verse(1, VersePosition.Right, "${text}a"))),
        recitations = emptyList(),
        previousId = null,
        nextId = null,
        summary = null,
    )

    @Test
    fun roundTripsAndResumes() = runTest {
        val store = MemoryByteStore()
        val lib = OfflineLibrary(store)

        // 1. Save a single poem and read it back.
        val p1 = poem(1, "ب")
        val item1 = lib.savePoem(p1)
        assertEquals(OfflineItemKind.Poem, item1.kind)
        assertTrue(item1.complete)
        assertTrue(lib.hasPoem(p1.ref))
        assertNotNull(lib.getPoem(p1.ref)) { decoded ->
            assertEquals(p1.title, decoded.title)
            assertEquals(p1.verses, decoded.verses)
            assertEquals(p1.blocks, decoded.blocks)
        }

        // 2. Start a book job of 3 poems.
        val refs = listOf(2, 3, 4).map { PoemRef(PoetSource.Ganjoor, it, "hafez") }
        val bookDraft = OfflineItem(
            id = "book:ganjoor:1",
            kind = OfflineItemKind.Book,
            title = "غزلیات",
            subtitle = "حافظ",
            totalRefs = refs,
            pendingRefs = refs,
        )
        val started = lib.beginOrResume(bookDraft)
        assertEquals(bookDraft.id, started.id)
        assertFalse(started.complete)

        // 3. Save 1 of 3.
        lib.onPoemSaved(started.id, poem(2, "c"))
        assertFalse(lib.item(started.id)!!.complete)
        assertEquals(listOf(3, 4), lib.item(started.id)!!.pendingRefs.map { it.id })

        // 4. Restart: a new OfflineLibrary on the same store confirms the job is still partial.
        val restarted = OfflineLibrary(store)
        val resumed = restarted.item(started.id)!!
        assertFalse(resumed.complete)
        assertEquals(2, resumed.pendingRefs.size)
        assertEquals(listOf(3, 4), resumed.pendingRefs.map { it.id })

        // 5. Finish the remaining poems and confirm complete with bytes > 0.
        resumed.pendingRefs.forEach { ref ->
            restarted.onPoemSaved(resumed.id, poem(ref.id, "d"))
        }
        val finished = restarted.item(started.id)!!
        assertTrue(finished.complete)
        assertTrue(finished.bytes > 0)
        assertTrue(restarted.hasPoem(PoemRef(PoetSource.Ganjoor, 2, "hafez")))

        // 6. Remove the book; its poems are gone, but the single poem survives.
        restarted.remove(started.id)
        assertNull(restarted.item(started.id))
        assertFalse(restarted.hasPoem(PoemRef(PoetSource.Ganjoor, 2, "hafez")))
        assertTrue(restarted.hasPoem(p1.ref))
        assertTrue(restarted.items().any { it.id == item1.id })
    }
}
