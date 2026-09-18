package com.google.android.material.oneui.responsivepane.helper.concept

import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

fun ResponsiveConfig.toElevationTierConcept(): ElevationConcept {
    return ElevationConcept(elevation, blur, nonBlurBGAlpha)
}
