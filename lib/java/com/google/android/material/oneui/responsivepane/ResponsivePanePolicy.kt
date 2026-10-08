package com.google.android.material.oneui.responsivepane

/**
 * Default gesture and spring tuning values for responsive pane behavior.
 */
object ResponsivePanePolicy {
	/**
	 * Minimum fling velocity in density-independent pixels per second; multiply by density for pixels.
	 */
	const val MIN_FLING_VELOCITY: Int = 400
	/**
	 * Scale applied to drag distance beyond the opened drawer width.
	 */
	const val OVER_MAX_WIDTH_DRAG_SCALE: Float = 0.02f
	/**
	 * Sensitivity factor reserved for the extra drawer interaction area.
	 */
	const val SESL_EXTRA_AREA_SENSITIVITY: Float = 0.1f
	/**
	 * Damping ratio used when snapping an oversize drawer back to its opened width.
	 */
	const val SNAP_OVER_MAX_WIDTH_SPRING_DAMPING_RATIO: Float = 0.8f
	/**
	 * Spring stiffness used when snapping an oversize drawer back to its opened width.
	 */
	const val SNAP_OVER_MAX_WIDTH_SPRING_STIFFNESS: Float = 255.0f
}
