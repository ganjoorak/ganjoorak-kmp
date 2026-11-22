package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.offline.OfflineItem
import net.ganjoorak.app.data.offline.OfflineItemKind
import net.ganjoorak.app.data.offline.OfflineJob
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.util.toPersianDigits
import kotlin.math.roundToInt

/** How an offline unit (a poet's works, or one book) should look, matching oryaan.ir. */
enum class OfflineUnitPhase { Idle, Busy, Saved, Partial, Covered }

data class OfflineUnitView(
    val phase: OfflineUnitPhase,
    val percent: Int = 0,
    val bytes: Long = 0,
)

/**
 * Resolves status for [itemId]. A book is [OfflineUnitPhase.Covered] when the
 * poet's complete "all works" item already includes it.
 */
fun offlineUnitView(
    itemId: String,
    items: List<OfflineItem>,
    active: List<OfflineJob>,
    coveringPoetId: String? = null,
): OfflineUnitView {
    val job = active.firstOrNull { it.itemId == itemId }
    if (job != null) {
        val percent = if (job.total <= 0) 0 else (job.completed * 100 / job.total).coerceIn(0, 99)
        return OfflineUnitView(OfflineUnitPhase.Busy, percent)
    }
    val item = items.firstOrNull { it.id == itemId }
    if (item != null && item.complete) return OfflineUnitView(OfflineUnitPhase.Saved, 100, item.bytes)
    if (item != null && !item.complete) {
        val percent = if (item.totalRefs.isEmpty()) {
            0
        } else {
            (item.completedRefs.size * 100 / item.totalRefs.size).coerceIn(0, 99)
        }
        return OfflineUnitView(OfflineUnitPhase.Partial, percent, item.bytes)
    }
    val poet = coveringPoetId?.let { id -> items.firstOrNull { it.id == id } }
    if (poet != null && poet.complete) return OfflineUnitView(OfflineUnitPhase.Covered, 100, poet.bytes)
    return OfflineUnitView(OfflineUnitPhase.Idle)
}

fun offlineUnitLabel(itemId: String, view: OfflineUnitView): String = when (view.phase) {
    OfflineUnitPhase.Busy -> "در حال دریافت ${view.percent.toPersianDigits()}٪"
    OfflineUnitPhase.Saved -> "آفلاین ذخیره شد"
    OfflineUnitPhase.Partial -> "ناقص؛ تکمیل دریافت"
    OfflineUnitPhase.Covered -> "با همهٔ آثار شاعر ذخیره شده"
    OfflineUnitPhase.Idle -> if (itemId.startsWith("poet:")) {
        "ذخیرهٔ همهٔ آثار برای آفلاین"
    } else {
        "ذخیرهٔ آفلاین"
    }
}

/** A short Persian byte size, like «۱۲ کیلوبایت» or «۳٫۴ مگابایت». */
fun persianByteSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val (number, unit) = when {
        kb >= 1024 -> oneDecimal(kb / 1024) to "مگابایت"
        kb >= 10 -> kb.roundToInt().toString() to "کیلوبایت"
        kb >= 1 -> oneDecimal(kb) to "کیلوبایت"
        bytes <= 0L -> return ""
        else -> bytes.toString() to "بایت"
    }
    return "$number $unit".toPersianDigits().replace('.', '٫')
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Composable
fun rememberOfflineUnitView(itemId: String, coveringPoetId: String? = null): OfflineUnitView {
    val downloads = AppGraph.instance.downloads
    val items by downloads.items.collectAsState()
    val active by downloads.active.collectAsState()
    return offlineUnitView(itemId, items, active, coveringPoetId)
}

/**
 * Labeled download control for a poet header or a book page: progress, resume,
 * saved/remove, or the covered state when the poet's works are already stored.
 */
