package net.ganjoorak.app.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.account.StoryPerson

/**
 * The menu's row of followed people with a recent fragment: avatars ringed in the person's
 * accent while unseen, grey once seen, with the name underneath. Renders nothing for an empty list.
 */
@Composable
fun StoriesRow(
    people: List<StoryPerson>,
    isUnseen: (StoryPerson) -> Boolean,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) return
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            "تکه‌ها",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(people, key = { _, person -> person.user.username }) { index, person ->
                val unseen = isUnseen(person)
                val accent = parseAccent(person.user.accent) ?: MaterialTheme.colorScheme.primary
                val ring = if (unseen) accent else muted.copy(alpha = 0.4f)
                Column(
                    Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpen(index) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(ring),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfileAvatar(
                            username = person.user.username,
                            displayName = person.user.displayName.ifBlank { person.user.username },
                            avatar = person.user.avatar,
                            size = 51.dp,
                            borderColor = MaterialTheme.colorScheme.surfaceContainer,
                            borderWidth = 2.dp,
                        )
                    }
                    Text(
                        person.user.displayName.ifBlank { person.user.username },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (unseen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (unseen) MaterialTheme.colorScheme.onSurface else muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
