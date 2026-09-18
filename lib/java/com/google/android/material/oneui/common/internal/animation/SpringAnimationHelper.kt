package com.google.android.material.oneui.common.internal.animation

import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

inline fun <T> T.springAnimation(
    name: String,
    crossinline getter: (T) -> Float,
    crossinline setter: (T, Float) -> Unit,
    crossinline onUpdate: (T) -> Unit = {},
    crossinline onEnd: () -> Unit = {}
): SpringAnimation {
    val prop = object : FloatPropertyCompat<T>(name) {
        override fun getValue(newValue: T): Float = getter(newValue)
        override fun setValue(newValue: T, value: Float) {
            setter(newValue, value)
            onUpdate(newValue)
        }
    }
    val springAnim = SpringAnimation(this, prop)
    val springForce = SpringForce(getter(this)).apply {
        dampingRatio = 1.0f
        stiffness = 1500.0f
    }
    springAnim.spring = springForce
    springAnim.addEndListener { _, _, _, _ -> onEnd() }
    return springAnim
}
