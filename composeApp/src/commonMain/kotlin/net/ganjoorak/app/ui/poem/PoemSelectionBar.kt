package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.account.MAX_QUOTE_LINES
import net.ganjoorak.app.util.toPersianDigits

/** Inclusive 0-based range of selected visible lines. */
data class LineSelection(val from: Int, val to: Int) {
    val count: Int get() = to - from + 1
    val range: IntRange get() = from..to

    fun extendTo(index: Int): LineSelection {
        val start = minOf(from, index)
        val end = maxOf(to, index)
        // Keep a contiguous range covering the original span and the new index.
        return LineSelection(minOf(start, from, index), maxOf(end, to, index))
    }
}

/**
 * Toolbar over a verse selection, matching the website's PoemSelectionMenu:
 * «در صفحهٔ من» (or «برای اشتراک وارد شوید»), copy, and a share card.
 */
@Composable
fun PoemSelectionBar(
    lineCount: Int,
    signedIn: Boolean,
    saving: Boolean,
    onSave: () -> Unit,
    onLogin: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overLimit = lineCount > MAX_QUOTE_LINES
    Surface(
        modifier = modifier.widthIn(max = 520.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (signedIn) {
                    TextButton(onClick = onSave, enabled = !saving) {
                        Icon(Icons.Outlined.FormatQuote, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(if (saving) "در حال ذخیره…" else "در صفحهٔ من")
                    }
                } else {
                    TextButton(onClick = onLogin) {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("برای اشتراک وارد شوید")
                    }
                }
                TextButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("کپی")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("اشتراک")
                }
            }
            if (overLimit && signedIn) {
                Text(
                    "در صفحه فقط ${MAX_QUOTE_LINES.toPersianDigits()} مصرع نخست می‌ماند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }
        }
    }
}
