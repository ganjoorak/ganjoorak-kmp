package net.ganjoorak.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

const val ECHOLALIA_API_BASE_URL = "https://echolalia.ir/wp-json/wp/v2"

@Serializable
data class WpCategory(
    val id: Int = 0,
    val count: Int = 0,
    val link: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val parent: Int = 0,
    val description: String? = null,
)

@Serializable
data class WpRendered(val rendered: String? = null)

@Serializable
data class WpPost(
    val id: Int = 0,
    val date: String? = null,
    val slug: String? = null,
    val link: String? = null,
    val title: WpRendered? = null,
    val content: WpRendered? = null,
    val excerpt: WpRendered? = null,
    val categories: List<Int> = emptyList(),
)

data class WpPage<T>(val items: List<T>, val totalPages: Int)

private const val CATEGORY_FIELDS = "id,name,slug,count,parent,link,description"

/** WordPress REST API of echolalia.ir, an archive of world poetry in Persian translation. */
class EcholaliaApi(
    private val client: HttpClient,
    private val baseUrl: String = ECHOLALIA_API_BASE_URL,
) {
    suspend fun categoryBySlug(slug: String): WpCategory? =
        client.get("$baseUrl/categories") {
            parameter("slug", slug)
            parameter("_fields", CATEGORY_FIELDS)
        }.json<List<WpCategory>>().firstOrNull()

    suspend fun category(id: Int): WpCategory = client.get("$baseUrl/categories/$id") {
        parameter("_fields", CATEGORY_FIELDS)
    }.json()

    suspend fun categories(page: Int): WpPage<WpCategory> {
        val response = client.get("$baseUrl/categories") {
            parameter("per_page", 100)
            parameter("page", page)
            parameter("_fields", CATEGORY_FIELDS)
        }
        return WpPage(response.json(), response.headers["x-wp-totalpages"]?.toIntOrNull() ?: 1)
    }

    suspend fun postsInCategory(categoryId: Int, page: Int): WpPage<WpPost> {
        val response = client.get("$baseUrl/posts") {
            parameter("categories", categoryId)
            parameter("per_page", 100)
            parameter("page", page)
            parameter("_fields", "id,date,slug,link,title,categories")
        }
        return WpPage(response.json(), response.headers["x-wp-totalpages"]?.toIntOrNull() ?: 1)
    }

    suspend fun post(id: Int): WpPost = client.get("$baseUrl/posts/$id") {
        parameter("_fields", "id,date,slug,link,title,content,excerpt,categories")
    }.json()

    suspend fun search(query: String, perPage: Int): List<WpPost> = client.get("$baseUrl/posts") {
        parameter("search", query)
        parameter("per_page", perPage)
        parameter("_fields", "id,slug,link,title,content,excerpt,categories")
    }.json()
}
