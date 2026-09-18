package com.google.android.material.oneui.responsivepane.helper

import android.content.Context
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

class ResponsiveConfigBuilder private constructor(
    private val context: Context,
    private val mode: ResponsivePaneLayoutMode,
    private val paneState: PaneState
) {
    var config: ResponsiveConfig = ResponsivePanePresetConfigs.paneConfig(context, mode, paneState)
        private set

    companion object {
        @JvmStatic
        fun create(context: Context, mode: ResponsivePaneLayoutMode, paneState: PaneState): ResponsiveConfigBuilder {
            return ResponsiveConfigBuilder(context, mode, paneState)
        }
    }

    fun build(): ResponsiveConfig = config

    fun customize(transform: (ResponsiveConfig) -> ResponsiveConfig): ResponsiveConfigBuilder {
        config = transform(config)
        return this
    }

    fun widthPx(widthPx: Int): ResponsiveConfigBuilder {
        return customize { it.copy(width = widthPx) }
    }

    fun elevationTier(tier: SeslElevationTier): ResponsiveConfigBuilder {
        return customize { prev ->
            ResponsivePanePresetConfigs.paneConfigForTier(context, tier, prev.width)
        }
    }

    fun resetElevationToPreset(): ResponsiveConfigBuilder {
        return elevationTier(ResponsivePanePresetConfigs.elevationTier(mode, paneState))
    }

    fun replace(config: ResponsiveConfig): ResponsiveConfigBuilder {
        this.config = config
        return this
    }
}
