package com.google.android.material.oneui.floatingactioncontainer

import android.animation.TimeInterpolator
import android.view.animation.PathInterpolator
import androidx.reflect.feature.SeslFloatingFeatureReflector

/**
 * Configuration of the alpha animations used when a floating layout shows or hides.
 *
 * The animation durations are shortened when the device supports the 3D surface
 * transition graphics feature.
 */
class FloatingAnimationConfig {
    companion object {
        const val ALPHA_DURATION = 150L
        const val ALPHA_DURATION_SHORT = 80L
        private val ALPHA_ANIM_INTERPOLATOR = PathInterpolator(0f, 0f, 1f, 1f)
    }

    val layoutAlphaAnimationDuration: Long
    val layoutAnimationInterpolator: TimeInterpolator
    val backgroundAlphaAnimationDuration: Long
    val backgroundAlphaAnimationInterpolator: TimeInterpolator

    init {
        val shortDuration = shouldShortDuration()
        layoutAlphaAnimationDuration = if (shortDuration) ALPHA_DURATION_SHORT else ALPHA_DURATION
        layoutAnimationInterpolator = ALPHA_ANIM_INTERPOLATOR
        backgroundAlphaAnimationDuration = if (shortDuration) ALPHA_DURATION_SHORT else ALPHA_DURATION
        backgroundAlphaAnimationInterpolator = ALPHA_ANIM_INTERPOLATOR
    }

    private fun shouldShortDuration(): Boolean {
        val flag = SeslFloatingFeatureReflector.getString(
            SeslFloatingFeatureReflector.SURFACE_TRANSITION_FLAG
        ) ?: "false"
        return flag == "false"
    }
}
