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
