package com.google.android.material.oneui.responsivepane.helper

import android.content.Context
import androidx.appcompat.oneui.common.internal.util.dp
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationDefaultAttributeMap
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

/**
 * Factory for standard drawer widths and mode-dependent elevation settings.
 */
object ResponsivePanePresetConfigs {
	/**
	 * Default closed drawer width in density-independent pixels.
	 */
	const val DEFAULT_CLOSED_WIDTH_DP: Int = 72
	/**
	 * Default opened drawer width in density-independent pixels.
	 */
	const val DEFAULT_OPENED_WIDTH_DP: Int = 300

	/**
	 * Returns the default width for [paneState], converted from dp to pixels by the density helper.
	 */
	fun defaultWidthPx(paneState: PaneState): Int {
		val widthDp = when (paneState) {
			PaneState.CLOSED -> DEFAULT_CLOSED_WIDTH_DP
			PaneState.OPENED -> DEFAULT_OPENED_WIDTH_DP
		}
		return widthDp.dp
	}

	/**
	 * Returns the elevation tier for [mode] and [paneState].
	 *
	 * Only the opened drawer in medium mode uses the large elevation tier; other cases use zero.
	 */
	fun elevationTier(mode: ResponsivePaneLayoutMode, paneState: PaneState): SeslElevationTier {
		return when (mode) {
			ResponsivePaneLayoutMode.MEDIUM -> {
				when (paneState) {
					PaneState.CLOSED -> SeslElevationTier.ELEVATION_ZERO
					PaneState.OPENED -> SeslElevationTier.ELEVATION_LG
				}
			}

			ResponsivePaneLayoutMode.LARGE -> SeslElevationTier.ELEVATION_ZERO
		}
	}

	/**
	 * Creates a preset configuration for [mode] and [paneState].
	 *
	 * @param context context used to resolve theme and elevation resources.
	 * @return resolved width and elevation in pixels with the tier's blur and background alpha.
	 */
	fun paneConfig(
		context: Context,
		mode: ResponsivePaneLayoutMode,
		paneState: PaneState
	): ResponsiveConfig {
		return paneConfigForTier(context, elevationTier(mode, paneState), defaultWidthPx(paneState))
	}

	/**
	 * Creates a configuration using the defaults for [tier].
	 *
	 * @param context context used to resolve theme and elevation resources.
	 * @param widthPx drawer width in pixels.
	 * @return resolved configuration for the supplied width and tier.
	 */
	fun paneConfigForTier(context: Context, tier: SeslElevationTier, widthPx: Int): ResponsiveConfig {
		val elevation = tier.getElevation(context)
		val map = SeslElevationDefaultAttributeMap
		return ResponsiveConfig(
			widthPx,
			elevation,
			map.getBlurPreset(tier),
			map.getNonBlurBackgroundAlpha(context, tier)
		)
	}
}
