package net.ganjoorak.app.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import net.ganjoorak.app.data.offline.ByteStore
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Application context, set once from `GanjoorakApplication.onCreate`. */
object AndroidPlatform {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}

private class ExoAudioPlayer(context: Context) : AudioPlayer {
    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    override val isSupported = true
    override val isPlaying get() = player.isPlaying
    override val isBuffering get() = player.playbackState == Player.STATE_BUFFERING
    override val isEnded get() = player.playbackState == Player.STATE_ENDED
    override val positionMs get() = player.currentPosition
    override val durationMs get() = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    override fun load(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}

actual fun createAudioPlayer(): AudioPlayer = ExoAudioPlayer(AndroidPlatform.context)

actual fun httpEngine(): HttpClientEngineFactory<*> = OkHttp

actual fun shareText(text: String, title: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    val chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AndroidPlatform.context.startActivity(chooser)
    return true
}

actual fun copyText(text: String) {
    val clipboard = AndroidPlatform.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("poem", text))
}

actual fun encodePng(image: ImageBitmap): ByteArray =
    ByteArrayOutputStream().also { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

actual fun shareImage(png: ByteArray, fileName: String): String? {
    val context = AndroidPlatform.context
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeBytes(png) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(chooser)
    return null
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual val usesSideNavigation: Boolean = false

/** Durable across app updates, unlike cacheDir. */
actual fun offlineByteStore(): ByteStore =
    FileByteStore(File(AndroidPlatform.context.filesDir, "offline"))

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
