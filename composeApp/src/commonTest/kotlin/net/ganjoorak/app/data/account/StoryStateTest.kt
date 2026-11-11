package net.ganjoorak.app.data.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StoryStateTest {
    private fun person(username: String, ids: List<Long>, latestAt: Long = ids.maxOrNull() ?: 0): StoryPerson =
        StoryPerson(
            user = StoryUser(username = username, displayName = username),
            quotes = ids.map { id -> Quote(id = id, key = "ganjoor:hafez:1", lines = listOf("بیت $id"), createdAt = id) },
            latestAt = latestAt,
        )

    @Test
    fun unseenPeopleSortFirstThenByLatest() {
        val seen = mapOf("old" to 10L)
        val people = listOf(
            person("old", listOf(10), latestAt = 100),
            person("newA", listOf(1, 2), latestAt = 50),
            person("newB", listOf(3), latestAt = 80),
        )
        val sorted = sortStoryPeople(people, seen)
        assertEquals(listOf("newB", "newA", "old"), sorted.map { it.user.username })
    }

    @Test
    fun firstUnseenIndexStartsAtOldestUnread() {
        val someone = person("hadi", listOf(3, 1, 2))
        assertEquals(0, someone.firstUnseenIndex(emptyMap()))
        assertEquals(1, someone.firstUnseenIndex(mapOf("hadi" to 1L)))
        assertEquals(0, someone.firstUnseenIndex(mapOf("hadi" to 3L)))
    }

    @Test
    fun markSeenNeverGoesBackwards() {
        var seen = emptyMap<String, Long>()
        seen = nextStorySeen(seen, "hadi", 2)
        assertEquals(mapOf("hadi" to 2L), seen)
        seen = nextStorySeen(seen, "hadi", 1)
        assertEquals(mapOf("hadi" to 2L), seen)
        seen = nextStorySeen(seen, "hadi", 5)
        assertEquals(mapOf("hadi" to 5L), seen)
        assertTrue(person("hadi", listOf(5, 4)).isSeen(seen))
        assertFalse(person("hadi", listOf(6)).isSeen(seen))
    }

    @Test
    fun formatStoryTimeMatchesWebsiteBuckets() {
        val now = 1_700_000_000_000L
        assertEquals("همین الان", formatStoryTime(now - 10_000, now))
        assertEquals("۱ دقیقه پیش", formatStoryTime(now - 60_000, now))
        assertEquals("۵ دقیقه پیش", formatStoryTime(now - 5 * 60_000, now))
        assertEquals("۱ ساعت پیش", formatStoryTime(now - 3_600_000, now))
        assertEquals("۳ ساعت پیش", formatStoryTime(now - 3 * 3_600_000, now))
        assertEquals("دیروز", formatStoryTime(now - 86_400_000, now))
        assertEquals("۳ روز پیش", formatStoryTime(now - 3 * 86_400_000L, now))
    }
}
