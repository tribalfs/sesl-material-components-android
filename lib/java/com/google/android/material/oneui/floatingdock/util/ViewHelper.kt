package com.google.android.material.oneui.floatingdock.util

import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.DimenRes

/**
 * Extension function to get the current layout bounds of a View, including translation.
 */
fun View.getCurrentLayoutBounds(outBounds: Rect) {
    requireNotNull(outBounds) { "outBounds must not be null" }
    outBounds.set(left, top, right, bottom)
    outBounds.offset(translationX.toInt(), translationY.toInt())
}

/**
 * Extension function to update the View's layout params and position to match the target bounds.
 */
fun View.updateViewBounds(targetBounds: Rect) {
    requireNotNull(targetBounds) { "targetBounds must not be null" }
    val layoutParams = layoutParams
    val marginLayoutParams = layoutParams as? ViewGroup.MarginLayoutParams
    if (marginLayoutParams != null) {
        marginLayoutParams.width = targetBounds.width()
        marginLayoutParams.height = targetBounds.height()
        marginLayoutParams.leftMargin = targetBounds.left
        marginLayoutParams.topMargin = targetBounds.top
        setLayoutParams(marginLayoutParams)
        invalidate()
    }
}

/**
 * Extension function to check if the View's layout direction is RTL.
 */
fun View.isLayoutRtl(): Boolean {
    return layoutDirection == View.LAYOUT_DIRECTION_RTL
}

/**
 * Extension function to get a float value from a dimension resource.
 * Returns true if the resource is a float type, and fills [out] with the value.
 */
fun View.getFloat(@DimenRes id: Int, out: TypedValue): Boolean {
    requireNotNull(out) { "out must not be null" }
    resources.getValue(id, out, true)
    return out.type == TypedValue.TYPE_FLOAT
}

/**
 * Extension function to get a float value from a dimension resource, or a default if not a float.
 */
fun View.getFloat(@DimenRes id: Int, default: Float): Float {
    val typedValue = TypedValue()
    resources.getValue(id, typedValue, true)
    return if (typedValue.type == TypedValue.TYPE_FLOAT) typedValue.float else default
}

inline fun View.doOnGlobalLayout(crossinline block: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                block()
            }
        }
    )
}