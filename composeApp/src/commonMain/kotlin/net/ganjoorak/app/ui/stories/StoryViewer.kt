package net.ganjoorak.app.ui.stories

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.account.STORY_DISMISS_DRAG_PX
import net.ganjoorak.app.data.account.STORY_DURATION_MS
import net.ganjoorak.app.data.account.STORY_HOLD_MS
import net.ganjoorak.app.data.account.StoryPerson
import net.ganjoorak.app.data.account.chronological
import net.ganjoorak.app.data.account.firstUnseenIndex
import net.ganjoorak.app.data.account.formatStoryTime
import net.ganjoorak.app.data.account.poemRef
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.toPersianDigits
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val DRAG_START_PX = 10f

/**
 * Full-screen viewer of a followed person's recent quotes: segmented progress bars, auto-advance,
 * hold to pause, tap the halves to move (in RTL the left half is forward), swipe down / Escape / ✕ to leave.
 */
@Composable
fun StoryViewer(
    people: List<StoryPerson>,
    startIndex: Int,
    onSeen: (username: String, quoteId: Long) -> Unit,
    onClose: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    seen: Map<String, Long> = emptyMap(),
) {
    if (people.isEmpty()) return
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        StoryViewerBody(people, startIndex, onSeen, onClose, onOpenPoem, seen)
    }
}

@Composable
private fun StoryViewerBody(
    people: List<StoryPerson>,
    startIndex: Int,
    onSeen: (username: String, quoteId: Long) -> Unit,
    onClose: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    seen: Map<String, Long>,
) {
    val colors = MaterialTheme.colorScheme
    var personIndex by remember { mutableIntStateOf(startIndex.coerceIn(people.indices)) }
    val person = people[personIndex]
    val quotes = remember(person) { person.chronological() }
    var quoteIndex by remember { mutableIntStateOf(person.firstUnseenIndex(seen).coerceIn(0, (quotes.size - 1).coerceAtLeast(0))) }
    var isHeld by remember { mutableStateOf(false) }
    var pressing by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(pressing, isDragging) {
        if (!pressing || isDragging) {
            if (!pressing) isHeld = false
            return@LaunchedEffect
        }
        delay(STORY_HOLD_MS)
        if (pressing && !isDragging) isHeld = true
    }
    val closeState = rememberUpdatedState(onClose)
    val nextQuote = quotes.getOrNull(quoteIndex.coerceIn(0, (quotes.size - 1).coerceAtLeast(0)))
    val accent = parseAccent(person.user.accent) ?: colors.primary
    val isPaused = isHeld || isDragging
    val progress = remember(person.user.username, quoteIndex) { Animatable(0f) }

    LaunchedEffect(nextQuote?.id, person.user.username) {
        if (nextQuote != null) onSeen(person.user.username, nextQuote.id)
    }

    val goToPerson = { index: Int, atEnd: Boolean ->
        val target = people[index]
        personIndex = index
        val list = target.chronological()
        quoteIndex = if (atEnd) (list.size - 1).coerceAtLeast(0) else target.firstUnseenIndex(seen)
    }

    val next: () -> Unit = {
        when {
            quoteIndex < quotes.size - 1 -> quoteIndex++
            personIndex < people.size - 1 -> goToPerson(personIndex + 1, false)
            else -> closeState.value()
        }
    }
    val previous: () -> Unit = {
        when {
            quoteIndex > 0 -> quoteIndex--
            personIndex > 0 -> goToPerson(personIndex - 1, true)
            else -> quoteIndex = 0
        }
    }

    LaunchedEffect(person.user.username, quoteIndex, isPaused) {
        if (nextQuote == null) return@LaunchedEffect
        if (isPaused) {
            progress.stop()
            return@LaunchedEffect
        }
        val remaining = ((1f - progress.value) * STORY_DURATION_MS).toInt().coerceAtLeast(1)
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        next()
    }

    val density = LocalDensity.current
    val dismissPx = with(density) { 110.dp.toPx() }.coerceAtLeast(STORY_DISMISS_DRAG_PX)
    val cardShape = RoundedCornerShape(16.dp)
    val opacity = if (dragY > 0f) max(0.35f, 1f - dragY / 420f) else 1f

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.45f))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Escape -> { closeState.value(); true }
                    Key.DirectionLeft -> { next(); true }
                    Key.DirectionRight -> { previous(); true }
                    Key.Spacebar -> { isHeld = !isHeld; true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val isWide = maxWidth >= 640.dp
            Surface(
                modifier = Modifier
                    .then(
                        if (isWide) Modifier.widthIn(max = 448.dp).height(704.dp).padding(16.dp).clip(cardShape)
                        else Modifier.fillMaxSize(),
                    )
                    .offset { IntOffset(0, dragY.roundToInt()) },
                color = colors.surface.copy(alpha = opacity),
                tonalElevation = if (isWide) 6.dp else 0.dp,
                shape = if (isWide) cardShape else RoundedCornerShape(0.dp),
            ) {
                if (nextQuote != null) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            quotes.forEachIndexed { index, _ ->
                                val fill = when {
                                    index < quoteIndex -> 1f
                                    index == quoteIndex -> progress.value
                                    else -> 0f
                                }
                                Box(
                                    Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(50)).background(colors.surfaceVariant),
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth(fill).height(3.dp).clip(RoundedCornerShape(50)).background(accent),
                                    )
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProfileAvatar(
                                username = person.user.username,
                                displayName = person.user.displayName.ifBlank { person.user.username },
                                avatar = person.user.avatar,
                                size = 40.dp,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    person.user.displayName.ifBlank { person.user.username },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatStoryTime(nextQuote.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = closeState.value) {
                                Icon(Icons.Filled.Close, contentDescription = "بستن")
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))

                        val poemStyle = LocalPoemStyle.current
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(person.user.username, quoteIndex) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        var dragging = false
                                        pressing = true
                                        isHeld = false
                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    val wasHeld = isHeld
                                                    pressing = false
                                                    isHeld = false
                                                    if (dragging) {
                                                        isDragging = false
                                                        val totalY = change.position.y - down.position.y
                                                        if (totalY > dismissPx) closeState.value() else dragY = 0f
                                                    } else if (!wasHeld) {
                                                        // Absolute left half is forward, as on the website (RTL).
                                                        if (change.position.x < size.width / 2f) next() else previous()
                                                    }
                                                    break
                                                }
                                                val totalX = change.position.x - down.position.x
                                                val totalY = change.position.y - down.position.y
                                                if (!dragging && totalY > DRAG_START_PX && totalY > abs(totalX)) {
                                                    dragging = true
                                                    isDragging = true
                                                }
                                                if (dragging) {
                                                    dragY = max(0f, totalY)
                                                    change.consume()
                                                }
                                            }
                                        } finally {
                                            pressing = false
                                            isHeld = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val classical = nextQuote.key.startsWith("ganjoor:")
                                nextQuote.lines.forEachIndexed { index, line ->
                                    val coupletEnd = classical && (nextQuote.from + index) % 2 == 1
                                    Text(
                                        line,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = poemStyle.fontFamily,
                                            fontSize = poemStyle.fontSize * 1.15f,
                                            lineHeight = poemStyle.fontSize * 1.9f,
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = if (coupletEnd) 20.dp else 8.dp),
                                    )
                                }
                            }
                        }

                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = {
                                    val ref = nextQuote.poemRef()
                                    if (ref != null) onOpenPoem(ref) else closeState.value()
                                },
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    "${nextQuote.poetName} · ${nextQuote.poemTitle}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.fillMaxSize())
                }
            }
        }
    }
}
