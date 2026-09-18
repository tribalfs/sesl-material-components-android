package com.google.android.material.oneui.responsivepane.model

import android.content.Context
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import com.google.android.material.oneui.common.internal.util.getFloat

class ResponsiveInitConfig(
    val width: Int,
    val elevation: Int,
    val blur: ColorCurvePreset,
    val nonBlurBGAlpha: Int
) {
    fun generate(context: Context): ResponsiveConfig {
        val resources = context.resources
        return ResponsiveConfig(
            resources.getDimensionPixelSize(width),
            resources.getDimension(elevation),
            blur,
            context.getFloat(nonBlurBGAlpha, 1.0f)
        )
    }
}
