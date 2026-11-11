package net.ganjoorak.app.data.account

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.local.LibraryStore
import net.ganjoorak.app.data.local.SettingsStore
import net.ganjoorak.app.data.remote.GanjoorJson
import net.ganjoorak.app.platform.currentTimeMillis

const val ACCOUNT_SYNC_DEBOUNCE_MS = 3_000L
internal const val ACCOUNT_SYNC_EXTRAS_KEY = "account_sync_extras"

/**
 * Keeps the device library and preferred poets in step with the website account.
 *
 * On sign-in (and when a session already exists at start) this PUTs the local
 * data with merge=true, then replaces the device lists with the sanitized union
 * the server returns. After that, changes are pushed with merge=false after a
 * 3s debounce, the same way the site's AccountSync does. A 401 is ignored.
 */
class AccountSync(
    private val accounts: AccountRepository,
    private val library: LibraryStore,
    private val settingsStore: SettingsStore,
    private val storage: Settings,
    private val poets: suspend () -> List<Poet>,
    private val scope: CoroutineScope,
    private val now: () -> Long = { currentTimeMillis() },
    private val debounceMs: Long = ACCOUNT_SYNC_DEBOUNCE_MS,
) {
    private var isSynced = false
    private var extras = readExtras(storage)
    private var pushJob: Job? = null

    init {
        scope.launch {
            accounts.session
                .map { it.user?.username }
                .distinctUntilChanged()
                .collect { username ->
                    pushJob?.cancel()
                    isSynced = false
                    if (username == null) return@collect
                    mergeFromAccount(username)
                }
        }
        scope.launch {
            combine(
                library.savedPoems,
                library.history,
                settingsStore.settings.map { it.followedPoets },
            ) { _, _, _ -> }
                .collect { schedulePush() }
        }
    }

    private suspend fun mergeFromAccount(username: String) {
        val directory = loadDirectory()
        try {
            val merged = accounts.api.saveAccountData(localData(directory, username), merge = true)
            applyMerged(merged, directory, username)
            isSynced = true
        } catch (e: AccountException) {
            if (e.status == 401) return
            // Leave unsynced so a later replace-push cannot wipe the account after a failed union.
        }
    }

    private fun schedulePush() {
        if (!isSynced || accounts.session.value.user == null) return
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(debounceMs)
            pushReplacement()
        }
    }

    private suspend fun pushReplacement() {
        val username = accounts.session.value.user?.username ?: return
        val directory = loadDirectory()
        try {
            val saved = accounts.api.saveAccountData(localData(directory, username), merge = false)
            applyMerged(saved, directory, username)
        } catch (e: AccountException) {
            if (e.status == 401) isSynced = false
        }
    }

    private fun applyMerged(data: AccountDataDto, directory: PoetDirectory, username: String) {
        val applied = applyAccountData(data, directory, username)
        extras = applied.extras
        writeExtras(storage, extras)
        library.replace(applied.favorites, applied.history)
        settingsStore.replaceFollowedPoets(applied.followed)
    }

    private fun localData(directory: PoetDirectory, username: String): AccountDataDto {
        val carried = extras.takeIf { it.username == username } ?: AccountSyncExtras(username = username)
        return buildAccountData(
            favorites = library.savedPoems.value,
            history = library.history.value,
            followed = settingsStore.settings.value.followedPoets,
            extras = carried,
            directory = directory,
            now = now(),
        )
    }

    private suspend fun loadDirectory(): PoetDirectory =
        PoetDirectory(runCatching { poets() }.getOrDefault(emptyList()))
}

internal fun readExtras(settings: Settings): AccountSyncExtras {
    val raw = settings.getStringOrNull(ACCOUNT_SYNC_EXTRAS_KEY) ?: return AccountSyncExtras()
    return runCatching { GanjoorJson.decodeFromString(AccountSyncExtras.serializer(), raw) }
        .getOrDefault(AccountSyncExtras())
}

internal fun writeExtras(settings: Settings, extras: AccountSyncExtras) {
    settings.putString(ACCOUNT_SYNC_EXTRAS_KEY, GanjoorJson.encodeToString(AccountSyncExtras.serializer(), extras))
}
