package com.google.android.material.oneui.responsivepane.model

import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.core.view.SemBlurCompat

data class ResponsiveConfig(
    val width: Int = -1,
    val elevation: Float = -1.0f,
    val blur: ColorCurvePreset = ColorCurvePreset(SemBlurCompat.CurveParameter(-1, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f)),
    val nonBlurBGAlpha: Float = 1.0f
)
