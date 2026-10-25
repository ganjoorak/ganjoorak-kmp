package net.ganjoorak.app

import com.russhwolf.settings.Settings
import kotlinx.coroutines.MainScope
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.local.LibraryStore
import net.ganjoorak.app.data.local.SettingsStore
import net.ganjoorak.app.data.remote.GanjoorApi
import net.ganjoorak.app.data.remote.createHttpClient
import net.ganjoorak.app.platform.createAudioPlayer
import net.ganjoorak.app.player.PlayerController

const val APP_VERSION = "1.0.0"

/** Hand-wired app singletons; small enough not to need a DI framework. */
class AppGraph {
    private val scope = MainScope()
    private val storage = Settings()

    val repository = PoemRepository(GanjoorApi(createHttpClient()))
    val settings = SettingsStore(storage)
    val library = LibraryStore(storage)
    val player by lazy { PlayerController(createAudioPlayer(), repository, scope) }

    companion object {
        val instance: AppGraph by lazy { AppGraph() }
    }
}
