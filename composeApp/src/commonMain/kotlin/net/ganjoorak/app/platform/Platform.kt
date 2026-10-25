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

/** PNG bytes of [image]. */
expect fun encodePng(image: androidx.compose.ui.graphics.ImageBitmap): ByteArray

/**
 * Hands a PNG to the system: the share sheet on Android, a save dialog on desktop.
 * Returns a message to show the user, or null when the system UI takes over.
 */
expect fun shareImage(png: ByteArray, fileName: String): String?
