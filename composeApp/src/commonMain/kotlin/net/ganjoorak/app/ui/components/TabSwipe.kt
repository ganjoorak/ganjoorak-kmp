package net.ganjoorak.app.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Horizontal swipe to move between tabs, for screens whose tabs are a [SegmentedSwitch] or
 * [AppTabs] and whose content switches with the existing slide animation (feed, poets,
 * favorites).
 *
 * A real HorizontalPager does not fit these screens: the feed's ViewModel holds a single pane
 * for the current mode, and the poets/favorites bodies live inside [AppPage]'s LazyColumn,
 * where a pager would need nested lazy lists with fixed viewport heights and would break the
 * sticky-bar fade and long-list scrolling. The swipe reuses the tab switch a tap already
 * performs, so the tab bar, the content and the slide animation stay exactly as they are.
 *
 * Direction follows the layout: in RTL (Persian) the next tab sits to the left, so a
 * rightward swipe goes forward; in LTR it is the other way round. Vertical scrolling is
 * untouched — the detector only claims horizontal drags, so the feed's vertical poem pager
 * and the lazy lists keep working, and horizontally scrolling rows in the header (tab row,
 * group chips) keep precedence because they consume the drag first.
 *
 * A swipe only switches when the drag passes 72.dp or the fling velocity passes 500 dp/s, so
 * small accidental drags do nothing. There is no wrap-around: swiping past the first or the
 * last tab does nothing.
 */
@Composable
fun Modifier.tabSwipe(
    enabled: Boolean = true,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val distanceThreshold = with(density) { 72.dp.toPx() }
    val velocityThreshold = with(density) { 500.dp.toPx() }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    return draggable(
        state = rememberDraggableState { delta -> totalDrag += delta },
        orientation = Orientation.Horizontal,
        enabled = enabled,
        onDragStopped = { velocity ->
            val distance = totalDrag
            totalDrag = 0f
            val fling = abs(velocity) > velocityThreshold
            if (abs(distance) > distanceThreshold || fling) {
                val delta = if (fling && abs(distance) <= distanceThreshold) velocity else distance
                val forward = if (layoutDirection == LayoutDirection.Rtl) delta > 0f else delta < 0f
                if (forward) onNext() else onPrevious()
            }
        },
    )
}
