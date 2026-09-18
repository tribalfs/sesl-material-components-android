package com.google.android.material.oneui.floatingdock.util

import android.content.Context
import android.view.ViewConfiguration

fun Context.getScaledTouchSlop(): Int {
    return ViewConfiguration.get(this).scaledTouchSlop
}

fun Context.isPhoneSize(): Boolean {
    return resources.configuration.smallestScreenWidthDp < 600
}
