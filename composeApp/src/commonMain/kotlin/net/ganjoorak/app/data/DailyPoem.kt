package net.ganjoorak.app.data

import net.ganjoorak.app.data.local.AppSettings
import net.ganjoorak.app.data.local.DailyPoem
import net.ganjoorak.app.data.local.DailyPoemMode

/**
 * The poem of the day, shared by the home-screen widget and the local daily
 * notification so both show the same poem all day.
 *
 * The poem is picked once per day (by the daily worker, or when the app opens
 * and the stored poem is stale) and stored; the widget and the notification
 * only ever read the stored poem, never pick their own. Tehran is UTC+3:30
 * year-round (Iran abolished DST in 2022), so the date math needs no timezone
 * database — a fixed offset is exact.
 */
object DailyPoemPicker {
    const val TEHRAN_OFFSET_MS = 3 * 3_600_000L + 30 * 60_000L
    private const val DAY_MS = 24 * 3_600_000L
    const val MAX_LINES = 4

    /** Tehran date for [nowMillis], `yyyy-MM-dd`. */
    fun todayString(nowMillis: Long): String {
        val (year, month, day) = epochDayToDate((nowMillis + TEHRAN_OFFSET_MS).floorDiv(DAY_MS))
        return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    /** Milliseconds from [nowMillis] until the next [hour]:[minute] in Tehran. */
    fun millisUntilNextDaily(hour: Int, minute: Int, nowMillis: Long): Long {
        val tehranNow = nowMillis + TEHRAN_OFFSET_MS
        val dayStart = tehranNow.floorDiv(DAY_MS) * DAY_MS
        var target = dayStart + hour * 3_600_000L + minute * 60_000L
        if (target <= tehranNow) target += DAY_MS
        return target - tehranNow
    }

    /** True when [stored] is missing, from another day, or picked under different settings. */
    fun isStale(stored: DailyPoem?, today: String, settings: AppSettings): Boolean {
        if (stored == null) return true
        if (stored.date != today) return true
        if (stored.mode != settings.dailyPoemMode) return true
        if (settings.dailyPoemMode == DailyPoemMode.Poet && stored.poetKey != settings.dailyPoetKey) return true
        return false
    }

    /**
     * Returns [stored] when it is still current, otherwise picks a new one
     * with [fetchRandom] (a [PoemRepository.randomPoem] bound to the chosen
     * poet, or null for any poet), hands it to [onPicked] and returns it. On
     * failure the previous poem is returned, so the widget never goes blank
     * just because the network is down; null only when no poem was ever picked.
     */
    suspend fun refreshIfNeeded(
        fetchRandom: suspend (String?) -> Poem,
        settings: AppSettings,
        stored: DailyPoem?,
        onPicked: (DailyPoem) -> Unit,
        nowMillis: Long,
    ): DailyPoem? {
        val today = todayString(nowMillis)
        if (!isStale(stored, today, settings)) return stored
        val poetKey = when (settings.dailyPoemMode) {
            DailyPoemMode.Poet -> settings.dailyPoetKey
            DailyPoemMode.Followed -> settings.followedPoets.randomOrNull()?.key
            DailyPoemMode.Random -> null
        }
        return try {
            val poem = fetchRandom(poetKey)
            DailyPoem(
                date = today,
                mode = settings.dailyPoemMode,
                poetKey = poetKey,
                ref = poem.ref,
                title = poem.title,
                poetName = poem.poetName,
                lines = poem.verses.take(MAX_LINES).map { it.text },
            ).also(onPicked)
        } catch (e: Exception) {
            println("Ganjoorak: daily poem pick failed, keeping previous: $e")
            stored
        }
    }

    /** Howard Hinnant's civil-from-days algorithm; [epochDay] is days since 1970-01-01. */
    internal fun epochDayToDate(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = z.floorDiv(146097)
        val doe = z - era * 146097 // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365 // [0, 399]
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100) // [0, 365]
        val mp = (5 * doy + 2) / 153 // [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1 // [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9 // [1, 12]
        val year = if (m <= 2) y + 1 else y
        return Triple(year.toInt(), m.toInt(), d.toInt())
    }
}
