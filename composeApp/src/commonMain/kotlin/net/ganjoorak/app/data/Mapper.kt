package net.ganjoorak.app.data

import net.ganjoorak.app.data.remote.CatDto
import net.ganjoorak.app.data.remote.GANJOOR_API_BASE_URL
import net.ganjoorak.app.data.remote.PoemDto
import net.ganjoorak.app.data.remote.PoetCompleteDto
import net.ganjoorak.app.data.remote.PoetDto
import net.ganjoorak.app.data.remote.RecitationDto
import net.ganjoorak.app.data.remote.SemanticResultDto
import net.ganjoorak.app.util.extractSnippet
import net.ganjoorak.app.util.stripHtml

internal fun slugFromUrl(fullUrl: String?): String =
    fullUrl.orEmpty().trim('/').substringBefore('/')

internal fun poetImageUrl(imageUrl: String?, slug: String): String = when {
    imageUrl.isNullOrBlank() -> "$GANJOOR_API_BASE_URL/api/ganjoor/poet/image/$slug.gif"
    imageUrl.startsWith("http") -> imageUrl
    else -> GANJOOR_API_BASE_URL + "/" + imageUrl.trimStart('/')
}

internal fun splitTitle(fullTitle: String?): List<String> =
    fullTitle.orEmpty().split("»").map { it.trim() }.filter { it.isNotEmpty() }

fun PoetDto.toPoet(): Poet {
    val slug = slugFromUrl(fullUrl)
    return Poet(
        id = id,
        name = name.orEmpty().ifBlank { nickname.orEmpty() },
        slug = slug,
        nickname = nickname?.takeIf { it.isNotBlank() },
        description = description?.stripHtml()?.takeIf { it.isNotBlank() },
        imageUrl = poetImageUrl(imageUrl, slug),
        rootCatId = rootCatId,
        birthYear = birthYearInLHijri?.takeIf { validBirthDate && it != 0 },
        deathYear = deathYearInLHijri?.takeIf { validDeathDate && it != 0 },
        birthPlace = birthPlace?.takeIf { it.isNotBlank() },
        deathPlace = deathPlace?.takeIf { it.isNotBlank() },
    )
}

private fun CatDto.toRef() = CategoryRef(id = id, title = title.orEmpty())

fun PoetCompleteDto.toCategory(): Category {
    val cat = cat ?: error("category missing")
    return Category(
        id = cat.id,
        title = cat.title.orEmpty(),
        description = (cat.descriptionHtml?.stripHtml() ?: cat.description)?.takeIf { it.isNotBlank() },
        poet = poet?.takeIf { it.id > 0 }?.toPoet(),
        ancestors = cat.ancestors.orEmpty().map { it.toRef() },
        children = cat.children.orEmpty().map { it.toRef() },
        poems = cat.poems.orEmpty().map {
            PoemSummary(id = it.id, title = it.title.orEmpty(), excerpt = it.excerpt?.stripHtml()?.takeIf { e -> e.isNotBlank() })
        },
    )
}

private fun RecitationDto.toRecitation() = Recitation(
    id = id,
    title = audioTitle.orEmpty(),
    artist = audioArtist.orEmpty(),
    mp3Url = (mp3Url ?: audioSrc).orEmpty(),
    inSync = inSyncWithText,
)

/**
 * Groups raw verses into display blocks: a Right hemistich followed by a Left one forms a couplet,
 * consecutive centered hemistichs form a band, and everything else stands alone.
 */
fun groupVerses(verses: List<Verse>): List<PoemBlock> {
    val blocks = mutableListOf<PoemBlock>()
    var i = 0
    while (i < verses.size) {
        val verse = verses[i]
        val next = verses.getOrNull(i + 1)
        when (verse.position) {
            VersePosition.Right -> {
                if (next?.position == VersePosition.Left) {
                    blocks += PoemBlock.Couplet(verse, next)
                    i++
                } else {
                    blocks += PoemBlock.Couplet(verse, null)
                }
            }
            VersePosition.Left -> blocks += PoemBlock.Couplet(verse, null)
            VersePosition.CenteredVerse1 -> {
                if (next?.position == VersePosition.CenteredVerse2) {
                    blocks += PoemBlock.Centered(listOf(verse, next))
                    i++
                } else {
                    blocks += PoemBlock.Centered(listOf(verse))
                }
            }
            VersePosition.CenteredVerse2 -> blocks += PoemBlock.Centered(listOf(verse))
            VersePosition.Single -> blocks += PoemBlock.Line(verse)
            VersePosition.Paragraph -> blocks += PoemBlock.Prose(verse)
            VersePosition.Comment -> blocks += PoemBlock.Note(verse)
        }
        i++
    }
    return blocks
}

