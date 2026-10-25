package net.ganjoorak.app.data

import kotlinx.serialization.Serializable

/** Where a poet's poems come from: the Ganjoor API, the Echolalia archive or the bundled collections. */
@Serializable
enum class PoetSource { Ganjoor, Echolalia, Local }

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
    val source: PoetSource = PoetSource.Ganjoor,
    /** Echolalia/local grouping such as «شاعران عرب»; null for Ganjoor (grouped by century). */
    val group: String? = null,
) {
    /** Stable identifier across sources, used for routes and the home-feed selection. */
    val key: String get() = PoetKey(source, if (source == PoetSource.Ganjoor) id.toString() else slug).value
}

/** `ganjoor:<id>`, `echolalia:<slug>` or `local:<slug>`. */
data class PoetKey(val source: PoetSource, val ref: String) {
    val value: String get() = "${source.name.lowercase()}:$ref"

    companion object {
        fun parse(value: String): PoetKey? {
            val source = when (value.substringBefore(':')) {
                "ganjoor" -> PoetSource.Ganjoor
                "echolalia" -> PoetSource.Echolalia
                "local" -> PoetSource.Local
                else -> return null
            }
            val ref = value.substringAfter(':', "")
            return if (ref.isEmpty()) null else PoetKey(source, ref)
        }
    }
}

data class PoetGroup(val title: String, val poets: List<Poet>)

data class CategoryRef(val id: Int, val title: String)

/** Identifies a poem across sources; [poetSlug] is needed for local poems, whose ids are per poet. */
@Serializable
data class PoemRef(val source: PoetSource, val id: Int, val poetSlug: String = "") {
    val key: String get() = "${source.name.lowercase()}:$poetSlug:$id"
}

data class PoemSummary(val ref: PoemRef, val title: String, val excerpt: String?)

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

/** A visual unit of a poem: a couplet (beyt), a band line, a free-verse line, prose or a stanza break. */
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

    data object Break : PoemBlock {
        override val verses get() = emptyList<Verse>()
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
    val ref: PoemRef,
    val title: String,
    val fullTitle: String,
    /** Page of the poem at its source (ganjoor.net, echolalia.ir or the original publication). */
    val link: String,
    val poetId: Int?,
    val poetKey: String?,
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
    val id: Int get() = ref.id
    val source: PoetSource get() = ref.source

    /** The poem on the Ganjoorak website, used for shared links. */
    val shareUrl: String
        get() = when (source) {
            PoetSource.Ganjoor -> "https://ganjoorak.ir/poem/$id"
            else -> "https://ganjoorak.ir/${ref.poetSlug}/$id"
        }

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
                is PoemBlock.Break -> appendLine()
                else -> block.verses.forEach { appendLine(it.text) }
            }
        }
        append(shareUrl)
    }.trim()

    val excerpt: String get() = verses.take(2).joinToString(" / ") { it.text }
}

data class SearchHit(
    val ref: PoemRef,
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
    val source: PoetSource = PoetSource.Ganjoor,
    val poetSlug: String = "",
) {
    val ref: PoemRef get() = PoemRef(source, id, poetSlug)
}
