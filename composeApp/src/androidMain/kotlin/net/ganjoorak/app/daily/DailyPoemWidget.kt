package net.ganjoorak.app.daily

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.local.DailyPoem
import net.ganjoorak.shared.R

/**
 * Daily-poem home-screen widget, classic AppWidgetProvider + RemoteViews.
 *
 * Deliberately NOT Glance: the widget only shows static text and opens the
 * poem on tap, which RemoteViews covers with no extra dependency — and it
 * avoids the Glance + WorkManager + R8 startup-crash class of bug (Room
 * database impls stripped by R8) that this project family already hit.
 * Updates come from the daily worker and from the app (after a re-pick);
 * `updatePeriodMillis` stays 0, so the system never wakes the app for it.
 */
class DailyPoemWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateDailyPoemWidgets(context, appWidgetManager)
    }
}

/** Rebuilds every widget instance from the stored poem of the day. */
fun updateDailyPoemWidgets(context: Context, manager: AppWidgetManager = AppWidgetManager.getInstance(context)) {
    val poem = runCatching { AppGraph.instance.dailyPoem.dailyPoem.value }.getOrNull()
    val views = dailyPoemViews(context, poem)
    val ids = manager.getAppWidgetIds(ComponentName(context, DailyPoemWidgetProvider::class.java))
    if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
}

internal fun dailyPoemViews(context: Context, poem: DailyPoem?): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_daily_poem).apply {
        if (poem == null) {
            setTextViewText(R.id.widget_poet, "گنجورک")
            setTextViewText(R.id.widget_lines, "شعر امروز هنوز انتخاب نشده است؛ برنامه را باز کنید.")
        } else {
            setTextViewText(R.id.widget_poet, "${poem.poetName} — ${poem.title}")
            setTextViewText(R.id.widget_lines, poem.lines.joinToString("\n"))
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openPoemIntent(context, poem?.ref),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setOnClickPendingIntent(R.id.widget_root, pending)
    }

/** [net.ganjoorak.app.platform.refreshDailyPoemWidget] is implemented in `platform/DailyPoemPlatform.android.kt`. */
