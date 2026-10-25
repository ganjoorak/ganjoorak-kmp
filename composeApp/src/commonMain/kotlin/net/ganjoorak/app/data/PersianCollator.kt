package net.ganjoorak.app.data

private const val PERSIAN_ALPHABET = "آاأإبپتثجچحخدذرزژسشصضطظعغفقکكگلمنوؤهةیيئ"

/** Orders strings by the Persian alphabet (code-point order misplaces پ چ ژ گ and Arabic variants). */
object PersianCollator : Comparator<String> {
    private fun rank(c: Char): Int {
        if (c == ' ' || c == '‌') return -1
        val index = PERSIAN_ALPHABET.indexOf(c)
        return if (index >= 0) index else PERSIAN_ALPHABET.length + c.code
    }

    override fun compare(a: String, b: String): Int {
        val length = minOf(a.length, b.length)
        for (i in 0 until length) {
            val diff = rank(a[i]) - rank(b[i])
            if (diff != 0) return diff
        }
        return a.length - b.length
    }
}
