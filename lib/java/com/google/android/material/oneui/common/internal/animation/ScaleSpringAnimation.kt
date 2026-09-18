package com.google.android.material.oneui.common.internal.animation

import android.view.View
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.common.internal.debug

class ScaleSpringAnimation(view: View) : MaterialLogTag {
    private val animations: List<SpringAnimation>
    override val logTag: String = "ScaleSpringAnimation"
    private val ratio: Float = 1000.0f
    private val scaleXAnimation: SpringAnimation
    private val scaleYAnimation: SpringAnimation
    private val updateListeners: MutableList<(Float, Float) -> Unit> = mutableListOf()

    init {
        val xProp = object : FloatPropertyCompat<View>("scaleX") {
            override fun getValue(newValue: View): Float = ratio * newValue.scaleX
            override fun setValue(newValue: View, value: Float) {
                newValue.scaleX = value / ratio
            }
        }
        val animX = SpringAnimation(view, xProp)
        val springX = SpringForce(ratio * view.scaleX).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animX.spring = springX

        val yProp = object : FloatPropertyCompat<View>("scaleY") {
            override fun getValue(newValue: View): Float = ratio * newValue.scaleY
            override fun setValue(newValue: View, value: Float) {
                newValue.scaleY = value / ratio
            }
        }
        val animY = SpringAnimation(view, yProp)
        val springY = SpringForce(ratio * view.scaleY).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animY.spring = springY

        scaleXAnimation = animX
        scaleYAnimation = animY
        animations = listOf(scaleXAnimation, scaleYAnimation)
    }

    fun addUpdateListener(function: (Float, Float) -> Unit) {
        updateListeners.add(function)
    }

    fun animateToFinalPosition(scaleX: Float, scaleY: Float) {
        debug("animateToFinalPosition scaleX=$scaleX, scaleY=$scaleY")
        scaleXAnimation.animateToFinalPosition(scaleX * ratio)
        scaleYAnimation.animateToFinalPosition(scaleY * ratio)
    }

    fun isRunning(): Boolean {
        return animations.any { it.isRunning }
    }

    fun setSpringForce(dampingRatio: Float?, stiffness: Float?) {
        animations.forEach { anim ->
            val spring = anim.spring
            if (dampingRatio != null) {
                spring.dampingRatio = dampingRatio
            }
            if (stiffness != null) {
                spring.stiffness = stiffness
            }
        }
    }
}
