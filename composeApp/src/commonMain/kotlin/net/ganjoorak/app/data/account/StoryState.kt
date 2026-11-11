package net.ganjoorak.app.data.account

import com.russhwolf.settings.Settings
import net.ganjoorak.app.data.remote.GanjoorJson
import net.ganjoorak.app.platform.currentTimeMillis
import net.ganjoorak.app.util.toPersianDigits
import kotlin.math.max
import kotlin.math.roundToInt

/** Per-device seen map, same key the website stores in localStorage. */
const val STORIES_SEEN_KEY = "ganjoorak:stories-seen:v1"

const val STORY_DURATION_MS = 7_000L
const val STORY_HOLD_MS = 220L
const val STORY_DISMISS_DRAG_PX = 110f
const val STORY_CACHE_TTL_MS = 2 * 60_000L

fun readStorySeen(settings: Settings): Map<String, Long> {
    val raw = settings.getStringOrNull(STORIES_SEEN_KEY) ?: return emptyMap()
    return runCatching {
        GanjoorJson.decodeFromString<Map<String, Long>>(raw).filterValues { it >= 0 }
    }.getOrDefault(emptyMap())
}

/** Remembers that [quoteId] of [username] was shown; never goes backwards. */
fun nextStorySeen(seen: Map<String, Long>, username: String, quoteId: Long): Map<String, Long> {
    if ((seen[username] ?: 0L) >= quoteId) return seen
    return seen + (username to quoteId)
}

fun markStorySeen(settings: Settings, username: String, quoteId: Long): Map<String, Long> {
    val next = nextStorySeen(readStorySeen(settings), username, quoteId)
    settings.putString(STORIES_SEEN_KEY, GanjoorJson.encodeToString(next))
    return next
}

/**
 * Relative Persian time for a story header: «همین الان», «۵ دقیقه پیش», «دیروز»…
 * Matches the website's `formatStoryTime` (`fa` + `numeric: "auto"`).
 */
fun formatStoryTime(timestamp: Long, now: Long = currentTimeMillis()): String {
    val seconds = max(0, ((now - timestamp) / 1000.0).roundToInt())
    return when {
        seconds < 45 -> "همین الان"
        seconds < 3_600 -> {
            val minutes = max(1, (seconds / 60.0).roundToInt())
            if (minutes == 1) "۱ دقیقه پیش" else "${minutes.toPersianDigits()} دقیقه پیش"
        }
        seconds < 86_400 -> {
            val hours = max(1, (seconds / 3_600.0).roundToInt())
            if (hours == 1) "۱ ساعت پیش" else "${hours.toPersianDigits()} ساعت پیش"
        }
        else -> {
            val days = max(1, (seconds / 86_400.0).roundToInt())
            if (days == 1) "دیروز" else "${days.toPersianDigits()} روز پیش"
        }
    }
}
