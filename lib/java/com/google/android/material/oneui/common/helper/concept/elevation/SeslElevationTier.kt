package com.google.android.material.oneui.common.helper.concept.elevation

import android.content.Context
import android.util.TypedValue
import androidx.annotation.DimenRes
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
import androidx.core.view.SemBlurCompat

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
    private val blurLightByTier = mapOf(
        SeslElevationTier.ELEVATION_ZERO to FIGMA_BLUR_COMPONENT_LIGHT_ZERO,
        SeslElevationTier.ELEVATION_SM to FIGMA_BLUR_COMPONENT_LIGHT_SM,
        SeslElevationTier.ELEVATION_MD to FIGMA_BLUR_COMPONENT_LIGHT_MD,
        SeslElevationTier.ELEVATION_LG to FIGMA_BLUR_COMPONENT_LIGHT_LG,
        SeslElevationTier.ELEVATION_XL to FIGMA_BLUR_COMPONENT_LIGHT_XL
    )
    private val blurDarkByTier = mapOf(
        SeslElevationTier.ELEVATION_ZERO to FIGMA_BLUR_COMPONENT_DARK_ZERO,
        SeslElevationTier.ELEVATION_SM to FIGMA_BLUR_COMPONENT_DARK_SM,
        SeslElevationTier.ELEVATION_MD to FIGMA_BLUR_COMPONENT_DARK_MD,
        SeslElevationTier.ELEVATION_LG to FIGMA_BLUR_COMPONENT_DARK_LG,
        SeslElevationTier.ELEVATION_XL to FIGMA_BLUR_COMPONENT_DARK_XL
    )
    private val nonBlurBackgroundAlphaByTier = mapOf(
        SeslElevationTier.ELEVATION_ZERO to R.dimen.sesl_figma_non_blur_bg_alpha_zero,
        SeslElevationTier.ELEVATION_SM to R.dimen.sesl_figma_non_blur_bg_alpha_sm,
        SeslElevationTier.ELEVATION_MD to R.dimen.sesl_figma_non_blur_bg_alpha_md,
        SeslElevationTier.ELEVATION_LG to R.dimen.sesl_figma_non_blur_bg_alpha_lg,
        SeslElevationTier.ELEVATION_XL to R.dimen.sesl_figma_non_blur_bg_alpha_xl
    )

    internal fun getBlurLightCurve(tier: SeslElevationTier): SemBlurCompat.CurveParameter {
        return blurLightByTier.getValue(tier)
    }

    internal fun getBlurDarkCurve(tier: SeslElevationTier): SemBlurCompat.CurveParameter {
        return blurDarkByTier.getValue(tier)
    }

    fun getBlurPreset(tier: SeslElevationTier): ColorCurvePreset {
        return ColorCurvePreset(getBlurLightCurve(tier), getBlurDarkCurve(tier))
    }

    fun getNonBlurBackgroundAlpha(context: Context, tier: SeslElevationTier): Float {
        val value = TypedValue()
        context.resources.getValue(getNonBlurBackgroundAlpha(tier), value, true)
        return if (value.type == TypedValue.TYPE_FLOAT) value.float else 1.0f
    }

    @DimenRes
    internal fun getNonBlurBackgroundAlpha(tier: SeslElevationTier): Int {
        return nonBlurBackgroundAlphaByTier.getValue(tier)
    }

    @DimenRes
    fun getNonBlurBackgroundAlphaRes(tier: SeslElevationTier): Int {
        return getNonBlurBackgroundAlpha(tier)
    }
}
