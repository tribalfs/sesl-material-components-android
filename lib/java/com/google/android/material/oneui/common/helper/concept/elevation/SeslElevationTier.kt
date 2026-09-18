package com.google.android.material.oneui.common.helper.concept.elevation

import android.content.Context
import androidx.appcompat.R
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_DARK_LG
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_DARK_MD
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_DARK_SM
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_DARK_XL
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_DARK_ZERO
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_LIGHT_LG
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_LIGHT_MD
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_LIGHT_SM
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_LIGHT_XL
import androidx.appcompat.oneui.common.internal.semblurinfo.FIGMA_BLUR_COMPONENT_LIGHT_ZERO

enum class SeslElevationTier(val elevationDimenRes: Int) {
    ELEVATION_ZERO(R.dimen.sesl_figma_elevation_zero),
    ELEVATION_SM(R.dimen.sesl_figma_elevation_sm),
    ELEVATION_MD(R.dimen.sesl_figma_elevation_md),
    ELEVATION_LG(R.dimen.sesl_figma_elevation_lg),
    ELEVATION_XL(R.dimen.sesl_figma_elevation_xl);

    fun getElevation(context: Context): Float {
        return context.resources.getDimension(elevationDimenRes)
    }
}

object SeslElevationDefaultAttributeMap {
    fun getBlurPreset(tier: SeslElevationTier): ColorCurvePreset {
        return when (tier) {
            SeslElevationTier.ELEVATION_ZERO -> ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_ZERO, FIGMA_BLUR_COMPONENT_DARK_ZERO)
            SeslElevationTier.ELEVATION_SM -> ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_SM, FIGMA_BLUR_COMPONENT_DARK_SM)
            SeslElevationTier.ELEVATION_MD -> ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_MD, FIGMA_BLUR_COMPONENT_DARK_MD)
            SeslElevationTier.ELEVATION_LG -> ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_LG, FIGMA_BLUR_COMPONENT_DARK_LG)
            SeslElevationTier.ELEVATION_XL -> ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_XL, FIGMA_BLUR_COMPONENT_DARK_XL)
        }
    }

    fun getNonBlurBackgroundAlpha(context: Context, tier: SeslElevationTier): Float {
        return 1.0f
    }

    fun getNonBlurBackgroundAlphaRes(tier: SeslElevationTier): Int {
        return R.dimen.sesl_figma_elevation_zero
    }
}
