package net.ganjoorak.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.SearchHit
import net.ganjoorak.app.data.local.LibraryStore

enum class SearchMode { Text, Semantic }

data class SearchState(
    val query: String = "",
    val mode: SearchMode = SearchMode.Text,
    val hits: List<SearchHit> = emptyList(),
    val total: Int? = null,
    val page: Int = 0,
    val hasNext: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
)

class SearchViewModel(
    private val repository: PoemRepository,
    private val library: LibraryStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private val requests = MutableStateFlow(Request("", SearchMode.Text))
    private var moreJob: Job? = null

    private data class Request(val query: String, val mode: SearchMode)

    init {
        viewModelScope.launch {
            requests
                .debounce { if (it.query.isBlank()) 0 else DEBOUNCE_MS }
                .distinctUntilChanged()
                .collectLatest { search(it) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        requests.value = Request(query.trim(), _state.value.mode)
    }

    fun onModeChange(mode: SearchMode) {
        _state.update { it.copy(mode = mode) }
        requests.value = Request(_state.value.query.trim(), mode)
    }

    fun saveRecent(query: String = _state.value.query) = library.addRecentSearch(query)

    fun retry() {
        val request = requests.value
        viewModelScope.launch { search(request) }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasNext || current.loading || current.loadingMore || moreJob?.isActive == true) return
        val request = requests.value
        moreJob = viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val page = repository.search(request.query, current.page + 1)
                if (requests.value != request) return@launch
                _state.update { state ->
                    val known = state.hits.mapTo(HashSet()) { it.ref }
                    state.copy(
                        hits = state.hits + page.hits.filter { it.ref !in known },
                        page = page.page,
                        hasNext = page.hasNext && page.hits.isNotEmpty(),
                        loadingMore = false,
                    )
                }
            } catch (e: GanjoorException) {
                _state.update { it.copy(loadingMore = false, hasNext = false) }
            }
        }
    }

    private suspend fun search(request: Request) {
        moreJob?.cancel()
        if (request.query.length < MIN_QUERY) {
            _state.update { it.copy(hits = emptyList(), total = null, page = 0, hasNext = false, loading = false, error = null, searched = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val page = when (request.mode) {
                SearchMode.Text -> repository.search(request.query, 1)
                SearchMode.Semantic -> repository.semanticSearch(request.query)
            }
            _state.update {
                it.copy(hits = page.hits, total = page.total, page = 1, hasNext = page.hasNext, loading = false, searched = true)
            }
        } catch (e: GanjoorException) {
            _state.update { it.copy(hits = emptyList(), loading = false, error = e.message, searched = true) }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 450L
        const val MIN_QUERY = 2
    }
}
