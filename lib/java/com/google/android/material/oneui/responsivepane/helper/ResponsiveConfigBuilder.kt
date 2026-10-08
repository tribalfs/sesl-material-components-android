package com.google.android.material.oneui.responsivepane.helper

import android.content.Context
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

/**
 * Builds a pane configuration from a layout-mode and state preset.
 *
 * Mutating methods return this builder for chaining; [build] returns its current immutable value.
 */
class ResponsiveConfigBuilder private constructor(
	private val context: Context,
	private val mode: ResponsivePaneLayoutMode,
	private val paneState: PaneState
) {
	/**
	 * Current configuration, replaceable through the builder methods.
	 */
	var config: ResponsiveConfig = ResponsivePanePresetConfigs.paneConfig(context, mode, paneState)
		private set

	/**
	 * Factory for preset-based builders.
	 */
	companion object {
		/**
		 * Creates a builder initialized with the requested preset.
		 *
		 * @param context context used to resolve theme and elevation resources.
		 * @param mode layout mode used to select the elevation preset.
		 * @param paneState state used to select the width and elevation preset.
		 * @return a new mutable builder.
		 */
		@JvmStatic
		fun create(
			context: Context,
			mode: ResponsivePaneLayoutMode,
			paneState: PaneState
		): ResponsiveConfigBuilder {
			return ResponsiveConfigBuilder(context, mode, paneState)
		}
	}

	/**
	 * Returns the current configuration without resetting this builder.
	 */
	fun build(): ResponsiveConfig = config

	/**
	 * Replaces the current configuration with the result of [transform].
	 *
	 * @return this builder.
	 */
	fun customize(transform: (ResponsiveConfig) -> ResponsiveConfig): ResponsiveConfigBuilder {
		config = transform(config)
		return this
	}

	/**
	 * Sets the drawer width to [widthPx] pixels, retaining other settings.
	 *
	 * @return this builder.
	 */
	fun widthPx(widthPx: Int): ResponsiveConfigBuilder {
		return customize { it.copy(width = widthPx) }
	}

	/**
	 * Replaces elevation, blur, and background alpha with the defaults for [tier], retaining width.
	 *
	 * @return this builder.
	 */
	fun elevationTier(tier: SeslElevationTier): ResponsiveConfigBuilder {
		return customize { prev ->
			ResponsivePanePresetConfigs.paneConfigForTier(context, tier, prev.width)
		}
	}

	/**
	 * Restores elevation, blur, and alpha for the original mode and state, retaining width.
	 *
	 * @return this builder.
	 */
	fun resetElevationToPreset(): ResponsiveConfigBuilder {
		return elevationTier(ResponsivePanePresetConfigs.elevationTier(mode, paneState))
	}

	/**
	 * Replaces all settings with [config].
	 *
	 * @return this builder.
	 */
	fun replace(config: ResponsiveConfig): ResponsiveConfigBuilder {
		this.config = config
		return this
	}
}
