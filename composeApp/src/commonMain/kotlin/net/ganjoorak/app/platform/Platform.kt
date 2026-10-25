package net.ganjoorak.app.platform

/** Minimal audio playback surface implemented per platform. */
interface AudioPlayer {
    /** False where in-app playback isn't available; [load] then hands the URL to the system. */
    val isSupported: Boolean
    val isPlaying: Boolean
    val isBuffering: Boolean
    val isEnded: Boolean
    val positionMs: Long
    val durationMs: Long

    fun load(url: String)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
}

expect fun createAudioPlayer(): AudioPlayer

/** Shares [text] through the system sheet. Returns false when it was copied to the clipboard instead. */
expect fun shareText(text: String, title: String): Boolean

expect fun copyText(text: String)
