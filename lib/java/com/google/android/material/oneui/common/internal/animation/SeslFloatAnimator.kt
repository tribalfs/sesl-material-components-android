package com.google.android.material.oneui.common.internal.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import kotlin.math.abs

class SeslFloatAnimator(
    private val durationMs: Long = DEFAULT_DURATION_MS,
    private val interpolator: TimeInterpolator = LinearInterpolator()
) : BaseAnimation {

    private var valueAnimator: ValueAnimator? = null

    companion object {
        const val DEFAULT_DURATION_MS: Long = 150L
        private const val VALUE_EQUALS_EPSILON: Float = 0.01f
    }

    override fun cancel() {
        valueAnimator?.cancel()
        valueAnimator = null
    }

    override fun isRunning(): Boolean {
        return valueAnimator?.isRunning == true
    }

    override fun skipToEnd() {
        valueAnimator?.end()
    }

    fun start(
        fromValue: Float,
        toValue: Float,
        skipAnimation: Boolean,
        onUpdate: (Float) -> Unit
    ) {
        cancel()
        if (skipAnimation || abs(fromValue - toValue) < VALUE_EQUALS_EPSILON) {
            onUpdate(toValue)
            return
        }
        val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
        anim.duration = durationMs
        anim.interpolator = interpolator
        anim.addUpdateListener { va ->
            val fraction = va.animatedValue as Float
            onUpdate((toValue - fromValue) * fraction + fromValue)
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (valueAnimator == animation) {
                    valueAnimator = null
                }
            }
        })
        valueAnimator = anim
        anim.start()
    }
}

typealias SeslFloatingAnimator = SeslFloatAnimator
