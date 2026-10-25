package net.ganjoorak.app.data

import kotlinx.serialization.Serializable

data class Poet(
    val id: Int,
    val name: String,
    val slug: String,
    val nickname: String?,
    val description: String?,
    val imageUrl: String,
    val rootCatId: Int,
    val birthYear: Int?,
    val deathYear: Int?,
    val birthPlace: String?,
    val deathPlace: String?,
)

data class PoetGroup(val title: String, val poets: List<Poet>)

data class CategoryRef(val id: Int, val title: String)

data class PoemSummary(val id: Int, val title: String, val excerpt: String?)

data class Category(
    val id: Int,
    val title: String,
    val description: String?,
    val poet: Poet?,
    val ancestors: List<CategoryRef>,
    val children: List<CategoryRef>,
    val poems: List<PoemSummary>,
)

data class PoetDetail(val poet: Poet, val root: Category)

/** Mirrors Ganjoor's `VersePosition` enum. */
enum class VersePosition {
    Right, Left, CenteredVerse1, CenteredVerse2, Single, Comment, Paragraph;

    companion object {
        fun fromApi(value: Int): VersePosition = when (value) {
            0 -> Right
            1 -> Left
            2 -> CenteredVerse1
            3 -> CenteredVerse2
            4 -> Single
            5 -> Comment
            -1 -> Paragraph
            else -> Single
        }
    }
}

data class Verse(val order: Int, val position: VersePosition, val text: String)

/** A visual unit of a poem: a couplet (beyt), a band line, a free-verse line or prose. */
sealed interface PoemBlock {
    val verses: List<Verse>

    data class Couplet(val first: Verse, val second: Verse?) : PoemBlock {
        override val verses get() = listOfNotNull(first, second)
    }

    data class Centered(override val verses: List<Verse>) : PoemBlock

    data class Line(val verse: Verse) : PoemBlock {
        override val verses get() = listOf(verse)
    }

    data class Prose(val verse: Verse) : PoemBlock {
        override val verses get() = listOf(verse)
    }

    data class Note(val verse: Verse) : PoemBlock {
        override val verses get() = listOf(verse)
    }
}

data class Recitation(
    val id: Int,
    val title: String,
    val artist: String,
    val mp3Url: String,
    val inSync: Boolean,
)

data class Poem(
    val id: Int,
    val title: String,
    val fullTitle: String,
    val fullUrl: String,
    val poetId: Int?,
    val poetName: String,
    val poetImageUrl: String?,
    val breadcrumbs: List<CategoryRef>,
    val verses: List<Verse>,
    val blocks: List<PoemBlock>,
    val recitations: List<Recitation>,
    val previousId: Int?,
    val nextId: Int?,
    val summary: String?,
) {
    val webUrl: String get() = "https://ganjoor.net$fullUrl"

    /** Plain text suitable for sharing/copying: hemistichs of a couplet share a line. */
    fun shareText(): String = buildString {
        appendLine("$poetName » $title")
        appendLine()
        blocks.forEach { block ->
            when (block) {
                is PoemBlock.Couplet -> {
                    appendLine(block.first.text)
                    block.second?.let { appendLine(it.text) }
                    appendLine()
                }
                else -> block.verses.forEach { appendLine(it.text) }
            }
        }
        append(webUrl)
    }.trim()

    val excerpt: String get() = verses.take(2).joinToString(" / ") { it.text }
}

data class SearchHit(
    val poemId: Int,
    val title: String,
    val poetName: String,
    val breadcrumb: String?,
    val snippet: String,
)

data class SearchPage(val hits: List<SearchHit>, val page: Int, val hasNext: Boolean, val total: Int?)

data class VerseTiming(val verseOrder: Int, val startMs: Long)

@Serializable
data class SavedPoem(
    val id: Int,
    val title: String,
    val poetName: String,
    val excerpt: String,
)
