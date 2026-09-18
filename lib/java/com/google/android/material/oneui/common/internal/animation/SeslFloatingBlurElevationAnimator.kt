package com.google.android.material.oneui.common.internal.animation

import android.content.Context
import androidx.core.view.SemBlurCompat
import com.google.android.material.oneui.common.internal.policy.SeslFloatingBlurElevationPolicy

class SeslFloatingBlurElevationAnimator(
    private val blurElevationPolicy: SeslFloatingBlurElevationPolicy,
    private val floatAnimator: SeslFloatAnimator = SeslFloatAnimator()
) : BaseAnimation {

    override fun cancel() {
        floatAnimator.cancel()
    }

    override fun isRunning(): Boolean {
        return floatAnimator.isRunning()
    }

    override fun skipToEnd() {
        floatAnimator.skipToEnd()
    }

    fun start(
        fromElevationPx: Float,
        toElevationPx: Float,
        skipAnimation: Boolean,
        context: Context,
        onUpdate: (Float, SemBlurCompat.CurveParameter) -> Unit
    ) {
        floatAnimator.start(fromElevationPx, toElevationPx, skipAnimation) { elevation ->
            onUpdate(elevation, blurElevationPolicy.curveParameterForElevation(elevation, context))
        }
    }
}
