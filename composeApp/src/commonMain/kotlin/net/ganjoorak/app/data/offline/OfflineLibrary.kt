package net.ganjoorak.app.data.offline

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.remote.GanjoorJson

private const val INDEX_PATH = "index.json"
private const val POEMS_PREFIX = "poems/"

/**
 * Durable offline poem/book store. Each poem is written as soon as it's saved, so
 * partially downloaded books/poets survive restarts and can be resumed via the index.
 */
class OfflineLibrary(private val store: ByteStore) {
    private val mutex = Mutex()
    private val items = linkedMapOf<String, OfflineItem>()
    private var loaded = false

    /** Reads `index.json` once, on first use, since [ByteStore] is suspend-only. */
    private suspend fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val index = store.read(INDEX_PATH) ?: return
        runCatching {
            GanjoorJson.decodeFromString(ListSerializer(OfflineItem.serializer()), index.decodeToString())
        }.getOrNull()?.forEach { items[it.id] = it }
    }

    suspend fun items(): List<OfflineItem> {
        ensureLoaded()
        return items.values.toList()
    }

    suspend fun item(itemId: String): OfflineItem? {
        ensureLoaded()
        return items[itemId]
    }

    suspend fun getPoem(ref: PoemRef): Poem? {
        ensureLoaded()
        val bytes = store.read(POEMS_PREFIX + poemFileName(ref)) ?: return null
        return runCatching {
            GanjoorJson.decodeFromString(Poem.serializer(), bytes.decodeToString())
        }.getOrNull()
    }

    suspend fun hasPoem(ref: PoemRef): Boolean {
        ensureLoaded()
        return store.exists(POEMS_PREFIX + poemFileName(ref))
    }

    /** Stores the item in the index, persists it and returns the item. */
    private suspend fun OfflineItem.saveInIndex(): OfflineItem {
        items[id] = this
        persistIndex()
        return this
    }

    /** Saves a single poem as its own one-poem item. */
    suspend fun savePoem(poem: Poem): OfflineItem = mutex.withLock {
        ensureLoaded()
        store.write(POEMS_PREFIX + poemFileName(poem.ref), GanjoorJson.encodeToString(Poem.serializer(), poem).encodeToByteArray())
        val singleId = "poem:${poem.ref.key}"
        val bytes = store.length(POEMS_PREFIX + poemFileName(poem.ref)) ?: 0
        val item = OfflineItem(
            id = singleId,
            kind = OfflineItemKind.Poem,
            title = poem.title,
            subtitle = poem.poetName,
            totalRefs = listOf(poem.ref),
            pendingRefs = emptyList(),
            completedRefs = listOf(poem.ref),
            bytes = bytes,
        )
        item.saveInIndex()
    }

    /**
     * Starts (or resumes) a multi-poem job. If an item with [OfflineItem.id] already exists,
     * that item is returned so the caller resumes from where it left off.
     */
    suspend fun beginOrResume(item: OfflineItem): OfflineItem = mutex.withLock {
        ensureLoaded()
        items[item.id] ?: item.saveInIndex()
    }

    /** Marks one poem of a multi-poem item as saved and updates the item's pending/completed lists and byte size. */
    suspend fun onPoemSaved(itemId: String, poem: Poem): OfflineItem = mutex.withLock {
        ensureLoaded()
        val item = items[itemId] ?: error("Unknown offline item $itemId")
        val ref = poem.ref
        store.write(POEMS_PREFIX + poemFileName(ref), GanjoorJson.encodeToString(Poem.serializer(), poem).encodeToByteArray())
        val wasPending = ref in item.pendingRefs
        val updated = item.copy(
            pendingRefs = item.pendingRefs - ref,
            completedRefs = if (ref in item.completedRefs) item.completedRefs else item.completedRefs + ref,
            bytes = if (wasPending) item.bytes + (store.length(POEMS_PREFIX + poemFileName(ref)) ?: 0) else item.bytes,
        )
        updated.saveInIndex()
    }

    /** Removes the item and any poem files that no other item still needs. */
    suspend fun remove(itemId: String): Unit = mutex.withLock {
        ensureLoaded()
        val item = items[itemId] ?: return
        val refsStillNeeded = items.values.filter { it.id != itemId }.flatMap { it.totalRefs }.toSet()
        item.totalRefs.filter { ref -> refsStillNeeded.none { it == ref } }.forEach { ref ->
            store.delete(POEMS_PREFIX + poemFileName(ref))
        }
        items.remove(itemId)
        persistIndex()
    }

    private suspend fun persistIndex() {
        val serializer = ListSerializer(OfflineItem.serializer())
        store.write(INDEX_PATH, GanjoorJson.encodeToString(serializer, items.values.toList()).encodeToByteArray())
    }

    companion object {
        /** `:` is unsafe in some filesystems, so the key is used for lookup only. */
        fun poemFileName(ref: PoemRef): String = "${ref.key.replace(":", "_")}.json"
    }
}
