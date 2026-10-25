package net.ganjoorak.app

import android.app.Application
import net.ganjoorak.app.platform.AndroidPlatform

class GanjoorakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.init(this)
    }
}
