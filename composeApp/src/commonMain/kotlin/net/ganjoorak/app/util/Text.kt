package net.ganjoorak.app.util

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

fun Int.toPersianDigits(): String = toString().toPersianDigits()

fun String.toPersianDigits(): String = buildString(length) {
    for (c in this@toPersianDigits) append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c)
}

private val TAG = Regex("<[^>]+>")
private val BLOCK_END = Regex("</(p|div|br)\\s*>|<br\\s*/?>", RegexOption.IGNORE_CASE)
private val SPACES = Regex("[ \\t\\u00A0]+")

fun String.stripHtml(): String = replace(BLOCK_END, "\n")
    .replace(TAG, "")
    .replace("&nbsp;", " ")
    .replace("&zwnj;", "‌")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")
    .lines()
    .joinToString("\n") { it.replace(SPACES, " ").trim() }
    .trim()

private val NUMERIC_ENTITY = Regex("&#(x[0-9a-fA-F]+|\\d+);")
private val NAMED_ENTITY = Regex("&([a-zA-Z]+);")
private val NAMED_ENTITIES = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
    "nbsp" to " ", "hellip" to "...", "zwnj" to "‌", "laquo" to "«", "raquo" to "»",
)

fun String.decodeHtmlEntities(): String = this
    .replace(NUMERIC_ENTITY) { match ->
        val code = match.groupValues[1]
        val value = if (code.startsWith("x")) code.drop(1).toIntOrNull(16) else code.toIntOrNull()
        value?.let { codePointToString(it) } ?: match.value
    }
    .replace(NAMED_ENTITY) { NAMED_ENTITIES[it.groupValues[1]] ?: it.value }

private fun codePointToString(codePoint: Int): String = if (codePoint < 0x10000) {
    codePoint.toChar().toString()
} else {
    val offset = codePoint - 0x10000
    charArrayOf(((offset shr 10) + 0xD800).toChar(), ((offset and 0x3FF) + 0xDC00).toChar()).concatToString()
}

private val SING_BLOCK = Regex("<div\\s+class=[\"']sing-block[\"'][\\s\\S]*$", RegexOption.IGNORE_CASE)
private val LINE_BREAK = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val PARAGRAPH_END = Regex("</(p|div)\\s*>", RegexOption.IGNORE_CASE)

/**
 * WordPress post HTML to plain text: one line per `<br>`, a blank line between paragraphs,
 * and the trailing «sing-block» (audio/credits widget) dropped.
 */
fun String.wordpressToText(): String = replace(SING_BLOCK, "")
    .replace(LINE_BREAK, "\n")
    .replace(PARAGRAPH_END, "\n\n")
    .replace(TAG, "")
    .replace("\r", "")
    .decodeHtmlEntities()
    .lines()
    .map { it.replace(SPACES, " ").trim() }
    .joinToString("\n")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

private val DIACRITICS = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

/** Folds Arabic letter variants, diacritics and ZWNJ so Persian text matches loosely. */
fun String.normalizeForSearch(): String = this
    .replace(DIACRITICS, "")
    .replace('ي', 'ی').replace('ى', 'ی').replace('ئ', 'ی')
    .replace('ك', 'ک')
    .replace('ة', 'ه').replace('ۀ', 'ه')
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
    .replace('‌', ' ')
    .replace(SPACES, " ")
    .trim()
    .lowercase()

/** Picks the line of [text] that contains [term], falling back to the first lines. */
fun extractSnippet(text: String, term: String, maxLines: Int = 2): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""
    val needle = term.normalizeForSearch()
    val index = if (needle.isEmpty()) -1 else lines.indexOfFirst { needle in it.normalizeForSearch() }
    if (index < 0) return lines.take(maxLines).joinToString(" / ")
    val start = if (index % 2 == 1) index - 1 else index
    return lines.subList(start, minOf(lines.size, start + maxLines)).joinToString(" / ")
}

fun hijriLifespan(birth: Int?, death: Int?): String? = when {
    birth != null && death != null -> "${birth.toPersianDigits()} – ${death.toPersianDigits()} ه‍.ق"
    death != null -> "درگذشت ${death.toPersianDigits()} ه‍.ق"
    birth != null -> "زادهٔ ${birth.toPersianDigits()} ه‍.ق"
    else -> null
}
