package net.ganjoorak.app.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.account.Quote
import net.ganjoorak.app.data.account.StoryPerson
import net.ganjoorak.app.data.account.formatStoryTime
import net.ganjoorak.app.data.account.poemRef
import net.ganjoorak.app.ui.account.FollowUserButton
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.platform.usesSideNavigation
import net.ganjoorak.app.ui.menu.LocalPersistentNavPadding
import net.ganjoorak.app.ui.poem.PoemControlsHeight
import net.ganjoorak.app.ui.stories.ProfileAvatar
import net.ganjoorak.app.ui.stories.StoriesRow

private data class FollowedPost(val person: StoryPerson, val quote: Quote)

/**
 * Home tab «دنبال‌شده‌ها»: stories of the last week, then the same quotes as a
 * vertical list of posts, from people the signed-in reader follows.
 */
@Composable
fun FollowedFeed(
    onOpenLogin: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = AppGraph.instance
    val session by graph.accounts.session.collectAsState()
    val storyState by graph.stories.state.collectAsState()
    var followingCount by remember { mutableStateOf<Int?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    val persistentBottom = LocalPersistentNavPadding.current.calculateBottomPadding() +
        if (usesSideNavigation) 0.dp else PoemControlsHeight

    LaunchedEffect(session.user?.username) {
        if (!session.isSignedIn) {
            followingCount = null
            listError = null
            return@LaunchedEffect
        }
        followingCount = null
        runCatching { graph.accounts.followList() }
            .onSuccess {
                followingCount = it.size
                listError = null
            }
            .onFailure { error ->
                followingCount = 0
                listError = error.message
            }
        graph.stories.refresh()
    }

    val posts = remember(storyState.people) {
        storyState.people.flatMap { person ->
            person.quotes.map { FollowedPost(person, it) }
        }.sortedByDescending { it.quote.createdAt }
    }
    // Actionable (unseen) posts stay as contained cards under one heading;
    // read posts become plain rows under theirs. Same posts, no rows added or
    // removed; a quote is unseen while its id is past the last seen id for
    // that person — the same rule the stories row uses.
    val (unseenPosts, readPosts) = remember(posts, storyState.seen) {
        posts.partition { it.quote.id > (storyState.seen[it.person.user.username] ?: 0L) }
    }

    when {
        session.isLoading && session.user == null -> {
            Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text("در حال بارگذاری…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        !session.isSignedIn -> FeedEmpty(
            title = "وارد شوید",
            body = "برای دیدن تکه‌ها و نوشته‌های کسانی که دنبال می‌کنید وارد حساب شوید.",
            primary = "ورود" to onOpenLogin,
        )
        followingCount == 0 -> FeedEmpty(
            title = "هنوز کسی را دنبال نمی‌کنید",
            body = listError ?: "در نمایهٔ هر کس یا در تکه‌هایش «دنبال کردن» را بزنید تا نوشته‌هایشان اینجا بیاید.",
            primary = null,
        )
        else -> {
            val openStories = LocalAppActions.current.openStories
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp + persistentBottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (storyState.people.isNotEmpty()) {
                    item(key = "stories") {
                        StoriesRow(
                            people = storyState.people,
                            isUnseen = { graph.stories.isUnseen(it) },
                            onOpen = { index -> openStories(storyState.people, index) },
                        )
                    }
                }
                when {
                    followingCount == null && posts.isEmpty() -> item(key = "loading") {
                        Text(
                            "در حال بارگذاری…",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    posts.isEmpty() -> item(key = "empty-posts") {
                        Text(
                            "هنوز تکه‌ای از دنبال‌شده‌ها نیست.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        if (unseenPosts.isNotEmpty()) {
                            item(key = "heading-new") { SectionLabel("تازه‌ها") }
                            items(unseenPosts, key = { "${it.person.user.username}-${it.quote.id}" }) { post ->
                                FollowedPostCard(post, onOpenPoem)
                            }
                        }
                        if (readPosts.isNotEmpty()) {
                            item(key = "heading-read") { SectionLabel("خوانده‌شده‌ها") }
                            items(readPosts, key = { "${it.person.user.username}-${it.quote.id}" }) { post ->
                                FollowedPostRow(post, onOpenPoem)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedPostCard(post: FollowedPost, onOpenPoem: (PoemRef) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        FollowedPostContent(post, onOpenPoem, Modifier.padding(16.dp))
    }
}

/** A read post: the same content as the card, but a plain row, no container. */
@Composable
private fun FollowedPostRow(post: FollowedPost, onOpenPoem: (PoemRef) -> Unit) {
    FollowedPostContent(
        post,
        onOpenPoem,
        Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** The post itself, shared by the contained card and the plain row. */
@Composable
private fun FollowedPostContent(post: FollowedPost, onOpenPoem: (PoemRef) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val user = post.person.user
    val quote = post.quote
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                username = user.username,
                displayName = user.displayName.ifBlank { user.username },
                avatar = user.avatar,
                size = 40.dp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    user.displayName.ifBlank { user.username },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    formatStoryTime(quote.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            FollowUserButton(username = user.username, displayName = user.displayName.ifBlank { user.username })
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    quote.poemRef()?.let { ref -> Modifier.clickable { onOpenPoem(ref) } } ?: Modifier,
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            quote.lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        if (quote.poetName.isNotBlank() || quote.poemTitle.isNotBlank()) {
            Text(
                listOf(quote.poetName, quote.poemTitle).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
