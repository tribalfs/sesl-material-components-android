package com.google.android.material.oneui.floatingdock.animation

import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation

/**
 * Creates a [SpringAnimation] for a custom property using getter and setter lambdas.
 *
 * @param T The type of the animated object.
 * @param name The property name.
 * @param getter Lambda to get the property value.
 * @param setter Lambda to set the property value.
 * @return A [SpringAnimation] for the given property.
 */
fun <T> springAnimation(
    target: T,
    name: String,
    getter: (T) -> Float,
    setter: (T, Float) -> Unit
): SpringAnimation {
    val property = object : FloatPropertyCompat<T>(name) {
        override fun getValue(`object`: T): Float = getter(`object`)
        override fun setValue(`object`: T, value: Float) = setter(`object`, value)
    }
    return SpringAnimation(target, property)
}