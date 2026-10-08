package com.google.android.material.oneui.responsivepane.model

import android.content.Context
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import com.google.android.material.oneui.common.internal.util.getFloat

/**
 * Resource-backed settings resolved into a [ResponsiveConfig].
 *
 * @property width dimension resource ID for drawer width.
 * @property elevation dimension resource ID for elevation.
 * @property blur blur preset carried into the resolved configuration.
 * @property nonBlurBGAlpha resource ID for a floating-point background opacity value.
 */
class ResponsiveInitConfig(
	val width: Int,
	val elevation: Int,
	val blur: ColorCurvePreset,
	val nonBlurBGAlpha: Int
) {
	/**
	 * Resolves dimensions and background opacity using [context].
	 *
	 * @return configuration with width and elevation in pixels; opacity defaults to 1 if unresolved.
	 */
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
