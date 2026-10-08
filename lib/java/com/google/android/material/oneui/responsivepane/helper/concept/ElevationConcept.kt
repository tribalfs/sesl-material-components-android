package com.google.android.material.oneui.responsivepane.helper.concept

import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset

/**
 * Visual settings at one endpoint of an elevation transition.
 *
 * @property elevationDimen elevation in pixels.
 * @property blurPreset light and dark blur curve preset.
 * @property nonBlurBackgroundAlpha background opacity used without blur, from 0 to 1.
 */
data class ElevationConcept(
	val elevationDimen: Float,
	val blurPreset: ColorCurvePreset,
	val nonBlurBackgroundAlpha: Float
)
