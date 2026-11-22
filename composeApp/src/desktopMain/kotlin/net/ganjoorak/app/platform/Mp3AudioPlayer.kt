package net.ganjoorak.app.platform

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import kotlin.concurrent.thread

/**
 * In-app desktop player used on Windows and Linux. MP3s are fetched over HTTP
 * and decoded with JLayer/mp3spi into a Java Sound [Clip], which gives the
 * MiniPlayer play/pause, seek, duration, and verse-follow position updates
 * without a preinstalled VLC or JavaFX runtime.
 */
internal class Mp3AudioPlayer : AudioPlayer {
    private val lock = Any()
    private var clip: Clip? = null
    private val generation = AtomicInteger(0)
    private val buffering = AtomicBoolean(false)
    private val ended = AtomicBoolean(false)
    private val playWhenReady = AtomicBoolean(false)
    private val durationMicros = AtomicLong(0)
    private val lastError = AtomicReference<String?>(null)

    override val isSupported: Boolean = true
    override val isPlaying: Boolean
        get() = synchronized(lock) { clip?.isRunning == true }
    override val isBuffering: Boolean
        get() = buffering.get()
    override val isEnded: Boolean
        get() = ended.get()
    override val positionMs: Long
        get() = synchronized(lock) { (clip?.microsecondPosition ?: 0L) / 1000 }
    override val durationMs: Long
        get() = durationMicros.get() / 1000

    override fun load(url: String) {
        val gen = generation.incrementAndGet()
        playWhenReady.set(false)
        ended.set(false)
        buffering.set(true)
        durationMicros.set(0)
        lastError.set(null)
        synchronized(lock) { closeClip() }
        thread(name = "ganjoorak-audio-load", isDaemon = true) {
            try {
                val bytes = download(url)
                if (gen != generation.get()) return@thread
                val newClip = openClip(bytes)
                synchronized(lock) {
                    if (gen != generation.get()) {
                        newClip.close()
                        return@thread
                    }
                    clip = newClip
                    durationMicros.set(newClip.microsecondLength.coerceAtLeast(0))
                    buffering.set(false)
                    if (playWhenReady.get()) newClip.start()
                }
            } catch (e: Exception) {
                if (gen == generation.get()) {
                    buffering.set(false)
                    lastError.set(e.message)
                    println("Ganjoorak: desktop audio failed: $e")
                }
            }
        }
    }

    override fun play() {
        playWhenReady.set(true)
        ended.set(false)
        synchronized(lock) { clip?.start() }
    }

    override fun pause() {
        playWhenReady.set(false)
        synchronized(lock) { clip?.stop() }
    }

    override fun seekTo(positionMs: Long) {
        val micros = positionMs.coerceAtLeast(0) * 1000
        synchronized(lock) {
            val current = clip ?: return
            val length = current.microsecondLength
            current.microsecondPosition = if (length > 0) micros.coerceAtMost(length) else micros
            ended.set(length > 0 && current.microsecondPosition >= length - 20_000)
        }
    }

    override fun stop() {
        generation.incrementAndGet()
        playWhenReady.set(false)
        buffering.set(false)
        ended.set(false)
        durationMicros.set(0)
        synchronized(lock) { closeClip() }
    }

    private fun closeClip() {
        clip?.let { running ->
            runCatching { running.stop() }
            runCatching { running.close() }
        }
        clip = null
    }

    private fun openClip(bytes: ByteArray): Clip {
        val mp3 = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
        val base = mp3.format
        val pcm = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            if (base.sampleRate > 0) base.sampleRate else 44100f,
            16,
            if (base.channels > 0) base.channels else 2,
            (if (base.channels > 0) base.channels else 2) * 2,
            if (base.sampleRate > 0) base.sampleRate else 44100f,
            false,
        )
        val decoded = AudioSystem.getAudioInputStream(pcm, mp3)
        val info = DataLine.Info(Clip::class.java, pcm)
        val newClip = (AudioSystem.getLine(info) as Clip)
        newClip.open(decoded)
        newClip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                val atEnd = synchronized(lock) {
                    val length = clip?.microsecondLength ?: 0
                    length > 0 && (clip?.microsecondPosition ?: 0) >= length - 20_000
                }
                if (atEnd) ended.set(true)
            }
        }
        decoded.close()
        mp3.close()
        return newClip
    }

    private fun download(url: String): ByteArray {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "Ganjoorak")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        init {
            // Make the mp3spi AudioFileReader visible to Java Sound on a fat classpath.
            runCatching { Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader") }
        }
    }
}
