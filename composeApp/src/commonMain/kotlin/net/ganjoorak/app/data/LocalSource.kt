package net.ganjoorak.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.remote.GanjoorJson
import net.ganjoorak.app.util.extractSnippet
import net.ganjoorak.app.util.normalizeForSearch

@Serializable
data class LocalCollectionDto(
    val poet: String = "",
    val poetSlug: String = "",
    val poems: List<LocalPoemDto> = emptyList(),
)

@Serializable
data class LocalPoemDto(
    val id: Int = 0,
    val title: String = "",
    val text: String = "",
    val source: String? = null,
    val collection: String? = null,
)

internal const val LOCAL_POETS_GROUP = "شاعران معاصر"

/** Poets whose collections ship with the app (the same JSON the website serves from /poems). */
class LocalSource(private val readCollection: suspend (slug: String) -> String) {
    private val mutex = Mutex()
    private val collections = mutableMapOf<String, LocalCollectionDto>()

    private suspend fun collection(slug: String): LocalCollectionDto = mutex.withLock {
        collections.getOrPut(slug) {
            if (slug !in POET_IDS) throw GanjoorException("شاعر پیدا نشد")
            GanjoorJson.decodeFromString(LocalCollectionDto.serializer(), readCollection(slug))
        }
    }

    private fun poet(slug: String, name: String) = Poet(
        id = POET_IDS.getValue(slug),
        name = name,
        slug = slug,
        nickname = null,
        description = null,
        imageUrl = "$SITE_IMAGES_URL/$slug.jpeg",
        rootCatId = 0,
        birthYear = null,
        deathYear = null,
        birthPlace = null,
        deathPlace = null,
        source = PoetSource.Local,
        group = LOCAL_POETS_GROUP,
    )

    suspend fun poets(): List<Poet> = POET_IDS.keys.map { poet(it) }

    suspend fun poet(slug: String): Poet = poet(slug, collection(slug).poet)

    suspend fun poemList(slug: String): List<PoemSummary> = collection(slug).poems.map {
        PoemSummary(PoemRef(PoetSource.Local, it.id, slug), it.title, it.text.lineSequence().map(String::trim).firstOrNull { l -> l.isNotEmpty() })
    }

    suspend fun poem(slug: String, id: Int): Poem {
        val collection = collection(slug)
        val dto = collection.poems.firstOrNull { it.id == id } ?: throw GanjoorException("مورد درخواستی پیدا نشد")
        return dto.toPoem(poet(slug, collection.poet))
    }

    suspend fun randomPoem(slug: String): Poem {
        val collection = collection(slug)
        if (collection.poems.isEmpty()) throw GanjoorException("شعری برای این شاعر پیدا نشد")
        return collection.poems.random().toPoem(poet(slug, collection.poet))
    }

    suspend fun search(term: String): List<SearchHit> {
        val needle = term.normalizeForSearch()
        if (needle.length < 2) return emptyList()
        return POET_IDS.keys.flatMap { slug ->
            val collection = collection(slug)
            collection.poems
                .filter { listOfNotNull(it.title, it.collection, it.text).joinToString(" ").normalizeForSearch().contains(needle) }
                .map {
                    SearchHit(
                        ref = PoemRef(PoetSource.Local, it.id, slug),
                        title = it.title,
                        poetName = collection.poet,
                        breadcrumb = it.collection?.trim()?.takeIf { c -> c.isNotEmpty() },
                        snippet = extractSnippet(it.text, term),
                    )
                }
        }.take(12)
    }

    private fun LocalPoemDto.toPoem(poet: Poet): Poem {
        val (verses, blocks) = freeVerse(text)
        val collectionTitle = collection?.trim()?.takeIf { it.isNotEmpty() }
        return Poem(
            ref = PoemRef(PoetSource.Local, id, poet.slug),
            title = title,
            fullTitle = listOfNotNull(poet.name, collectionTitle, title).joinToString(" » "),
            link = source.orEmpty(),
            poetId = null,
            poetKey = poet.key,
            poetName = poet.name,
            poetImageUrl = poet.imageUrl,
            breadcrumbs = listOfNotNull(CategoryRef(0, poet.name), collectionTitle?.let { CategoryRef(0, it) }),
            verses = verses,
            blocks = blocks,
            recitations = emptyList(),
            previousId = null,
            nextId = null,
            summary = null,
        )
    }

    companion object {
        /** Ids the website assigns to the bundled poets. */
        val POET_IDS = linkedMapOf("rahmani" to 101, "farrokhzad" to 102)
    }
}
