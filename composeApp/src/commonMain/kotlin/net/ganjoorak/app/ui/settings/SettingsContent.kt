package net.ganjoorak.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import net.ganjoorak.app.APP_VERSION
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.VersePosition
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.data.local.DailyPoemMode
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.platform.rememberNotificationPermissionRequester
import net.ganjoorak.app.platform.rescheduleDailyPoemWork
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.ui.poem.PoemBlockView
import net.ganjoorak.app.ui.theme.fontFamilyFor
import net.ganjoorak.app.ui.theme.fontLabel
import net.ganjoorak.app.util.toPersianDigits
import kotlin.math.roundToInt

private val SamplePreview = PoemBlock.Couplet(
    Verse(1, VersePosition.Right, "بشنو این نی چون شکایت می‌کند"),
    Verse(2, VersePosition.Left, "از جدایی‌ها حکایت می‌کند"),
)

/** Reading and appearance settings plus the about section; shown on the profile page. */
@Composable
fun SettingsContent() {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    val uriHandler = LocalUriHandler.current

    Column {
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Box(Modifier.padding(vertical = 16.dp)) {
                PoemBlockView(SamplePreview, number = 1, sideBySide = false, activeVerse = null, onVerseClick = null)
            }
        }

        SectionTitle("پوسته")
        ChipRow(
            options = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.Paper, ThemeMode.System, ThemeMode.Dynamic),
            selected = settings.theme,
            label = {
                when (it) {
                    ThemeMode.Dark -> "تاریک"
                    ThemeMode.Light -> "روشن"
                    ThemeMode.Paper -> "کاغذی"
                    ThemeMode.System -> "هماهنگ با دستگاه"
                    ThemeMode.Dynamic -> "پویا — از والپیپر"
                }
            },
            onSelect = { mode -> store.update { it.copy(theme = mode) } },
        )

        SectionTitle("قلم شعر")
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PoemFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.poemFont == font,
                    onClick = { store.update { it.copy(poemFont = font) } },
                    label = { Text(fontLabel(font), style = TextStyle(fontFamily = fontFamilyFor(font))) },
                )
            }
        }

        SectionTitle("اندازهٔ متن شعر: ${(settings.fontScale * 100).roundToInt().toPersianDigits()}٪")
        Slider(
            value = settings.fontScale,
            onValueChange = { value -> store.update { it.copy(fontScale = (value * 20).roundToInt() / 20f) } },
            valueRange = 0.8f..1.6f,
            steps = 15,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionTitle("چیدمان بیت‌ها")
        ChipRow(
            options = CoupletLayout.entries,
            selected = settings.coupletLayout,
            label = {
                when (it) {
                    CoupletLayout.Auto -> "خودکار"
                    CoupletLayout.Stacked -> "زیر هم"
                    CoupletLayout.SideBySide -> "دو ستونی"
                }
            },
            onSelect = { layout -> store.update { it.copy(coupletLayout = layout) } },
        )

        SwitchRow("شمارهٔ بیت‌ها", settings.showVerseNumbers) { on -> store.update { it.copy(showVerseNumbers = on) } }
        SwitchRow("دنبال کردن بیت‌ها هنگام خوانش", settings.autoScrollWithAudio) { on ->
            store.update { it.copy(autoScrollWithAudio = on) }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("شعر روزانه")
        Text(
            "ویجت صفحهٔ اصلی و اعلان روزانه، هر روز یک شعر نشان می‌دهند.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        ChipRow(
            options = listOf(DailyPoemMode.Random, DailyPoemMode.Followed, DailyPoemMode.Poet),
            selected = settings.dailyPoemMode,
            label = {
                when (it) {
                    DailyPoemMode.Random -> "تصادفی از همه"
                    DailyPoemMode.Followed -> "از شاعران دنبال‌شده"
                    DailyPoemMode.Poet -> "شاعر مشخص"
                }
            },
            onSelect = { mode -> store.update { it.copy(dailyPoemMode = mode) } },
        )
        if (settings.dailyPoemMode == DailyPoemMode.Poet) {
            var pickerOpen by remember { mutableStateOf(false) }
            SettingRow("شاعر", settings.dailyPoetName ?: "انتخاب شاعر") { pickerOpen = true }
            if (pickerOpen) {
                DailyPoetPickerDialog(
                    onDismiss = { pickerOpen = false },
                    onSelect = { poet ->
                        store.update { it.copy(dailyPoetKey = poet.key, dailyPoetName = poet.name) }
                        pickerOpen = false
                    },
                )
            }
        }
        if (settings.dailyPoemMode == DailyPoemMode.Followed && settings.followedPoets.isEmpty()) {
            Text(
                "هنوز شاعری را دنبال نکرده‌اید؛ تا آن موقع شعر از همهٔ شاعران انتخاب می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        val requestPermission = rememberNotificationPermissionRequester()
        SwitchRow("اعلان روزانه", settings.dailyNotificationEnabled) { on ->
            store.update { it.copy(dailyNotificationEnabled = on) }
            if (on) {
                requestPermission()
                rescheduleDailyPoemWork()
            }
        }
        if (settings.dailyNotificationEnabled) {
            var timePickerOpen by remember { mutableStateOf(false) }
            val timeLabel = "${settings.dailyNotificationHour.toPersianDigits()}:" +
                settings.dailyNotificationMinute.toString().padStart(2, '0').toPersianDigits()
            SettingRow("ساعت اعلان", timeLabel) { timePickerOpen = true }
            if (timePickerOpen) {
                DailyTimePickerDialog(
                    initialHour = settings.dailyNotificationHour,
                    initialMinute = settings.dailyNotificationMinute,
                    onDismiss = { timePickerOpen = false },
                    onConfirm = { hour, minute ->
                        store.update { it.copy(dailyNotificationHour = hour, dailyNotificationMinute = minute) }
                        rescheduleDailyPoemWork()
                        timePickerOpen = false
                    },
                )
            }
            Text(
                "اگر اعلان نمی‌رسد، اجازهٔ اعلان‌ها را در تنظیمات سیستم بررسی کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("درباره")
        Text(
            "گنجورک ${APP_VERSION.toPersianDigits()} — خوانش شعر فارسی از گنجینهٔ گنجور",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LinkRow("وب‌سایت گنجورک", "ganjoorak.ir") { uriHandler.openUri("https://ganjoorak.ir") }
        LinkRow("گنجور", "اشعار و خوانش‌ها از ganjoor.net") { uriHandler.openUri("https://ganjoor.net") }
        LinkRow("کد منبع", "github.com/hadi7546/ganjoorak") { uriHandler.openUri("https://github.com/hadi7546/ganjoorak") }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Picks any poet (classical, contemporary, Echolalia) by name search. */
@Composable
private fun DailyPoetPickerDialog(onDismiss: () -> Unit, onSelect: (Poet) -> Unit) {
    val repository = AppGraph.instance.repository
    var query by remember { mutableStateOf("") }
    var poets by remember { mutableStateOf<List<Poet>?>(null) }
    LaunchedEffect(Unit) {
        val classical = runCatching { repository.poetGroups() }.getOrDefault(emptyList()).flatMap { it.poets }
        val contemporary = runCatching { repository.contemporaryGroups() }.getOrDefault(emptyList()).flatMap { it.poets }
        poets = classical + contemporary
    }
    val filtered = remember(query, poets) {
        val q = query.trim()
        val all = poets.orEmpty()
        if (q.isEmpty()) all else all.filter { it.name.contains(q) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب شاعر") },
        text = {
            Column {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجوی شاعر…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (poets == null) {
                    Text("در حال دریافت فهرست شاعران…", modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.key }) { poet ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onSelect(poet) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(poet.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
    )
}

@Composable
private fun DailyTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساعت اعلان روزانه") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
    )
}
