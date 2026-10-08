package com.google.android.material.oneui.responsivepane.model

import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.core.view.SemBlurCompat

/**
 * Resolved dimensions and visual settings for one pane state.
 *
 * Default negative width and elevation values represent an unresolved configuration, rather
 * than usable drawer dimensions. Use the preset helpers or provide resolved values.
 *
 * @property width drawer width in pixels.
 * @property elevation view elevation in pixels; a negative value marks it as unresolved.
 * @property blur light and dark blur curve preset.
 * @property nonBlurBGAlpha background opacity when blur is not applied, from 0 to 1.
 */
data class ResponsiveConfig(
	val width: Int = -1,
	val elevation: Float = -1.0f,
	val blur: ColorCurvePreset = ColorCurvePreset(
		SemBlurCompat.CurveParameter(
			-1,
			-1.0f,
			-1.0f,
			-1.0f,
			-1.0f,
			-1.0f,
			-1.0f
		)
	),
	val nonBlurBGAlpha: Float = 1.0f
)
