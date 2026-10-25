package net.ganjoorak.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.local.SettingsStore

data class FeedState(
    val poems: List<Poem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val poetId: Int? = null,
    val poetName: String? = null,
    /** Bumped whenever the feed restarts so the pager can jump back to the top. */
    val generation: Int = 0,
)

/** Endless feed of random poems, optionally limited to one poet, loaded a few pages ahead. */
class FeedViewModel(
    private val repository: PoemRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()
    private var job: Job? = null
    private var visibleIndex = 0

    init {
        viewModelScope.launch {
            settings.settings
                .map { it.feedPoetId to it.feedPoetName }
                .distinctUntilChanged()
                .collect { (id, name) -> restart(id, name) }
        }
    }

    fun onPageVisible(index: Int) {
        visibleIndex = index
        ensureLoaded()
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        ensureLoaded()
    }

    fun refresh() = restart(_state.value.poetId, _state.value.poetName)

    fun setPoet(poet: Poet?) = settings.update { it.copy(feedPoetId = poet?.id, feedPoetName = poet?.name) }

    private fun restart(poetId: Int?, poetName: String?) {
        job?.cancel()
        visibleIndex = 0
        _state.update { FeedState(poetId = poetId, poetName = poetName, generation = it.generation + 1) }
        ensureLoaded()
    }

    private fun ensureLoaded() {
        if (job?.isActive == true || _state.value.error != null) return
        if (_state.value.poems.size > visibleIndex + PREFETCH) return
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            var duplicates = 0
            try {
                while (_state.value.poems.size <= visibleIndex + PREFETCH && duplicates < MAX_DUPLICATES) {
                    val poem = repository.randomPoem(_state.value.poetId)
                    if (_state.value.poems.any { it.id == poem.id }) {
                        duplicates++
                    } else {
                        _state.update { it.copy(poems = it.poems + poem) }
                    }
                }
                _state.update { it.copy(loading = false) }
            } catch (e: GanjoorException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private companion object {
        const val PREFETCH = 2
        const val MAX_DUPLICATES = 6
    }
}
