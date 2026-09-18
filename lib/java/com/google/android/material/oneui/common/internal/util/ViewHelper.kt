package com.google.android.material.oneui.common.internal.util

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup

fun View.getBackgroundColor(): Int? {
	val background = background ?: return null
	val drawableMutate = background.mutate()
	if (drawableMutate !is GradientDrawable) {
		return null
	}

	// API 24 and above can use the native getColor() API
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		val color = drawableMutate.color ?: return null
		return color.defaultColor
	} else {
		// Backward compatibility for API 23 using Reflection
		return try {
			// GradientDrawable holds its properties inside a nested GradientState object called mGradientState
			val drawableClass = GradientDrawable::class.java
			val stateField = drawableClass.getDeclaredField("mGradientState")
			stateField.isAccessible = true
			val state = stateField.get(drawableMutate) ?: return null

			// Inside GradientState, the ColorStateList is kept in a field named mSolidColors
			val stateClass = state.javaClass
			val colorsField = stateClass.getDeclaredField("mSolidColors")
			colorsField.isAccessible = true

			val colorStateList = colorsField.get(state) as? android.content.res.ColorStateList
			colorStateList?.defaultColor
		} catch (e: Exception) {
			// Fallback
			null
		}
	}
}

fun View.getCurrentLayoutBounds(outBounds: Rect) {
    outBounds.set(left, top, right, bottom)
    outBounds.offset(translationX.toInt(), translationY.toInt())
}

fun View.getViewRect(rect: Rect) {
    rect.set(left, top, right, bottom)
}

fun View.setViewRect(rect: Rect) {
    left = rect.left
    top = rect.top
    right = rect.right
    bottom = rect.bottom
}

fun View.updateViewBounds(targetBounds: Rect) {
    val marginLayoutParams = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    marginLayoutParams.width = targetBounds.width()
    marginLayoutParams.height = targetBounds.height()
    marginLayoutParams.leftMargin = targetBounds.left
    marginLayoutParams.topMargin = targetBounds.top
    layoutParams = marginLayoutParams
    invalidate()
}
