package net.ganjoorak.app.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.Recitation
import net.ganjoorak.app.data.VerseTiming
import net.ganjoorak.app.platform.AudioPlayer

data class PlayerState(
    val poemId: Int? = null,
    val poemTitle: String = "",
    val poetName: String = "",
    val recitation: Recitation? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val activeVerseOrder: Int? = null,
) {
    val isActive: Boolean get() = recitation != null
}

/** Finds the verse being recited at [positionMs], given timings sorted by start. */
fun activeVerseAt(timings: List<VerseTiming>, positionMs: Long): Int? {
    if (timings.isEmpty() || positionMs < timings.first().startMs) return null
    var low = 0
    var high = timings.lastIndex
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (timings[mid].startMs <= positionMs) low = mid else high = mid - 1
    }
    return timings[low].verseOrder.takeIf { it > 0 }
}

/** App-wide recitation player shared by the feed, poem screen and mini player. Main thread only. */
class PlayerController(
    private val player: AudioPlayer,
    private val repository: PoemRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    val isSupported: Boolean get() = player.isSupported

    private var timings: List<VerseTiming> = emptyList()
    private var ticker: Job? = null
    private var timingJob: Job? = null

    fun play(poem: Poem, recitation: Recitation = poem.recitations.first()) {
        if (!player.isSupported) {
            player.load(recitation.mp3Url)
            return
        }
        val current = _state.value
        if (current.poemId == poem.id && current.recitation?.id == recitation.id) {
            if (!current.isPlaying) resume()
            return
        }
        timings = emptyList()
        timingJob?.cancel()
        if (recitation.inSync) {
            timingJob = scope.launch {
                timings = runCatching { repository.timings(recitation.id) }.getOrDefault(emptyList())
            }
        }
        player.load(recitation.mp3Url)
        player.play()
        _state.value = PlayerState(
            poemId = poem.id,
            poemTitle = poem.title,
            poetName = poem.poetName,
            recitation = recitation,
            isPlaying = true,
            isBuffering = true,
        )
        startTicker()
    }

    fun toggle() {
        if (_state.value.isPlaying) pause() else resume()
    }

    fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        if (!_state.value.isActive) return
        if (player.isEnded) player.seekTo(0)
        player.play()
        _state.update { it.copy(isPlaying = true) }
        startTicker()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs, activeVerseOrder = activeVerseAt(timings, positionMs)) }
    }

    /** Jumps to the moment a verse is recited; does nothing when timings are unknown. */
    fun seekToVerse(verseOrder: Int) {
        val timing = timings.firstOrNull { it.verseOrder == verseOrder } ?: return
        seekTo(timing.startMs)
        if (!_state.value.isPlaying) resume()
    }

    fun stop() {
        ticker?.cancel()
        timingJob?.cancel()
        player.stop()
        timings = emptyList()
        _state.value = PlayerState()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val position = player.positionMs
                val ended = player.isEnded
                _state.update {
                    it.copy(
                        isPlaying = player.isPlaying || (it.isPlaying && !ended),
                        isBuffering = player.isBuffering,
                        positionMs = position,
                        durationMs = player.durationMs.coerceAtLeast(0),
                        activeVerseOrder = if (ended) null else activeVerseAt(timings, position),
                    )
                }
                if (ended) {
                    _state.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
                    break
                }
                delay(200)
            }
        }
    }
}
