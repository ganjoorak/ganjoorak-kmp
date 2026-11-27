package net.ganjoorak.app.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.account.quoteLines
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.platform.copyText
import net.ganjoorak.app.platform.encodePng
import net.ganjoorak.app.platform.shareImage
import net.ganjoorak.app.ui.theme.fontFamilyFor
import net.ganjoorak.app.ui.theme.fontLabel
import net.ganjoorak.app.util.toPersianDigits

/** Fonts offered for share images, as on the website. */
private val SHARE_FONTS = listOf(PoemFont.Vazirmatn, PoemFont.Samim, PoemFont.Gandom, PoemFont.Parastoo, PoemFont.Sahel)

private enum class ShareAction { Link, Text, Image }

/**
 * The poem share screen, redesigned from scratch (2026-11-27): the rendered
 * card is the hero at the top, style choices live in one tonal card of
 * segmented controls, the share action is the single strong fill, and verse
 * selection is a numbered list in the verse's own font. Everything it must
 * do is unchanged — line selection, theme / font / layout, image export,
 * text copy/share, link — only the layout is new. The sheet container
 * itself stays a [ModalBottomSheet], as before.
 */
@Composable
fun ShareSheet(poem: Poem, onDismiss: () -> Unit, initialLines: IntRange? = null) {
    val settings by AppGraph.instance.settings.settings.collectAsState()
    val lines = remember(poem) { poem.quoteLines() }
    var selected by remember(poem, initialLines) {
        mutableStateOf(initialLines?.toSet() ?: lines.indices.take(4).toSet())
    }
    var theme by remember { mutableStateOf(ShareTheme.Night) }
    var layout by remember { mutableStateOf(ShareLayout.Single) }
    var font by remember { mutableStateOf(settings.poemFont.takeIf { it in SHARE_FONTS } ?: PoemFont.Vazirmatn) }
    var done by remember { mutableStateOf<ShareAction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val families = SHARE_FONTS.associateWith { fontFamilyFor(it) }
    val resolver = LocalFontFamilyResolver.current
    val measurer = remember(resolver) { shareTextMeasurer(resolver) }
    val selectedLines = selected.sorted().map { lines[it] }
    val image = remember(selectedLines, theme, layout, families.getValue(font)) {
        if (selectedLines.isEmpty()) null else renderShareImage(measurer, poem.title, poem.poetName, selectedLines, theme, families.getValue(font), layout)
    }
    val shareText = (selectedLines + listOf("", "${poem.title} - ${poem.poetName}", poem.shareUrl)).joinToString("\n").trim()

    LaunchedEffect(done) {
        if (done != null) {
            delay(1400)
            done = null
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item(key = "head") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("اشتراک شعر", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${poem.title} • ${poem.poetName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "بستن")
                    }
                }
            }
            item(key = "preview") {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        "پیش‌نمایش",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .aspectRatio(SHARE_IMAGE_WIDTH / SHARE_IMAGE_HEIGHT.toFloat())
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (image != null) {
                            Image(image, contentDescription = "پیش‌نمایش تصویر اشتراک شعر", modifier = Modifier.fillMaxWidth())
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp),
                                )
                                Text("مصرعی انتخاب نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item(key = "style") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("قالب", style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ShareTheme.entries.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = option == theme,
                                        onClick = { theme = option },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ShareTheme.entries.size),
                                        label = { Text(option.label) },
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("چیدمان", style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ShareLayout.entries.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = option == layout,
                                        onClick = { layout = option },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ShareLayout.entries.size),
                                        label = { Text(option.label) },
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("قلم", style = MaterialTheme.typography.labelLarge)
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SHARE_FONTS.forEach { option ->
                                    FilterChip(
                                        selected = option == font,
                                        onClick = { font = option },
                                        label = { Text(fontLabel(option), style = TextStyle(fontFamily = families.getValue(option))) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val png = image?.let(::encodePng) ?: return@Button
                            message = shareImage(png, "ganjoorak-${poem.id}.png")
                            done = ShareAction.Image
                        },
                        enabled = image != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ActionLabel(done == ShareAction.Image, Icons.Filled.Image, "اشتراک تصویر")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            copyText(poem.shareUrl)
                            done = ShareAction.Link
                        }, modifier = Modifier.weight(1f)) {
                            ActionLabel(done == ShareAction.Link, Icons.Filled.Link, "کپی لینک")
                        }
                        OutlinedButton(onClick = {
                            copyText(shareText)
                            done = ShareAction.Text
                        }, enabled = selectedLines.isNotEmpty(), modifier = Modifier.weight(1f)) {
                            ActionLabel(done == ShareAction.Text, Icons.Filled.ContentCopy, "کپی متن")
                        }
                    }
                    message?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "select") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${selected.size.toPersianDigits()} مصرع انتخاب شده",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    val all = selected.size == lines.size && lines.isNotEmpty()
                    TextButton(onClick = { selected = if (all) emptySet() else lines.indices.toSet() }) {
                        Text(if (all) "لغو انتخاب همه" else "انتخاب همه")
                    }
                }
            }
            itemsIndexed(lines, key = { index, _ -> "l$index" }) { index, line ->
                val isSelected = index in selected
                val colors = MaterialTheme.colorScheme
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isSelected) colors.primaryContainer else colors.surfaceContainer)
                        .clickable { selected = if (isSelected) selected - index else selected + index }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.primary else colors.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (index + 1).toPersianDigits(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                        )
                    }
                    Text(
                        line,
                        style = TextStyle(fontFamily = families.getValue(font)),
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.onPrimary)
                        }
                    } else {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, colors.outline, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionLabel(done: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Icon(if (done) Icons.Filled.Check else icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Text(label, modifier = Modifier.padding(start = 8.dp))
}
