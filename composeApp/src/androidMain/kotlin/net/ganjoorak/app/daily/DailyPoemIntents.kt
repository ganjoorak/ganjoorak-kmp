package net.ganjoorak.app.daily

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import net.ganjoorak.app.data.PoemRef

/** Intent extras carrying the poem a widget / notification tap should open. */
const val EXTRA_POEM_ID = "net.ganjoorak.app.daily.POEM_ID"
const val EXTRA_POEM_SOURCE = "net.ganjoorak.app.daily.POEM_SOURCE"
const val EXTRA_POEM_POET_SLUG = "net.ganjoorak.app.daily.POEM_POET_SLUG"

/** MainActivity lives in the androidApp module, which this library cannot see — refer to it by name. */
const val MAIN_ACTIVITY_CLASS = "net.ganjoorak.app.MainActivity"

/** Opens [ref] in the app (cold start or on top of a running app). */
fun openPoemIntent(context: Context, ref: PoemRef?): Intent =
    Intent().apply {
        component = ComponentName(context.packageName, MAIN_ACTIVITY_CLASS)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        ref?.let {
            putExtra(EXTRA_POEM_ID, it.id)
            putExtra(EXTRA_POEM_SOURCE, it.source.name)
            putExtra(EXTRA_POEM_POET_SLUG, it.poetSlug)
        }
    }

/** Reads a poem ref back out of an intent built by [openPoemIntent]; null when absent. */
fun Intent.poemRefOrNull(): PoemRef? {
    val id = getIntExtra(EXTRA_POEM_ID, -1)
    if (id <= 0) return null
    val source = getStringExtra(EXTRA_POEM_SOURCE)
        ?.let { runCatching { net.ganjoorak.app.data.PoetSource.valueOf(it) }.getOrNull() }
        ?: net.ganjoorak.app.data.PoetSource.Ganjoor
    return PoemRef(source, id, getStringExtra(EXTRA_POEM_POET_SLUG).orEmpty())
}
