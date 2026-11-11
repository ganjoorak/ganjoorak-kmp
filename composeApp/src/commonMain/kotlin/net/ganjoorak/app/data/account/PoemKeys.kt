package net.ganjoorak.app.data.account

import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource

/**
 * The website's library key for a poem: `ganjoor|custom|echolalia` + poet slug + id.
 * The app's local collections are the site's `custom` source.
 */
fun Poem.libraryKey(): String = poemLibraryKey(ref)

fun poemLibraryKey(ref: PoemRef): String {
    val source = when (ref.source) {
        PoetSource.Ganjoor -> "ganjoor"
        PoetSource.Echolalia -> "echolalia"
        PoetSource.Local -> "custom"
    }
    val slug = ref.poetSlug.ifBlank { "unknown" }
    return "$source:$slug:${ref.id}"
}

fun parsePoemLibraryKey(key: String): PoemRef? {
    val parts = key.split(':')
    if (parts.size < 3) return null
    val source = when (parts[0]) {
        "ganjoor" -> PoetSource.Ganjoor
        "echolalia" -> PoetSource.Echolalia
        "custom" -> PoetSource.Local
        else -> return null
    }
    val id = parts.last().toIntOrNull() ?: return null
    val slug = parts.drop(1).dropLast(1).joinToString(":")
    if (slug.isEmpty() || id < 1) return null
    return PoemRef(source, id, slug)
}

/** Resolves a quote to a [PoemRef], preferring the library key and falling back to the site href. */
fun Quote.poemRef(): PoemRef? = parsePoemLibraryKey(key) ?: parseQuoteHref(href)

fun parseQuoteHref(href: String): PoemRef? {
    val path = href.substringBefore('?').trim().trim('/')
    if (path.isEmpty()) return null
    val parts = path.split('/')
    return when {
        parts.size >= 2 && parts[0] == "poem" -> parts[1].toIntOrNull()?.let { PoemRef(PoetSource.Ganjoor, it) }
        parts.size >= 2 -> parts[1].toIntOrNull()?.let { PoemRef(PoetSource.Local, it, parts[0]) }
        else -> null
    }
}

/** Visible lines of a poem, matching the website's `poemToLines` (trimmed, empty dropped). */
fun Poem.quoteLines(): List<String> = verses.map { it.text.trim() }.filter { it.isNotEmpty() }

/** 0-based index of [verse] in [quoteLines], or null if that verse is blank. */
fun Poem.quoteLineIndex(verse: net.ganjoorak.app.data.Verse): Int? {
    var index = -1
    for (item in verses) {
        if (item.text.trim().isEmpty()) continue
        index++
        if (item.order == verse.order) return index
    }
    return null
}
