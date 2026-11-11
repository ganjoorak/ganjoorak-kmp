package net.ganjoorak.app

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.coroutines.MainScope
import net.ganjoorak.app.data.EcholaliaSource
import net.ganjoorak.app.data.LocalSource
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.account.ACCOUNT_API_BASE_URL
import net.ganjoorak.app.data.account.AccountApi
import net.ganjoorak.app.data.account.AccountRepository
import net.ganjoorak.app.data.account.StoriesRepository
import net.ganjoorak.app.data.account.createAccountHttpClient
import net.ganjoorak.app.data.local.LibraryStore
import net.ganjoorak.app.data.local.SettingsStore
import net.ganjoorak.app.data.remote.EcholaliaApi
import net.ganjoorak.app.data.remote.GanjoorApi
import net.ganjoorak.app.data.remote.createHttpClient
import net.ganjoorak.app.platform.createAudioPlayer
import net.ganjoorak.app.platform.httpEngine
import net.ganjoorak.app.player.PlayerController
import net.ganjoorak.app.resources.Res

const val APP_VERSION = "1.0.0"

/** Hand-wired app singletons; small enough not to need a DI framework. */
class AppGraph {
    private val scope = MainScope()
    private val storage = Settings()
    private val http = createHttpClient(httpEngine())

    /** Plain client for Coil: no JSON defaults, same explicit engine. */
    val imageHttp: HttpClient by lazy { HttpClient(httpEngine()) }

    val repository = PoemRepository(
        api = GanjoorApi(http),
        echolalia = EcholaliaSource(EcholaliaApi(http)),
        local = LocalSource { slug -> Res.readBytes("files/poems/$slug.json").decodeToString() },
    )
    val settings = SettingsStore(storage)
    val library = LibraryStore(storage)
    val accounts = AccountRepository(
        settings = storage,
        apiFactory = { token, onCookie ->
            AccountApi(createAccountHttpClient(httpEngine()), token, onCookie, baseUrl = accountApiBaseUrl)
        },
        scope = scope,
    )
    val stories = StoriesRepository(accounts, storage, scope)
    val player by lazy { PlayerController(createAudioPlayer(), repository, scope) }

    companion object {
        /**
         * Public accounts API the website uses. Desktop QA can point this at a local
         * fake before [instance] is first read, via the ganjoorak.accountApi property
         * or the GANJOORAK_ACCOUNT_API environment variable.
         */
        var accountApiBaseUrl: String = ACCOUNT_API_BASE_URL
        val instance: AppGraph by lazy { AppGraph() }
    }
}
