package net.ganjoorak.app.data

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ganjoorak.app.data.remote.GanjoorApi
import net.ganjoorak.app.data.remote.PoemDto

/** An error with a message that is safe to show to the user. */
class GanjoorException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PoemRepository(private val api: GanjoorApi) {
    private val mutex = Mutex()
    private var poetGroups: List<PoetGroup>? = null
    private val poems = LruCache<Int, Poem>(64)
    private val categories = LruCache<Int, Category>(32)
    private val timings = LruCache<Int, List<VerseTiming>>(16)

    suspend fun poetGroups(): List<PoetGroup> = mutex.withLock {
        poetGroups ?: request("دریافت فهرست شاعران ممکن نشد") {
            api.centuries()
                .filter { it.poets.isNotEmpty() }
                .sortedBy { it.halfCenturyOrder }
                .map { century ->
                    PoetGroup(
                        title = century.name.orEmpty(),
                        poets = century.poets.filter { it.published }.map { it.toPoet() },
                    )
                }
                .ifEmpty {
                    listOf(PoetGroup("شاعران", api.poets().filter { it.published }.map { it.toPoet() }))
                }
        }.also { poetGroups = it }
    }

    suspend fun poet(id: Int): PoetDetail = request("دریافت اطلاعات شاعر ممکن نشد") {
        val dto = api.poet(id)
        val poet = dto.poet?.toPoet() ?: throw GanjoorException("شاعر پیدا نشد")
        PoetDetail(poet = poet, root = dto.toCategory())
    }

    suspend fun category(id: Int): Category = categories[id] ?: request("دریافت مجموعه ممکن نشد") {
        api.category(id).toCategory()
    }.also { categories[id] = it }

    suspend fun poem(id: Int): Poem = poems[id] ?: request("دریافت شعر ممکن نشد") {
        api.poem(id).toPoem()
    }.also { poems[id] = it }

    suspend fun randomPoem(poetId: Int? = null): Poem = request("دریافت شعر تصادفی ممکن نشد") {
        val dto: PoemDto = api.randomPoem(poetId)
        if (dto.isIncomplete() && dto.id > 0) api.poem(dto.id).toPoem() else dto.toPoem()
    }.also { poems[it.id] = it }

    suspend fun search(term: String, page: Int, pageSize: Int = 20): SearchPage = request("جستجو ممکن نشد") {
        val response = api.search(term, page, pageSize)
        val hits = response.poems.filter { it.id > 0 }.map { it.toSearchHit(term) }
        val paging = response.paging
        SearchPage(
            hits = hits,
            page = page,
            hasNext = paging?.hasNextPage ?: (paging?.let { page < it.totalPages } ?: (hits.size >= pageSize)),
            total = paging?.totalCount,
        )
    }

    suspend fun semanticSearch(query: String, topK: Int = 20): SearchPage = request("جستجوی معنایی در دسترس نیست") {
        val hits = api.semanticSearch(query, topK).results.map { it.toSearchHit() }.filter { it.poemId > 0 }
        SearchPage(hits = hits, page = 1, hasNext = false, total = hits.size)
    }

    suspend fun timings(recitationId: Int): List<VerseTiming> = timings[recitationId] ?: request("همگام‌سازی خوانش ممکن نشد") {
        api.recitationTimings(recitationId)
            .map { VerseTiming(verseOrder = it.verseOrder, startMs = it.audioStartMilliseconds) }
            .sortedBy { it.startMs }
    }.also { timings[recitationId] = it }

    private suspend fun <T> request(failure: String, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GanjoorException) {
        throw e
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) throw GanjoorException("مورد درخواستی پیدا نشد", e)
        throw GanjoorException(failure, e)
    } catch (e: ServerResponseException) {
        throw GanjoorException("$failure. سرور گنجور پاسخ نمی‌دهد", e)
    } catch (e: Exception) {
        throw GanjoorException("$failure. اتصال اینترنت را بررسی کنید", e)
    }
}

internal class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    operator fun get(key: K): V? = map.remove(key)?.also { map[key] = it }

    operator fun set(key: K, value: V) {
        map.remove(key)
        map[key] = value
        while (map.size > maxSize) map.remove(map.keys.first())
    }
}
