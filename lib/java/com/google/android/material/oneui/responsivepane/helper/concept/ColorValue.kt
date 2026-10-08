package com.google.android.material.oneui.responsivepane.helper.concept

import androidx.annotation.ColorInt

/**
 * Background color and opacity recorded while applying an elevation concept.
 *
 * @property alpha normalized opacity from 0 to 1.
 * @property color ARGB color, or -1 when no gradient background color was available.
 */
data class ColorValue(
	val alpha: Float,
	@param:ColorInt val color: Int
) {
	/**
	 * Returns the recorded opacity and color for logging.
	 */
	override fun toString(): String {
		return "ColorValue(alpha=$alpha, color=${Integer.toHexString(color)})"
	}
}
