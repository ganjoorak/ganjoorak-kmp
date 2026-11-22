package net.ganjoorak.app.data.offline

import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.PoemRef

/** Durable named byte storage; paths are `/`-separated relative paths such as `poems/x.json`. */
interface ByteStore {
    suspend fun write(path: String, bytes: ByteArray)
    suspend fun read(path: String): ByteArray?
    suspend fun delete(path: String): Boolean
    suspend fun list(prefix: String): List<String>
    suspend fun exists(path: String): Boolean
    suspend fun length(path: String): Long?
}

/** In-memory implementation for tests. */
class MemoryByteStore : ByteStore {
    private val files = mutableMapOf<String, ByteArray>()

    override suspend fun write(path: String, bytes: ByteArray) {
        files[path] = bytes
    }

    override suspend fun read(path: String): ByteArray? = files[path]

    override suspend fun delete(path: String): Boolean = files.remove(path) != null

    override suspend fun list(prefix: String): List<String> = files.keys.filter { it.startsWith(prefix) }

    override suspend fun exists(path: String): Boolean = files.containsKey(path)

    override suspend fun length(path: String): Long? = files[path]?.size?.toLong()
}

/** What [OfflineItem] holds: one poem, one book (a Ganjoor category and its descendants) or all of a poet. */
@Serializable
enum class OfflineItemKind { Poem, Book, Poet }

@Serializable
data class OfflineItem(
    val id: String,
    val kind: OfflineItemKind,
    val title: String,
    val subtitle: String? = null,
    val totalRefs: List<PoemRef> = emptyList(),
    val pendingRefs: List<PoemRef> = emptyList(),
    val completedRefs: List<PoemRef> = emptyList(),
    val bytes: Long = 0,
) {
    val complete: Boolean get() = pendingRefs.isEmpty()
}
