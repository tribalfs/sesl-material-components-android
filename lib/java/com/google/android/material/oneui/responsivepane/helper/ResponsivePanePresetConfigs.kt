package com.google.android.material.oneui.responsivepane.helper

import android.content.Context
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationDefaultAttributeMap
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

object ResponsivePanePresetConfigs {
    const val DEFAULT_CLOSED_WIDTH_DP: Int = 72
    const val DEFAULT_OPENED_WIDTH_DP: Int = 300

    fun defaultWidthPx(paneState: PaneState): Int {
        val dp = when (paneState) {
            PaneState.CLOSED -> DEFAULT_CLOSED_WIDTH_DP
            PaneState.OPENED -> DEFAULT_OPENED_WIDTH_DP
        }
        return dp
    }

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

    fun paneConfig(context: Context, mode: ResponsivePaneLayoutMode, paneState: PaneState): ResponsiveConfig {
        return paneConfigForTier(context, elevationTier(mode, paneState), defaultWidthPx(paneState))
    }

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
