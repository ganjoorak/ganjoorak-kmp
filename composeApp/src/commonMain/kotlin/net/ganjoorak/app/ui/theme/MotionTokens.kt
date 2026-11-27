package net.ganjoorak.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * The three spring specs from the M3 Expressive polish plan (§3), in one place so every
 * polished transition uses the same values and no site invents its own spring.
 *
 * Values are the plan's prescribed ones: default spatial damping 0.8 / stiffness 380,
 * fast 0.6 / 800, slow 0.8 / 200. Damping ratios below 1 settle with a hair of overshoot
 * by design; none of these is bouncy.
 */
internal object MotionTokens {
    /** Default spatial spring: damping 0.8, stiffness 380. */
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Fast spring: damping 0.6, stiffness 800. */
    fun <T> fast(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Slow spring: damping 0.8, stiffness 200. */
    fun <T> slow(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
}
