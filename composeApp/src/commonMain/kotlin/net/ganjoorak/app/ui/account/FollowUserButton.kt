package net.ganjoorak.app.ui.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph

/**
 * Follow / unfollow one person, matching the website's FollowUserButton.
 * Hidden until the server has answered, and not shown for the viewer's own username.
 */
@Composable
fun FollowUserButton(
    username: String,
    displayName: String,
    modifier: Modifier = Modifier,
) {
    val accounts = AppGraph.instance.accounts
    val session by accounts.session.collectAsState()
    val self = session.user?.username
    if (!session.isSignedIn || self == username) return

    var following by remember(username) { mutableStateOf<Boolean?>(null) }
    var saving by remember(username) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(username) {
        following = runCatching { accounts.followState(username) }.getOrNull()
    }

    val isFollowing = following ?: return
    TextButton(
        onClick = {
            if (saving) return@TextButton
            saving = true
            scope.launch {
                runCatching { accounts.setFollowing(username, !isFollowing) }
                    .onSuccess { following = it }
                saving = false
            }
        },
        enabled = !saving,
        modifier = modifier,
    ) {
        Text(
            if (isFollowing) "دنبال می‌کنید" else "دنبال کردن",
            style = MaterialTheme.typography.labelLarge,
            color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
    }
}