/** Used when the API returns no structured verses: treat non-empty lines as couplet halves. */
internal fun versesFromText(text: String): List<Verse> =
    text.stripHtml().lines().map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { index, line ->
        Verse(order = index + 1, position = if (index % 2 == 0) VersePosition.Right else VersePosition.Left, text = line)
    }

fun PoemDto.toPoem(): Poem {
    require(id > 0) { "poem not found" }
    val titleParts = splitTitle(fullTitle)
    val poetDto = category?.poet?.takeIf { it.id > 0 }
    val poetName = poetDto?.name?.takeIf { it.isNotBlank() } ?: titleParts.firstOrNull().orEmpty()
    val verses = verses.orEmpty()
        .filter { !it.text.isNullOrBlank() }
        .sortedBy { it.vOrder }
        .map { Verse(order = it.vOrder, position = VersePosition.fromApi(it.versePosition), text = it.text!!.trim()) }
        .ifEmpty { versesFromText(htmlText ?: plainText.orEmpty()) }

    val cat = category?.cat
    val breadcrumbs = buildList {
        cat?.ancestors.orEmpty().forEach { add(it.toRef()) }
        if (cat != null && cat.id > 0) add(cat.toRef())
    }.filter { it.title.isNotBlank() }

    return Poem(
        id = id,
        title = title.orEmpty().ifBlank { titleParts.lastOrNull().orEmpty() },
        fullTitle = fullTitle.orEmpty(),
        fullUrl = fullUrl.orEmpty(),
        poetId = poetDto?.id,
        poetName = poetName,
        poetImageUrl = poetDto?.let { poetImageUrl(it.imageUrl, slugFromUrl(it.fullUrl)) }
            ?: slugFromUrl(fullUrl).takeIf { it.isNotEmpty() }?.let { poetImageUrl(null, it) },
        breadcrumbs = breadcrumbs,
        verses = verses,
        blocks = groupVerses(verses),
        recitations = recitations.orEmpty().sortedBy { it.audioOrder }.map { it.toRecitation() }.filter { it.mp3Url.isNotBlank() },
        previousId = previous?.id?.takeIf { it > 0 },
        nextId = next?.id?.takeIf { it > 0 },
        summary = poemSummary?.stripHtml()?.takeIf { it.isNotBlank() },
    )
}

/** True when a random/search payload lacks the details a reader view needs. */
internal fun PoemDto.isIncomplete(): Boolean = verses.isNullOrEmpty() || category?.poet == null

fun PoemDto.toSearchHit(term: String): SearchHit {
    val parts = splitTitle(fullTitle)
    return SearchHit(
        poemId = id,
        title = title.orEmpty().ifBlank { parts.lastOrNull().orEmpty() },
        poetName = parts.firstOrNull().orEmpty(),
        breadcrumb = parts.drop(1).dropLast(1).joinToString(" » ").takeIf { it.isNotBlank() },
        snippet = extractSnippet(plainText ?: htmlText?.stripHtml().orEmpty(), term),
    )
}

fun SemanticResultDto.toSearchHit(): SearchHit {
    val parts = splitTitle(fullTitle)
    return SearchHit(
        poemId = poemId.takeIf { it > 0 } ?: id,
        title = title.orEmpty().ifBlank { parts.lastOrNull().orEmpty() },
        poetName = parts.firstOrNull().orEmpty(),
        breadcrumb = parts.drop(1).dropLast(1).joinToString(" » ").takeIf { it.isNotBlank() },
        snippet = verses.sortedBy { it.vOrder }.take(2).mapNotNull { it.text?.trim() }.joinToString(" / ")
            .ifBlank { poemSummary.orEmpty() },
    )
}
