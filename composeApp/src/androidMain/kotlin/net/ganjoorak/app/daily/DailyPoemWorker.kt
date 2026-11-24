package net.ganjoorak.app.daily

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.DailyPoemPicker
import net.ganjoorak.app.data.local.AppSettings
import java.util.concurrent.TimeUnit

/**
 * Picks the poem of the day (once), pushes it to the widget, posts the local
 * notification when enabled, then chains the next run at the next configured
 * time.
 *
 * A self-perpetuating one-time chain — not a PeriodicWorkRequest — so the run
 * stays anchored to the configured time (09:00 Tehran by default) instead of
 * drifting a little later every day under Doze. Exact-to-the-minute timing
 * is not required for a daily poem, so no exact alarm (and no
 * SCHEDULE_EXACT_ALARM permission) is used; WorkManager may run minutes late
 * after Doze, which is fine here.
 */
class DailyPoemWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val graph = AppGraph.instance
        val picked = DailyPoemPicker.refreshIfNeeded(
            fetchRandom = { key -> graph.repository.randomPoem(key) },
            settings = graph.settings.settings.value,
            stored = graph.dailyPoem.dailyPoem.value,
            onPicked = graph.dailyPoem::set,
            nowMillis = System.currentTimeMillis(),
        )
        updateDailyPoemWidgets(applicationContext)
        val settings = graph.settings.settings.value
        if (settings.dailyNotificationEnabled && picked != null) {
            DailyPoemNotifier.post(applicationContext, picked)
        }
        DailyPoemScheduler.enqueueNext(applicationContext, settings, ExistingWorkPolicy.REPLACE)
        return Result.success()
    }
}

object DailyPoemScheduler {
    const val UNIQUE_NAME = "daily-poem"
    private const val TAG = "daily-poem"

    /** Enqueues the next run at the next configured daily time. */
    fun enqueueNext(context: Context, settings: AppSettings, policy: ExistingWorkPolicy) {
        val delay = DailyPoemPicker.millisUntilNextDaily(
            settings.dailyNotificationHour,
            settings.dailyNotificationMinute,
            System.currentTimeMillis(),
        )
        val request = OneTimeWorkRequestBuilder<DailyPoemWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, policy, request)
    }

    /** First run (app start): keep an already-scheduled chain, don't reset it. */
    fun ensureScheduled(context: Context) {
        enqueueNext(context, AppGraph.instance.settings.settings.value, ExistingWorkPolicy.KEEP)
    }
}
