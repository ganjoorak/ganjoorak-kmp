package net.ganjoorak.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import net.ganjoorak.app.data.offline.ByteStore
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

actual fun httpEngine(): HttpClientEngineFactory<*> = OkHttp

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

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual val usesSideNavigation: Boolean
    get() = System.getProperty("ganjoorak.nav") != "bar" && System.getenv("GANJOORAK_NAV") != "bar"

/** Durable user-data dir: %APPDATA%/ganjoorak/offline on Windows, ~/.local/share/ganjoorak/offline elsewhere. */
actual fun offlineByteStore(): ByteStore {
    val windows = System.getProperty("os.name")?.startsWith("Windows") == true
    val dir = if (windows) {
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "ganjoorak/offline")
    } else {
        File(System.getProperty("user.home"), ".local/share/ganjoorak/offline")
    }
    return FileByteStore(dir)
}

/** Blocking file-backed [ByteStore]; offline content is small JSON files. */
private class FileByteStore(private val root: File) : ByteStore {
    init { root.mkdirs() }
    private fun file(path: String) = File(root, path)

    override suspend fun write(path: String, bytes: ByteArray) {
        val f = file(path)
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
    }

    override suspend fun read(path: String): ByteArray? = file(path).takeIf { it.isFile }?.readBytes()

    override suspend fun delete(path: String): Boolean = file(path).delete()

    override suspend fun list(prefix: String): List<String> =
        root.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filter { it.startsWith(prefix) }
            .toList()

    override suspend fun exists(path: String): Boolean = file(path).isFile

    override suspend fun length(path: String): Long? = file(path).takeIf { it.isFile }?.length()
}
