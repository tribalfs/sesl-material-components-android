package com.google.android.material.oneui.floatingdock.util

import android.content.Context
import android.view.ViewConfiguration

val Context.scaledTouchSlop: Int
    get() = ViewConfiguration.get(this).scaledTouchSlop

/**
 * Checks if the device is a phone-sized device.
 *
 * This is determined by checking if the smallest screen width is less than 600dp.
 *
 * @return `true` if the device is a phone-sized device, `false` otherwise.
 */
fun Context.isPhoneSize(): Boolean {
    return resources.configuration.smallestScreenWidthDp < 600
}