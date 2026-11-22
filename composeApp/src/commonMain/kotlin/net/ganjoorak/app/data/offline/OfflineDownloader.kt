package net.ganjoorak.app.data.offline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ganjoorak.app.data.Category
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.PoetDetail

/** Snapshot of one running download job, for progress UI later. */
data class OfflineJob(val itemId: String, val completed: Int, val total: Int)

/**
 * Drives downloads into the [OfflineLibrary]. Jobs run one coroutine per item in [scope];
 * a killed app resumes incomplete items from the persisted index on creation.
 */
class OfflineDownloader(
    private val repository: PoemRepository,
    private val library: OfflineLibrary,
    private val scope: CoroutineScope,
) {
    private val jobs = mutableMapOf<String, Job>()

    private val itemsFlow = MutableStateFlow<List<OfflineItem>>(emptyList())
    val items: StateFlow<List<OfflineItem>> = itemsFlow.asStateFlow()

    private val activeFlow = MutableStateFlow<List<OfflineJob>>(emptyList())
    val active: StateFlow<List<OfflineJob>> = activeFlow.asStateFlow()

    init {
        scope.launch {
            refreshItems()
            resumePartial()
        }
    }

    /** Saves one poem as its own item; it is already in hand, so no fetching. */
    suspend fun downloadPoem(poem: Poem) {
        library.savePoem(poem)
        refreshItems()
    }

    /** Downloads [category], all of its poems and every descendant category's poems. */
    fun downloadBook(category: Category) {
        launch("book:${category.id}") {
            val refs = collectRefs(category)
            runItem(
                OfflineItem(
                    id = "book:${category.id}",
                    kind = OfflineItemKind.Book,
                    title = category.title,
                    subtitle = category.poet?.name,
                    totalRefs = refs,
                    pendingRefs = refs,
                )
            )
        }
    }

    /** Downloads every poem under the poet's root category tree. */
    fun downloadPoet(detail: PoetDetail) {
        val poet = detail.poet
        launch("poet:${poet.key}") {
            val refs = collectRefs(detail.root)
            runItem(
                OfflineItem(
                    id = "poet:${poet.key}",
                    kind = OfflineItemKind.Poet,
                    title = poet.name,
                    subtitle = poet.nickname,
                    totalRefs = refs,
                    pendingRefs = refs,
                )
            )
        }
    }

    /** Cancels the item's job (if running) and deletes the item and its unshared poem files. */
    fun remove(itemId: String) {
        jobs.remove(itemId)?.cancel()
        scope.launch {
            library.remove(itemId)
            refreshItems()
            activeFlow.value = activeFlow.value.filterNot { it.itemId == itemId }
        }
    }

    /** Continues every incomplete item; called once on creation so restarts resume. */
    suspend fun resumePartial() {
        library.items().filterNot { it.complete }.forEach { item ->
            launch(item.id) { runItem(item) }
        }
    }

    private fun launch(itemId: String, block: suspend () -> Unit) {
        jobs[itemId]?.let { if (it.isActive) return }
        val job = scope.launch {
            val self = requireNotNull(coroutineContext[Job])
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Ganjoorak: offline download $itemId failed: $e")
            } finally {
                withContext(NonCancellable) {
                    if (jobs[itemId] == self) jobs.remove(itemId)
                    activeFlow.value = activeFlow.value.filterNot { it.itemId == itemId }
                    refreshItems()
                }
            }
        }
        jobs[itemId] = job
    }

    private suspend fun runItem(draft: OfflineItem) {
        val item = library.beginOrResume(draft)
        refreshItems()
        val total = item.totalRefs.size
        var completed = item.completedRefs.size
        updateActive(item.id, completed, total)
        item.pendingRefs.forEach { ref ->
            val poem = try {
                repository.poem(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null // leave this ref pending and keep going
            } ?: return@forEach
            library.onPoemSaved(item.id, poem)
            completed += 1
            updateActive(item.id, completed, total)
            refreshItems()
        }
    }

    /** Every poem ref in [root] and its descendants, deduped and in tree order. */
    private suspend fun collectRefs(root: Category): List<PoemRef> {
        val refs = linkedMapOf<String, PoemRef>()
        val visited = mutableSetOf<Int>()
        suspend fun walk(category: Category) {
            if (!visited.add(category.id)) return
            category.poems.forEach { refs[it.ref.key] = it.ref }
            category.children.forEach { child ->
                repository.category(child.id)?.let { walk(it) }
            }
        }
        walk(root)
        return refs.values.toList()
    }

    private fun updateActive(itemId: String, completed: Int, total: Int) {
        activeFlow.value = activeFlow.value.filterNot { it.itemId == itemId } + OfflineJob(itemId, completed, total)
    }

    private suspend fun refreshItems() {
        itemsFlow.value = library.items()
    }
}
