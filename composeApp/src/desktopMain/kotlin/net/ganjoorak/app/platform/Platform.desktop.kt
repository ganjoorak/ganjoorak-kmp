package net.ganjoorak.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
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

actual fun encodePng(image: ImageBitmap): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)

actual fun shareImage(png: ByteArray, fileName: String): String? {
    val dialog = FileDialog(null as Frame?, "ذخیرهٔ تصویر شعر", FileDialog.SAVE).apply {
        file = fileName
        isVisible = true
    }
    val name = dialog.file ?: return "ذخیره لغو شد"
    val target = File(dialog.directory, if (name.endsWith(".png")) name else "$name.png")
    target.writeBytes(png)
    return "تصویر ذخیره شد: ${target.absolutePath}"
}
