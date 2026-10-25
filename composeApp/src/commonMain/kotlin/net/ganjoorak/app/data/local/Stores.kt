package net.ganjoorak.app.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.data.remote.GanjoorJson

enum class ThemeMode { System, Light, Dark, Paper }

enum class PoemFont { Vazirmatn, Samim, Shabnam, Gandom }

enum class CoupletLayout { Auto, Stacked, SideBySide }

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val poemFont: PoemFont = PoemFont.Vazirmatn,
    val fontScale: Float = 1f,
    val coupletLayout: CoupletLayout = CoupletLayout.Auto,
    val showVerseNumbers: Boolean = false,
    val autoScrollWithAudio: Boolean = true,
    val feedPoetId: Int? = null,
    val feedPoetName: String? = null,
)

/** A JSON value persisted in [Settings] and mirrored as a [StateFlow]. */
private class PersistedValue<T>(
    private val settings: Settings,
    private val key: String,
    private val serializer: KSerializer<T>,
    default: T,
) {
    private val state = MutableStateFlow(
        settings.getStringOrNull(key)
            ?.let { runCatching { GanjoorJson.decodeFromString(serializer, it) }.getOrNull() }
            ?: default,
    )
    val flow: StateFlow<T> = state.asStateFlow()

    fun update(transform: (T) -> T) {
        state.update(transform)
        settings.putString(key, GanjoorJson.encodeToString(serializer, state.value))
    }
}

class SettingsStore(settings: Settings) {
    private val value = PersistedValue(settings, "app_settings", AppSettings.serializer(), AppSettings())
    val settings: StateFlow<AppSettings> = value.flow

    fun update(transform: (AppSettings) -> AppSettings) = value.update(transform)
}

class LibraryStore(settings: Settings) {
    private val saved = PersistedValue(settings, "saved_poems", ListSerializer(SavedPoem.serializer()), emptyList())
    private val recent = PersistedValue(settings, "recent_searches", ListSerializer(String.serializer()), emptyList())

    val savedPoems: StateFlow<List<SavedPoem>> = saved.flow
    val recentSearches: StateFlow<List<String>> = recent.flow

    fun isSaved(id: Int): Boolean = saved.flow.value.any { it.id == id }

    fun toggleSaved(poem: Poem) {
        saved.update { list ->
            if (list.any { it.id == poem.id }) {
                list.filterNot { it.id == poem.id }
            } else {
                listOf(SavedPoem(poem.id, poem.title, poem.poetName, poem.excerpt)) + list
            }
        }
    }

    fun remove(id: Int) = saved.update { list -> list.filterNot { it.id == id } }

    fun addRecentSearch(term: String) {
        val clean = term.trim()
        if (clean.length < 2) return
        recent.update { (listOf(clean) + it.filterNot { old -> old == clean }).take(MAX_RECENT) }
    }

    fun clearRecentSearches() = recent.update { emptyList() }

    private companion object {
        const val MAX_RECENT = 12
    }
}
