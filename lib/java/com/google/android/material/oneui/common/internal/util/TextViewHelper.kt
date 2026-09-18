package com.google.android.material.oneui.common.internal.util

import android.widget.TextView

enum class MaxFontScaleRatio {
    NONE,
    SMALL,
    MEDIUM,
    LARGE
}

fun TextView.checkMaxFontScale(resId: Int, maxFontScaleRatio: MaxFontScaleRatio = MaxFontScaleRatio.LARGE) {
}
