package net.ganjoorak.app.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Predictive-back and navigation transitions for [androidx.navigation3.ui.NavDisplay].
 *
 * The motion (elastic finger-following slide, underlay scale/shift, 32.dp corners,
 * vertical pull, and release-aware easing) is modeled after
 * [essentials](https://github.com/sameerasw/essentials) (AGPL-3.0), reimplemented
 * for this app rather than copied.
 */
object PredictiveBackTransitions {
    const val UNDERLAY_SCALE = 0.88f
    const val UNDERLAY_SHIFT = 0.12f
    const val UNDERLAY_CORNER_DP = 32f
    const val LEAVING_SCALE = 0.82f
    const val VERTICAL_MAX = 0.08f
    const val UNDERLAY_VERTICAL_RATIO = 0.4f

    private const val GESTURE_PHASE = 0.8f
    private const val BACK_THRESHOLD = 0.05f
    private const val GESTURE_SLIDE = 0.12f
    private const val THRESHOLD_BOOST = 0.14f
    private const val UNDERLAY_GESTURE_SHARE = 0.35f
    private const val FADE_START = 0.45f
    private const val FADE_END = 0.85f
    private const val VERTICAL_STIFFNESS = 3f
    private const val PREDICTIVE_MS = 400
    private const val POP_MS = 300

    private val slideSpring = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 500f)

    private enum class Underlay { None, Pop, Predictive }

    @Volatile private var underlay = Underlay.None
    @Volatile private var releasedAt = -1f
    @Volatile private var verticalPull = 0f

    fun push(): ContentTransform {
        underlay = Underlay.None
        verticalPull = 0f
        return (
            slideInHorizontally(slideSpring) { it / 3 } +
                fadeIn(tween(220, delayMillis = 40))
        ) togetherWith (
            scaleOut(tween(POP_MS, easing = FastOutSlowInEasing), targetScale = 0.92f) +
                fadeOut(tween(200))
        )
    }

    fun pop(): ContentTransform {
        underlay = Underlay.Pop
        return ContentTransform(
            targetContentEnter =
                scaleIn(tween(POP_MS, easing = FastOutSlowInEasing), initialScale = UNDERLAY_SCALE) +
                    slideInHorizontally(tween(POP_MS, easing = FastOutSlowInEasing)) {
                        -(it * UNDERLAY_SHIFT).toInt()
                    },
            initialContentExit =
                slideOutHorizontally(slideSpring) { it } +
                    scaleOut(tween(POP_MS), targetScale = 0.95f),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )
    }

    fun predictivePop(@NavigationEvent.SwipeEdge swipeEdge: Int): ContentTransform {
        underlay = Underlay.Predictive
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        return ContentTransform(
            targetContentEnter =
                scaleIn(tween(PREDICTIVE_MS, easing = underlayScaleEasing), initialScale = UNDERLAY_SCALE) +
                    slideInHorizontally(tween(PREDICTIVE_MS, easing = underlayScaleEasing)) {
                        -direction * (it * UNDERLAY_SHIFT).toInt()
                    },
            initialContentExit =
                slideOutHorizontally(tween(PREDICTIVE_MS, easing = predictiveSlideEasing)) { direction * it } +
                    scaleOut(tween(PREDICTIVE_MS, easing = predictiveScaleEasing), targetScale = LEAVING_SCALE) +
                    fadeOut(tween(PREDICTIVE_MS, easing = predictiveFadeEasing)),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )
    }

    /**
     * Watches the system back gesture so custom easings can follow the finger
     * and finish smoothly after release. A no-op on platforms without a dispatcher.
     */
    @Composable
    fun TrackGesture() {
        val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
        val height = LocalWindowInfo.current.containerSize.height.toFloat().coerceAtLeast(1f)
        LaunchedEffect(dispatcher, height) {
            var lastProgress = 0f
            var inGesture = false
            var startY = 0f
            dispatcher.transitionState.collect { state ->
                if (state is NavigationEventTransitionState.InProgress) {
                    val y = state.latestEvent.touchY
                    if (!inGesture) {
                        releasedAt = -1f
                        startY = y
                    }
                    inGesture = true
                    lastProgress = state.latestEvent.progress
                    verticalPull = verticalElastic((y - startY) / height)
                } else if (inGesture) {
                    inGesture = false
                    releasedAt = lastProgress
                    launch {
                        delay(600)
                        if (!inGesture) verticalPull = 0f
                    }
                }
            }
        }
    }

    fun <T : Any> entryDecorator(): NavEntryDecorator<T> =
        NavEntryDecorator { entry ->
            val scope = LocalNavAnimatedContentScope.current
            val pull by scope.transition.animateFloat(
                transitionSpec = {
                    if (initialState == EnterExitState.PreEnter) {
                        tween(PREDICTIVE_MS, easing = underlayVerticalEasing)
                    } else {
                        tween(PREDICTIVE_MS, easing = predictiveVerticalEasing)
                    }
                },
                label = "predictiveVerticalPull",
            ) { state ->
                if (state == EnterExitState.Visible) 0f else 1f
            }
            val corner by scope.transition.animateFloat(
                transitionSpec = {
                    when (underlay) {
                        Underlay.Predictive -> tween(PREDICTIVE_MS, easing = underlayScaleEasing)
                        else -> tween(POP_MS, easing = FastOutSlowInEasing)
                    }
                },
                label = "underlayCorner",
            ) { state ->
                if (state == EnterExitState.PreEnter && underlay != Underlay.None) UNDERLAY_CORNER_DP else 0f
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val entering = scope.transition.targetState == EnterExitState.Visible
                        val ratio = if (entering) UNDERLAY_VERTICAL_RATIO else 1f
                        translationY = pull * ratio * VERTICAL_MAX * size.height
                        if (corner > 0f) {
                            shape = RoundedCornerShape(corner.dp)
                            clip = true
                        }
                    },
            ) {
                entry.Content()
            }
        }

    private fun verticalElastic(dragFraction: Float): Float {
        val d = kotlin.math.abs(dragFraction)
        val v = 1f - 1f / (1f + d * VERTICAL_STIFFNESS)
        return if (dragFraction < 0f) -v else v
    }

    private fun commitFade(u: Float): Float {
        val c = ((u - FADE_START) / (FADE_END - FADE_START)).coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private fun afterRelease(t: Float, during: (Float) -> Float): Float {
        val p = releasedAt
        if (p < 0f || t <= p || p >= 1f) return during(t)
        val from = during(p)
        val u = ((t - p) / (1f - p)).coerceIn(0f, 1f)
        return from + (1f - from) * FastOutSlowInEasing.transform(u)
    }

    private val elasticSlide = Easing { t ->
        if (t < GESTURE_PHASE) {
            val x = t / GESTURE_PHASE
            val elastic = GESTURE_SLIDE * (1f - (1f - x) * (1f - x) * (1f - x))
            val b = ((x - BACK_THRESHOLD) / 0.05f).coerceIn(0f, 1f)
            elastic + THRESHOLD_BOOST * b * b * (3f - 2f * b)
        } else {
            val start = GESTURE_SLIDE + THRESHOLD_BOOST
            start + (1f - start) * ((t - GESTURE_PHASE) / (1f - GESTURE_PHASE))
        }
    }

    private val gestureScale = Easing { t ->
        val x = (t / 0.8f).coerceAtMost(1f)
        1f - (1f - x) * (1f - x)
    }

    private val predictiveSlideEasing = Easing { t ->
        afterRelease(t) { elasticSlide.transform(it * GESTURE_PHASE) }
    }

    private val predictiveScaleEasing = Easing { t ->
        afterRelease(t) { gestureScale.transform(it * GESTURE_PHASE) }
    }

    private val underlayScaleEasing = Easing { t ->
        afterRelease(t) { UNDERLAY_GESTURE_SHARE * gestureScale.transform(it * GESTURE_PHASE) }
    }

    private val predictiveFadeEasing = Easing { t ->
        val p = releasedAt
        if (p < 0f || t <= p || p >= 1f) 0f else commitFade((t - p) / (1f - p))
    }

    private val predictiveVerticalEasing = Easing { t ->
        val p = releasedAt
        when {
            p < 0f -> verticalPull
            t <= p && p > 0f -> verticalPull * (t / p)
            else -> verticalPull
        }
    }

    private val underlayVerticalEasing = Easing { t ->
        val p = releasedAt
        val pull = when {
            p < 0f -> verticalPull
            t <= p && p > 0f -> verticalPull * (t / p)
            else -> verticalPull * (1f - FastOutSlowInEasing.transform(((t - p) / (1f - p)).coerceIn(0f, 1f)))
        }
        1f - pull
    }
}
