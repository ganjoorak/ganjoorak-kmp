package net.ganjoorak.app.platform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/** The JVM has no built-in MP3 decoder, so desktop hands recitations to the default browser. */
private class BrowserAudioPlayer : AudioPlayer {
    override val isSupported = false
    override val isPlaying = false
    override val isBuffering = false
    override val isEnded = false
    override val positionMs = 0L
    override val durationMs = 0L

    override fun load(url: String) {
        runCatching { Desktop.getDesktop().browse(URI(url)) }
    }

    override fun play() = Unit
    override fun pause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun stop() = Unit
}

actual fun createAudioPlayer(): AudioPlayer = BrowserAudioPlayer()

actual fun shareText(text: String, title: String): Boolean {
    copyText(text)
    return false
}

actual fun copyText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
