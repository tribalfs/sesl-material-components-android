package com.google.android.material.oneui.floatingdock.animation

import android.util.Log
import android.view.View
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * A class that provides a spring animation for scaling a view.
 *
 * This class uses two `SpringAnimation` objects, one for the X-axis and one for the Y-axis, to animate the scale of a
 * view. The `SpringAnimation` objects are configured with a `SpringForce` that has a damping ratio of 1.0 and a
 * stiffness of 1_500.0.
 *
 * The `ScaleSpringAnimation` class provides methods for animating the view to a final scale, checking if the animation
 * is running, and setting the spring force.
 *
 * @param view The view to be animated.
 */
class ScaleSpringAnimation(view: View) {

    companion object {
        const val TAG = "ScaleSpringAnimation"
    }

    private val ratio = 1_000f

    private val scaleXAnimation: SpringAnimation = SpringAnimation(view, object : FloatPropertyCompat<View>("scaleX") {
        override fun getValue(view: View): Float {
            return ratio * view.scaleX
        }

        override fun setValue(view: View, value: Float) {
            view.scaleX = value / ratio
        }
    }).apply {
        spring = SpringForce(ratio * view.scaleX).apply {
            dampingRatio = 1.0f
            stiffness = 1_500.0f
        }
    }

    private val scaleYAnimation: SpringAnimation = SpringAnimation(view, object : FloatPropertyCompat<View>("scaleY") {
        override fun getValue(view: View): Float {
            return ratio * view.scaleY
        }

        override fun setValue(view: View, value: Float) {
            view.scaleY = value / ratio
        }
    }).apply {
        spring = SpringForce(ratio * view.scaleY).apply {
            dampingRatio = 1.0f
            stiffness = 1_500.0f
        }
    }

    private val animations: List<SpringAnimation> = listOf(scaleXAnimation, scaleYAnimation)

    private val updateListeners: MutableList<(Any?, Any?) -> Any?> = mutableListOf()

    fun addUpdateListener(function: (Any?, Any?) -> Any?) {
        updateListeners.add(function)
    }

    fun animateToFinalPosition(scaleX: Float, scaleY: Float) {
        Log.d(TAG, "animateToFinalPosition scaleX=$scaleX, scaleY=$scaleY")
        scaleXAnimation.animateToFinalPosition(scaleX * ratio)
        scaleYAnimation.animateToFinalPosition(scaleY * ratio)
    }

    fun isRunning(): Boolean {
        return animations.any { it.isRunning }
    }

    fun setSpringForce(dampingRatio: Float?, stiffness: Float?) {
        for (animation in animations) {
            val spring = animation.spring ?: continue
            if (dampingRatio != null) {
                spring.dampingRatio = dampingRatio
            }
            if (stiffness != null) {
                spring.stiffness = stiffness
            }
        }
    }
}