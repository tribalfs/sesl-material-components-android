package com.google.android.material.oneui.floatingdock.util

import android.content.Context
import android.util.TypedValue
import androidx.annotation.DimenRes

val Context.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

val Context.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

fun Context.getFloat(@DimenRes id: Int, out: TypedValue): Boolean {
    resources.getValue(id, out, true)
    return out.type == TypedValue.TYPE_FLOAT
}

fun Context.getFloat(@DimenRes id: Int, default: Float): Float {
    val typedValue = TypedValue()
    resources.getValue(id, typedValue, true)
    return if (typedValue.type == TypedValue.TYPE_FLOAT) typedValue.float else default
}