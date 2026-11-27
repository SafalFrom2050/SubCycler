package com.subtrack.core.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Material 3 Expressive v4.0 Role-Split Spring Motion Token System
 * ("Botanical Kinetic Architecture II").
 *
 * Separates focal bouncy springs ([ExpressiveSpatialFast], [ExpressiveSpatialDefault],
 * [ExpressiveSpatialSlow]) from calm zero-overshoot chrome springs ([StandardSpatialFast],
 * [StandardSpatialDefault]) and strictly clamped effect channels ([EffectsFast], [EffectsDefault]).
 */
object ExpressiveMotion {
    const val EXPRESSIVE_FAST_DAMPING = 0.62f
    const val EXPRESSIVE_FAST_STIFFNESS = 600f

    const val EXPRESSIVE_DEFAULT_DAMPING = 0.68f
    const val EXPRESSIVE_DEFAULT_STIFFNESS = 380f

    const val EXPRESSIVE_SLOW_DAMPING = 0.74f
    const val EXPRESSIVE_SLOW_STIFFNESS = 180f

    const val STANDARD_FAST_DAMPING = 0.95f
    const val STANDARD_FAST_STIFFNESS = 700f

    const val STANDARD_DEFAULT_DAMPING = 0.96f
    const val STANDARD_DEFAULT_STIFFNESS = 420f

    const val EFFECTS_FAST_DAMPING = 1.0f
    const val EFFECTS_FAST_STIFFNESS = 800f

    const val EFFECTS_DEFAULT_DAMPING = 1.0f
    const val EFFECTS_DEFAULT_STIFFNESS = 450f

    fun <T> expressiveSpatialFast(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EXPRESSIVE_FAST_DAMPING,
        stiffness = EXPRESSIVE_FAST_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> expressiveSpatialDefault(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EXPRESSIVE_DEFAULT_DAMPING,
        stiffness = EXPRESSIVE_DEFAULT_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> expressiveSpatialSlow(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EXPRESSIVE_SLOW_DAMPING,
        stiffness = EXPRESSIVE_SLOW_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> standardSpatialFast(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = STANDARD_FAST_DAMPING,
        stiffness = STANDARD_FAST_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> standardSpatialDefault(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = STANDARD_DEFAULT_DAMPING,
        stiffness = STANDARD_DEFAULT_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> effectsFast(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EFFECTS_FAST_DAMPING,
        stiffness = EFFECTS_FAST_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> effectsDefault(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = EFFECTS_DEFAULT_DAMPING,
        stiffness = EFFECTS_DEFAULT_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    fun expressiveSpatialFastFloat(): SpringSpec<Float> = expressiveSpatialFast(0.001f)
    fun expressiveSpatialDefaultFloat(): SpringSpec<Float> = expressiveSpatialDefault(0.001f)
    fun expressiveSpatialSlowFloat(): SpringSpec<Float> = expressiveSpatialSlow(0.001f)
    fun standardSpatialFastFloat(): SpringSpec<Float> = standardSpatialFast(0.001f)
    fun standardSpatialDefaultFloat(): SpringSpec<Float> = standardSpatialDefault(0.001f)
    fun expressiveSpatialFastDp(): SpringSpec<Dp> = expressiveSpatialFast()
    fun expressiveSpatialDefaultDp(): SpringSpec<Dp> = expressiveSpatialDefault()
    fun standardSpatialFastDp(): SpringSpec<Dp> = standardSpatialFast()
    fun expressiveSpatialDefaultIntOffset(): SpringSpec<IntOffset> = expressiveSpatialDefault()
    fun expressiveSpatialDefaultIntSize(): SpringSpec<IntSize> = expressiveSpatialDefault()
    fun effectsFastColor(): SpringSpec<Color> = effectsFast()
    fun effectsDefaultColor(): SpringSpec<Color> = effectsDefault()

    /**
     * Clamps raw spring progress strictly into [0f, 1f] for alpha, scrims, and shadows
     * so spring overshoot never causes visual flicker.
     */
    fun clampEffectProgress(raw: Float): Float = raw.coerceIn(0f, 1f)

    /**
     * Hyperbolic resistance factor for sheet/drag overshoot past rest bounds
     * (`1 / (1 + overshoot * resistance)`), matching BunPod's tactile overdrag feel.
     */
    fun applyOverdragResistance(overshoot: Float, resistance: Float = 100f): Float {
        if (overshoot <= 0f) return 1f
        return 1f / (1f + overshoot * resistance.coerceAtLeast(0f))
    }

    /**
     * Computes a calm horizontal nudge target so an edge-clipped filter chip lands
     * inside the comfortable 25%..75% viewport band instead of snapping dead-center.
     * Returns `null` if the item is already comfortably inside the band.
     */
    fun computeComfortBandScrollDelta(
        itemStartPx: Int,
        itemSizePx: Int,
        viewportStartPx: Int,
        viewportEndPx: Int
    ): Int? {
        val viewportWidth = (viewportEndPx - viewportStartPx).coerceAtLeast(1)
        val itemCenter = itemStartPx + itemSizePx / 2
        val comfortMin = viewportStartPx + (viewportWidth * 0.25f).toInt()
        val comfortMax = viewportStartPx + (viewportWidth * 0.75f).toInt()
        return when {
            itemCenter < comfortMin -> itemCenter - comfortMin
            itemCenter > comfortMax -> itemCenter - comfortMax
            else -> null
        }
    }
}

/**
 * Checks whether system animations are disabled (`ANIMATOR_DURATION_SCALE == 0f`)
 * so every spring morph, wave animation, and ambient shape bloom respects accessibility.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        }.getOrDefault(false)
    }
}

/**
 * Returns [spec] when motion is enabled, or an instant [snap] when reduced motion is active.
 */
fun <T> AnimationSpec<T>.orSnapIfReduced(reducedMotion: Boolean): AnimationSpec<T> =
    if (reducedMotion) snap() else this
