package net.ganjoorak.app.ui.poets

import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.offline.OfflineItem
import net.ganjoorak.app.data.offline.OfflineItemKind
import net.ganjoorak.app.data.offline.OfflineJob
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineUnitViewTest {
    private val ref = PoemRef(PoetSource.Ganjoor, 1, "hafez")

    private fun book(pending: Boolean, completed: Int = 0, total: Int = 10) = OfflineItem(
        id = "book:1",
        kind = OfflineItemKind.Book,
        title = "دیوان",
        totalRefs = List(total) { ref },
        pendingRefs = if (pending) List(total - completed) { ref } else emptyList(),
        completedRefs = List(completed) { ref },
        bytes = 12_000,
    )

    private fun poet(complete: Boolean) = OfflineItem(
        id = "poet:hafez",
        kind = OfflineItemKind.Poet,
        title = "همهٔ آثار حافظ",
        pendingRefs = if (complete) emptyList() else listOf(ref),
        completedRefs = if (complete) listOf(ref) else emptyList(),
        totalRefs = listOf(ref),
        bytes = 80_000,
    )

    @Test
    fun idleWhenNothingIsStored() {
        val view = offlineUnitView("book:1", emptyList(), emptyList(), coveringPoetId = "poet:hafez")
        assertEquals(OfflineUnitPhase.Idle, view.phase)
    }

    @Test
    fun busyUsesJobPercent() {
        val view = offlineUnitView("poet:hafez", listOf(poet(false)), listOf(OfflineJob("poet:hafez", 2, 10)))
        assertEquals(OfflineUnitPhase.Busy, view.phase)
        assertEquals(20, view.percent)
    }

    @Test
    fun savedWhenComplete() {
        val view = offlineUnitView("book:1", listOf(book(pending = false, completed = 10)), emptyList())
        assertEquals(OfflineUnitPhase.Saved, view.phase)
        assertEquals(12_000, view.bytes)
    }

    @Test
    fun partialWhenIncompleteAndNotRunning() {
        val view = offlineUnitView("book:1", listOf(book(pending = true, completed = 3, total = 10)), emptyList())
        assertEquals(OfflineUnitPhase.Partial, view.phase)
        assertEquals(30, view.percent)
    }

    @Test
    fun coveredWhenPoetsWorksAreSaved() {
        val view = offlineUnitView(
            "book:1",
            listOf(poet(complete = true)),
            emptyList(),
            coveringPoetId = "poet:hafez",
        )
        assertEquals(OfflineUnitPhase.Covered, view.phase)
    }
}
