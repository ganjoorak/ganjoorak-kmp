package net.ganjoorak.app.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poem
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

@Composable
fun ShareSheet(poem: Poem, onDismiss: () -> Unit) {
    val settings by AppGraph.instance.settings.settings.collectAsState()
    val lines = remember(poem) { poem.verses.map { it.text } }
    var selected by remember(poem) { mutableStateOf(lines.indices.take(4).toSet()) }
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
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text("اشتراک شعر", style = MaterialTheme.typography.titleLarge)
                    Text(poem.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "preview") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .aspectRatio(SHARE_IMAGE_WIDTH / SHARE_IMAGE_HEIGHT.toFloat())
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (image != null) {
                        Image(image, contentDescription = "پیش‌نمایش تصویر اشتراک شعر", modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("مصرعی انتخاب نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "options") {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OptionRow("قالب", ShareTheme.entries, theme, { it.label }) { theme = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                        Text("قلم", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically))
                        SHARE_FONTS.forEach { option ->
                            FilterChip(
                                selected = option == font,
                                onClick = { font = option },
                                label = { Text(fontLabel(option), style = TextStyle(fontFamily = families.getValue(option))) },
                            )
                        }
                    }
                    OptionRow("چیدمان", ShareLayout.entries, layout, { it.label }) { layout = it }
                }
            }
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    FilledTonalButton(
                        onClick = {
                            val png = image?.let(::encodePng) ?: return@FilledTonalButton
                            message = shareImage(png, "ganjoorak-${poem.id}.png")
                            done = ShareAction.Image
                        },
                        enabled = image != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ActionLabel(done == ShareAction.Image, Icons.Filled.Image, "اشتراک تصویر")
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer,
                        )
                        .clickable { selected = if (isSelected) selected - index else selected + index }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(line, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun <T> OptionRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically))
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun ActionLabel(done: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Icon(if (done) Icons.Filled.Check else icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Text(label, modifier = Modifier.padding(start = 8.dp))
}
