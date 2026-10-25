package net.ganjoorak.app.ui.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.GanjoorException

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
}

/** A ViewModel that runs one suspending load and exposes its result; enough for every detail screen. */
class LoaderViewModel<T>(private val loader: suspend () -> T) : ViewModel() {
    private val _state = MutableStateFlow<Loadable<T>>(Loadable.Loading)
    val state: StateFlow<Loadable<T>> = _state.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        _state.value = Loadable.Loading
        job = viewModelScope.launch {
            _state.value = try {
                Loadable.Ready(loader())
            } catch (e: GanjoorException) {
                Loadable.Error(e.message ?: "خطا")
            }
        }
    }
}

@Composable
fun <T> rememberLoader(key: String, loader: suspend () -> T): LoaderViewModel<T> =
    viewModel(key = key) { LoaderViewModel(loader) }
