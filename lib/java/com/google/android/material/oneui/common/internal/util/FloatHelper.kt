package com.google.android.material.oneui.common.internal.util

import android.view.View
import com.google.android.material.internal.ViewUtils

fun Float.withRTLDirection(view: View): Float {
    return if (ViewUtils.isLayoutRtl(view)) -this else this
}
