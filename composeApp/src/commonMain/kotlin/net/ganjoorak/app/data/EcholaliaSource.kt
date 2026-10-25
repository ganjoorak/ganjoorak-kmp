package net.ganjoorak.app.data

import io.ktor.http.decodeURLPart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ganjoorak.app.data.remote.EcholaliaApi
import net.ganjoorak.app.data.remote.WpCategory
import net.ganjoorak.app.data.remote.WpPost
import net.ganjoorak.app.util.decodeHtmlEntities
import net.ganjoorak.app.util.extractSnippet
import net.ganjoorak.app.util.stripHtml
import net.ganjoorak.app.util.wordpressToText

/** Poets under this category are Echolalia's poetry archive. */
private const val POET_ROOT_SLUG = "sher"

/** Shown from the bundled collection instead (mirrors the website). */
private val HIDDEN_POET_SLUGS = setOf("forough-farrokhzad")

internal const val SITE_IMAGES_URL = "https://ganjoorak.ir/images/poets"

internal fun WpCategory.toEcholaliaPoet(group: String?): Poet {
    val slug = slug.orEmpty().decodeSlug()
    return Poet(
        id = id,
        name = name.orEmpty().decodeHtmlEntities(),
        slug = slug,
        nickname = null,
        description = description?.stripHtml()?.decodeHtmlEntities()?.takeIf { it.isNotBlank() },
        imageUrl = "$SITE_IMAGES_URL/echolalia-$slug.jpg",
        rootCatId = id,
        birthYear = null,
        deathYear = null,
        birthPlace = null,
        deathPlace = null,
        source = PoetSource.Echolalia,
        group = group?.decodeHtmlEntities(),
    )
}

private fun String.decodeSlug(): String = runCatching { decodeURLPart() }.getOrDefault(this)

internal fun WpPost.toEcholaliaPoem(poet: Poet): Poem {
    val title = title?.rendered.orEmpty().decodeHtmlEntities()
    val (verses, blocks) = freeVerse(content?.rendered.orEmpty().wordpressToText())
    return Poem(
        ref = PoemRef(PoetSource.Echolalia, id, poet.slug),
        title = title,
        fullTitle = "${poet.name} » $title",
        link = link.orEmpty(),
        poetId = null,
        poetKey = poet.key,
        poetName = poet.name,
        poetImageUrl = poet.imageUrl,
        breadcrumbs = emptyList(),
        verses = verses,
        blocks = blocks,
        recitations = emptyList(),
        previousId = null,
        nextId = null,
        summary = null,
    )
}

/**
 * Echolalia poets are the leaf categories (with posts) under the «sher» category; their parent
 * category names the group, e.g. «شاعران عرب».
 */
internal fun selectEcholaliaPoets(categories: List<WpCategory>, rootId: Int): List<Poet> {
    val children = categories.groupBy { it.parent }
    val byId = categories.associateBy { it.id }
    val descendants = mutableSetOf<Int>()
    fun collect(parent: Int) {
        children[parent].orEmpty().forEach { if (descendants.add(it.id)) collect(it.id) }
    }
    collect(rootId)
    return categories
        .filter { it.id in descendants && it.count > 0 && children[it.id].isNullOrEmpty() }
        .filter { it.slug.orEmpty().decodeSlug() !in HIDDEN_POET_SLUGS }
        .map { it.toEcholaliaPoet(group = byId[it.parent]?.name) }
        .sortedWith(compareBy(PersianCollator) { it.name })
}

class EcholaliaSource(private val api: EcholaliaApi) {
    private val mutex = Mutex()
    private var poets: List<Poet>? = null
    private val poemLists = LruCache<Int, List<PoemSummary>>(16)

    suspend fun poets(): List<Poet> = mutex.withLock {
        poets ?: run {
            val root = api.categoryBySlug(POET_ROOT_SLUG) ?: return@run emptyList()
            val first = api.categories(1)
            val all = first.items + (2..first.totalPages).flatMap { api.categories(it).items }
            selectEcholaliaPoets(all, root.id)
        }.also { poets = it }
    }

    suspend fun poet(slug: String): Poet {
        poets?.firstOrNull { it.slug == slug }?.let { return it }
        val category = api.categoryBySlug(slug)?.takeIf { it.count > 0 && slug !in HIDDEN_POET_SLUGS }
            ?: throw GanjoorException("شاعر در اکولالیا پیدا نشد")
        val group = category.parent.takeIf { it > 0 }?.let { runCatching { api.category(it).name }.getOrNull() }
        return category.toEcholaliaPoet(group)
    }

    suspend fun poemList(poet: Poet): List<PoemSummary> = poemLists[poet.id] ?: run {
        val first = api.postsInCategory(poet.id, 1)
        val posts = first.items + (2..first.totalPages).flatMap { api.postsInCategory(poet.id, it).items }
        posts.map {
            PoemSummary(PoemRef(PoetSource.Echolalia, it.id, poet.slug), it.title?.rendered.orEmpty().decodeHtmlEntities(), null)
        }
    }.also { poemLists[poet.id] = it }

    suspend fun poem(id: Int): Poem {
        val post = api.post(id)
        val categoryId = post.categories.firstOrNull() ?: throw GanjoorException("شعر در اکولالیا پیدا نشد")
        val poet = poets?.firstOrNull { it.id == categoryId } ?: api.category(categoryId).toEcholaliaPoet(null)
        return post.toEcholaliaPoem(poet)
    }

    suspend fun randomPoem(slug: String): Poem {
        val poems = poemList(poet(slug))
        if (poems.isEmpty()) throw GanjoorException("شعری برای این شاعر پیدا نشد")
        return poem(poems.random().ref.id)
    }

    suspend fun search(query: String): List<SearchHit> {
        val known = runCatching { poets() }.getOrDefault(emptyList()).associateBy { it.id }
        return api.search(query, perPage = 12).mapNotNull { post ->
            val poet = post.categories.firstNotNullOfOrNull { known[it] } ?: return@mapNotNull null
            SearchHit(
                ref = PoemRef(PoetSource.Echolalia, post.id, poet.slug),
                title = post.title?.rendered.orEmpty().decodeHtmlEntities(),
                poetName = poet.name,
                breadcrumb = "اکولالیا",
                snippet = extractSnippet(post.content?.rendered.orEmpty().wordpressToText(), query),
            )
        }
    }
}
