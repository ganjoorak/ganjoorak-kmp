package net.ganjoorak.app.data.account

import kotlinx.serialization.Serializable

/** Maximum hemistichs the server keeps on one quote; the website uses the same cap. */
const val MAX_QUOTE_LINES = 8

/** One shared fragment, matching the accounts service `Quote`. */
@Serializable
data class Quote(
    val id: Long,
    val key: String,
    val href: String = "",
    val poetName: String = "",
    val poemTitle: String = "",
    /** Inclusive 0-based line indices into the poem's visible lines. */
    val from: Int = 0,
    val to: Int = 0,
    val lines: List<String> = emptyList(),
    val createdAt: Long = 0,
)

@Serializable
data class StoryUser(
    val username: String,
    val displayName: String = "",
    val avatar: String = "",
    val accent: String = "",
)

/** A followed account with fragments from the last 7 days, as `GET following/quotes` returns it. */
@Serializable
data class StoryPerson(
    val user: StoryUser,
    val quotes: List<Quote> = emptyList(),
    val latestAt: Long = 0,
)

data class QuotesPage(
    val quotes: List<Quote> = emptyList(),
    val next: Long? = null,
)

fun StoryPerson.newestQuoteId(): Long = quotes.maxOfOrNull { it.id } ?: 0

fun StoryPerson.chronological(): List<Quote> = quotes.sortedBy { it.id }

fun StoryPerson.isSeen(seen: Map<String, Long>): Boolean =
    (seen[user.username] ?: 0L) >= newestQuoteId()

/** Index of the first quote (oldest first) newer than the last one seen; 0 when all are seen. */
fun StoryPerson.firstUnseenIndex(seen: Map<String, Long>): Int {
    val seenId = seen[user.username] ?: 0L
    val index = chronological().indexOfFirst { it.id > seenId }
    return if (index < 0) 0 else index
}

/** Unseen people first (each group newest first), matching `useFollowingStories`. */
fun sortStoryPeople(people: List<StoryPerson>, seen: Map<String, Long>): List<StoryPerson> {
    val withQuotes = people.filter { it.quotes.isNotEmpty() }
    val unseen = withQuotes.filter { !it.isSeen(seen) }.sortedByDescending { it.latestAt }
    val read = withQuotes.filter { it.isSeen(seen) }.sortedByDescending { it.latestAt }
    return unseen + read
}
