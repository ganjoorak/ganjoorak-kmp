package net.ganjoorak.app.data.account

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ganjoorak.app.platform.currentTimeMillis

data class StoriesState(
    val people: List<StoryPerson> = emptyList(),
    val seen: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = false,
) {
    val unseenCount: Int get() = people.count { !it.isSeen(seen) }
    val hasUnseen: Boolean get() = unseenCount > 0

    fun isUnseen(person: StoryPerson): Boolean = !person.isSeen(seen)
}

/**
 * Followed people's recent fragments, plus the per-device seen map the website
 * keeps in localStorage. Cached for two minutes, scoped to the signed-in username.
 */
class StoriesRepository(
    private val accounts: AccountRepository,
    private val settings: Settings,
    private val scope: CoroutineScope,
) {
    private val stateFlow = MutableStateFlow(StoriesState(seen = readStorySeen(settings)))
    val state: StateFlow<StoriesState> = stateFlow.asStateFlow()

    private var owner: String? = null
    private var fetchedAt = 0L
    private var pending = false

    init {
        scope.launch {
            accounts.session
                .map { it.user?.username }
                .distinctUntilChanged()
                .collect { username ->
                    if (username == null) {
                        owner = null
                        fetchedAt = 0
                        stateFlow.value = StoriesState(seen = readStorySeen(settings))
                    } else {
                        refresh(force = false)
                    }
                }
        }
    }

    fun isUnseen(person: StoryPerson): Boolean = stateFlow.value.isUnseen(person)

    fun markSeen(username: String, quoteId: Long) {
        val seen = markStorySeen(settings, username, quoteId)
        val current = stateFlow.value
        stateFlow.value = current.copy(people = sortStoryPeople(current.people, seen), seen = seen)
    }

    fun refresh(force: Boolean = false) {
        val username = accounts.session.value.user?.username ?: return
        val isFresh = owner == username && currentTimeMillis() - fetchedAt < STORY_CACHE_TTL_MS
        if ((!force && isFresh) || pending) return
        pending = true
        stateFlow.value = stateFlow.value.copy(isLoading = true)
        scope.launch {
            try {
                val fetched = accounts.api.followingQuotes()
                val seen = readStorySeen(settings)
                owner = username
                fetchedAt = currentTimeMillis()
                // Ignore a late answer if the signed-in user changed mid-flight.
                if (accounts.session.value.user?.username == username) {
                    stateFlow.value = StoriesState(people = sortStoryPeople(fetched, seen), seen = seen)
                }
            } catch (_: Exception) {
                if (accounts.session.value.user?.username == username) {
                    stateFlow.value = stateFlow.value.copy(isLoading = false)
                }
            } finally {
                pending = false
            }
        }
    }
}
