package net.ganjoorak.app

import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.DailyPoemPicker
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.VersePosition
import net.ganjoorak.app.data.local.AppSettings
import net.ganjoorak.app.data.local.DailyPoem
import net.ganjoorak.app.data.local.DailyPoemMode
import net.ganjoorak.app.data.local.FollowedPoet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyPoemTest {
    // Epoch millis grounded with `TZ=Asia/Tehran date`.
    private val epochZero = 0L
    private val nov24Midnight = 1795465800000L // 2026-11-24 00:00 Tehran
    private val nov24Eight = 1795494600000L // 2026-11-24 08:00 Tehran
    private val nov24Nine = 1795498200000L // 2026-11-24 09:00 Tehran
    private val nov24HalfPastTen = 1795503600000L // 2026-11-24 10:30 Tehran
    private val nov23ElevenPm = 1795462200000L // 2026-11-23 23:00 Tehran

    private fun fakePoem() = Poem(
        ref = PoemRef(PoetSource.Ganjoor, 123),
        title = "غزل شمارهٔ ۱",
        fullTitle = "حافظ » غزل شمارهٔ ۱",
        link = "",
        poetId = 1,
        poetKey = "ganjoor:1",
        poetName = "حافظ",
        poetImageUrl = null,
        breadcrumbs = emptyList(),
        verses = listOf(
            Verse(1, VersePosition.Right, "الا یا ایها الساقی ادر کاسا و ناولها"),
            Verse(2, VersePosition.Left, "که عشق آسان نمود اول ولی افتاد مشکل‌ها"),
            Verse(3, VersePosition.Right, "به بوی نافه‌ای کاخر صبا زان طره بگشاید"),
            Verse(4, VersePosition.Left, "ز تاب جعد مشکینش چه خون افتاد در دل‌ها"),
            Verse(5, VersePosition.Right, "مرا در منزل جانان چه امن عیش چون هر دم"),
        ),
        blocks = emptyList(),
        recitations = emptyList(),
        previousId = null,
        nextId = null,
        summary = null,
    )

    private fun stored(date: String, mode: DailyPoemMode = DailyPoemMode.Random, poetKey: String? = null) =
        DailyPoem(
            date = date,
            mode = mode,
            poetKey = poetKey,
            ref = PoemRef(PoetSource.Ganjoor, 1),
            title = "غزل",
            poetName = "حافظ",
            lines = listOf("بیت اول"),
        )

    @Test
    fun tehranDateIsUtcPlusThreeThirty() {
        assertEquals("1970-01-01", DailyPoemPicker.todayString(epochZero))
        assertEquals("2026-11-24", DailyPoemPicker.todayString(nov24Midnight))
        assertEquals("2026-11-24", DailyPoemPicker.todayString(nov24HalfPastTen))
        assertEquals("2026-11-23", DailyPoemPicker.todayString(nov23ElevenPm))
    }

    @Test
    fun delayUntilNextDailyTime() {
        assertEquals(3_600_000L, DailyPoemPicker.millisUntilNextDaily(9, 0, nov24Eight))
        // Exactly at the time means tomorrow, not now.
        assertEquals(86_400_000L, DailyPoemPicker.millisUntilNextDaily(9, 0, nov24Nine))
        assertEquals(81_000_000L, DailyPoemPicker.millisUntilNextDaily(9, 0, nov24HalfPastTen))
    }

    @Test
    fun staleness() {
        val settings = AppSettings()
        assertTrue(DailyPoemPicker.isStale(null, "2026-11-24", settings))
        assertFalse(DailyPoemPicker.isStale(stored("2026-11-24"), "2026-11-24", settings))
        assertTrue(DailyPoemPicker.isStale(stored("2026-11-23"), "2026-11-24", settings))
        assertTrue(
            DailyPoemPicker.isStale(
                stored("2026-11-24", DailyPoemMode.Followed),
                "2026-11-24",
                settings.copy(dailyPoemMode = DailyPoemMode.Random),
            ),
        )
        assertTrue(
            DailyPoemPicker.isStale(
                stored("2026-11-24", DailyPoemMode.Poet, "ganjoor:2"),
                "2026-11-24",
                settings.copy(dailyPoemMode = DailyPoemMode.Poet, dailyPoetKey = "ganjoor:1"),
            ),
        )
    }

    @Test
    fun refreshPicksAndStoresWhenStale() = runTest {
        var saved: DailyPoem? = null
        var fetchedKey: String? = "unset"
        val picked = DailyPoemPicker.refreshIfNeeded(
            fetchRandom = { key ->
                fetchedKey = key
                fakePoem()
            },
            settings = AppSettings(),
            stored = null,
            onPicked = { saved = it },
            nowMillis = nov24Eight,
        )
        assertEquals("2026-11-24", picked?.date)
        assertEquals("حافظ", picked?.poetName)
        assertEquals(4, picked?.lines?.size)
        assertEquals("الا یا ایها الساقی ادر کاسا و ناولها", picked?.lines?.first())
        assertEquals(saved, picked)
        assertNull(fetchedKey)
    }

    @Test
    fun refreshKeepsCurrentPoemWithoutFetching() = runTest {
        var fetched = false
        val current = stored("2026-11-24")
        val picked = DailyPoemPicker.refreshIfNeeded(
            fetchRandom = {
                fetched = true
                fakePoem()
            },
            settings = AppSettings(),
            stored = current,
            onPicked = { error("must not pick again") },
            nowMillis = nov24Eight,
        )
        assertEquals(current, picked)
        assertFalse(fetched)
    }

    @Test
    fun refreshKeepsPreviousPoemWhenFetchFails() = runTest {
        val previous = stored("2026-11-23")
        val picked = DailyPoemPicker.refreshIfNeeded(
            fetchRandom = { throw IllegalStateException("offline") },
            settings = AppSettings(),
            stored = previous,
            onPicked = { error("must not store a failed pick") },
            nowMillis = nov24Eight,
        )
        assertEquals(previous, picked)
    }

    @Test
    fun followedModePicksFromFollowedPoets() = runTest {
        val settings = AppSettings(
            dailyPoemMode = DailyPoemMode.Followed,
            followedPoets = listOf(
                FollowedPoet("ganjoor:1", "حافظ"),
                FollowedPoet("ganjoor:2", "سعدی"),
            ),
        )
        var fetchedKey: String? = null
        DailyPoemPicker.refreshIfNeeded(
            fetchRandom = { key ->
                fetchedKey = key
                fakePoem()
            },
            settings = settings,
            stored = null,
            onPicked = {},
            nowMillis = nov24Eight,
        )
        assertTrue(fetchedKey == "ganjoor:1" || fetchedKey == "ganjoor:2")
    }
}
