package com.google.android.material.oneui.common.internal.util

import android.content.Context
import android.util.TypedValue

fun Context.getScreenWidth(): Int = resources.displayMetrics.widthPixels
fun Context.getScreenHeight(): Int = resources.displayMetrics.heightPixels

fun Context.getFloat(resId: Int, defaultValue: Float = 0.0f): Float {
    return try {
        val typedValue = TypedValue()
        resources.getValue(resId, typedValue, true)
        typedValue.float
    } catch (e: Exception) {
        defaultValue
    }
}

fun Context.getFloat(resId: Int, typedValue: TypedValue): Boolean {
    return try {
        resources.getValue(resId, typedValue, true)
        true
    } catch (e: Exception) {
        false
    }
}