@Composable
fun OfflineUnitButton(
    itemId: String,
    start: () -> Unit,
    modifier: Modifier = Modifier,
    coveringPoetId: String? = null,
    compact: Boolean = false,
) {
    val downloads = AppGraph.instance.downloads
    val view = rememberOfflineUnitView(itemId, coveringPoetId)
    val label = offlineUnitLabel(itemId, view)
    val enabled = view.phase != OfflineUnitPhase.Busy && view.phase != OfflineUnitPhase.Covered
    val onClick: () -> Unit = {
        when (view.phase) {
            OfflineUnitPhase.Idle -> start()
            OfflineUnitPhase.Partial -> downloads.resume(itemId)
            OfflineUnitPhase.Saved -> downloads.remove(itemId)
            OfflineUnitPhase.Busy, OfflineUnitPhase.Covered -> Unit
        }
    }
    if (compact) {
        IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            OfflineUnitIcon(view, label)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            OfflineUnitIcon(view, label)
            Text(
                if (view.phase == OfflineUnitPhase.Saved) "$label · حذف" else label,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OfflineUnitIcon(view: OfflineUnitView, label: String) {
    when (view.phase) {
        OfflineUnitPhase.Busy -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        OfflineUnitPhase.Saved, OfflineUnitPhase.Covered -> Icon(Icons.Filled.Check, contentDescription = label)
        OfflineUnitPhase.Partial, OfflineUnitPhase.Idle -> Icon(Icons.Filled.Download, contentDescription = label)
    }
}

/** Entry on the poets page that opens the on-device download manager. */
@Composable
fun OfflineLibraryEntry(onOpen: () -> Unit) {
    val downloads = AppGraph.instance.downloads
    val items by downloads.items.collectAsState()
    val active by downloads.active.collectAsState()
    val count = items.size
    val meta = when {
        active.isNotEmpty() -> "در حال دریافت"
        count == 0 -> "هنوز کتابی برای خواندن آفلاین ذخیره نشده است."
        else -> "${count.toPersianDigits()} مورد · ${persianByteSize(items.sumOf { it.bytes })}".trimEnd(' ', '·')
    }
    BorderedCard(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
        AppRow(
            title = "آفلاین",
            meta = meta,
            leading = { Icon(Icons.Filled.DownloadForOffline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Manage downloaded poets and books: size, status, resume, and remove. */
@Composable
fun OfflineDownloadsScreen(
    onBack: () -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val downloads = AppGraph.instance.downloads
    val items by downloads.items.collectAsState()
    val active by downloads.active.collectAsState()
    AppPage(
        title = "آفلاین",
        subtitle = "دانلودهای این دستگاه پس از به‌روزرسانی باقی می‌مانند.",
        onBack = onBack,
        actions = {
            if (items.isNotEmpty()) {
                TextButton(onClick = { downloads.removeAll() }) {
                    Text("حذف همهٔ دانلودها")
                }
            }
        },
    ) {
        if (items.isEmpty() && active.isEmpty()) {
            item(key = "empty") {
                Text(
                    "هنوز کتابی برای خواندن آفلاین ذخیره نشده است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        } else {
            items.forEach { item ->
                item(key = item.id) {
                    OfflineManagedRow(
                        item = item,
                        active = active.firstOrNull { it.itemId == item.id },
                        onOpen = {
                            when (item.kind) {
                                OfflineItemKind.Poet -> item.id.removePrefix("poet:").let(onOpenPoet)
                                OfflineItemKind.Book -> item.id.removePrefix("book:").toIntOrNull()?.let(onOpenCategory)
                                OfflineItemKind.Poem -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineManagedRow(item: OfflineItem, active: OfflineJob?, onOpen: () -> Unit) {
    val downloads = AppGraph.instance.downloads
    val view = offlineUnitView(item.id, listOf(item), listOfNotNull(active))
    val status = when (view.phase) {
        OfflineUnitPhase.Busy -> offlineUnitLabel(item.id, view)
        OfflineUnitPhase.Partial -> offlineUnitLabel(item.id, view)
        else -> listOfNotNull(item.subtitle, offlineKindLabel(item.kind), persianByteSize(item.bytes).ifEmpty { null })
            .joinToString(" · ")
    }
    AppRow(
        title = item.title,
        meta = status,
        onClick = onOpen,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (view.phase == OfflineUnitPhase.Partial) {
                    TextButton(onClick = { downloads.resume(item.id) }) { Text("تکمیل") }
                }
                IconButton(onClick = { downloads.remove(item.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف از آفلاین")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun offlineKindLabel(kind: OfflineItemKind): String = when (kind) {
    OfflineItemKind.Poem -> "شعر"
    OfflineItemKind.Book -> "دفتر"
    OfflineItemKind.Poet -> "شاعر"
}
