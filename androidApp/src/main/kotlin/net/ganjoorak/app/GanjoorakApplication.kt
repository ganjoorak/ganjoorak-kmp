package net.ganjoorak.app

import android.app.Application
import net.ganjoorak.app.daily.DailyPoemNotifier
import net.ganjoorak.app.daily.DailyPoemScheduler
import net.ganjoorak.app.platform.AndroidPlatform

class GanjoorakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.init(this)
        DailyPoemNotifier.createChannel(this)
        // Scheduling must never take the app down with it; WorkManager is only for the widget.
        runCatching { DailyPoemScheduler.ensureScheduled(this) }
    }
}
