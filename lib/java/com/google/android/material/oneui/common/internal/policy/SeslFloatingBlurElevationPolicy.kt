package com.google.android.material.oneui.common.internal.policy

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
import androidx.appcompat.oneui.common.internal.util.evaluator.BlurCurveEvaluator
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import androidx.core.view.SemBlurCompat

class SeslFloatingBlurElevationPolicy(context: Context) : MaterialLogTag {

    data class ElevationBlur(
        val elevationResId: Int,
        val blur: ColorCurvePreset
    )

    private val blurCurveEvaluator = BlurCurveEvaluator()
    override val logTag: String = "SeslFloatingBlurElevationPolicy"
    private var sortedElevationBlurPairs: List<Pair<Float, ColorCurvePreset>> = emptyList()

    init {
        update(context)
    }

    companion object {
        private val ELEVATION_ZERO = R.dimen.sesl_figma_elevation_zero
        private val ELEVATION_SM = R.dimen.sesl_figma_elevation_sm
        private val ELEVATION_MD = R.dimen.sesl_figma_elevation_md
        private val ELEVATION_LG = R.dimen.sesl_figma_elevation_lg
        private val ELEVATION_XL = R.dimen.sesl_figma_elevation_xl

        private val ELEVATION_BLUR_ZERO = ElevationBlur(
            ELEVATION_ZERO,
            ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_ZERO, FIGMA_BLUR_COMPONENT_DARK_ZERO)
        )
        private val ELEVATION_BLUR_SM = ElevationBlur(
            ELEVATION_SM,
            ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_SM, FIGMA_BLUR_COMPONENT_DARK_SM)
        )
        private val ELEVATION_BLUR_MD = ElevationBlur(
            ELEVATION_MD,
            ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_MD, FIGMA_BLUR_COMPONENT_DARK_MD)
        )
        private val ELEVATION_BLUR_LG = ElevationBlur(
            ELEVATION_LG,
            ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_LG, FIGMA_BLUR_COMPONENT_DARK_LG)
        )
        private val ELEVATION_BLUR_XL = ElevationBlur(
            ELEVATION_XL,
            ColorCurvePreset(FIGMA_BLUR_COMPONENT_LIGHT_XL, FIGMA_BLUR_COMPONENT_DARK_XL)
        )

        private val ELEVATION_BLUR_BASE = listOf(
            ELEVATION_BLUR_ZERO,
            ELEVATION_BLUR_SM,
            ELEVATION_BLUR_MD,
            ELEVATION_BLUR_LG,
            ELEVATION_BLUR_XL
        )
    }

    fun update(context: Context) {
        val list = ELEVATION_BLUR_BASE.map { elevationBlur ->
            Pair(context.resources.getDimension(elevationBlur.elevationResId), elevationBlur.blur)
        }
        sortedElevationBlurPairs = list.sortedBy { it.first }
        debug("update context policy=$sortedElevationBlurPairs")
    }

    fun curveParameterForElevation(elevationPx: Float, context: Context): SemBlurCompat.CurveParameter {
        if (sortedElevationBlurPairs.isEmpty()) {
            update(context)
        }
        val list = sortedElevationBlurPairs
        if (list.isEmpty()) {
            warn("curveParameterForElevation se is empty. return default Blur Info")
            return ELEVATION_BLUR_ZERO.blur.getResource(context)
        }
        if (elevationPx <= list.first().first) {
            return list.first().second.getResource(context)
        }
        if (elevationPx >= list.last().first) {
            return list.last().second.getResource(context)
        }
        var i = 0
        while (i < list.size - 1) {
            val i5 = i + 1
            if (list[i5].first >= elevationPx) {
                break
            }
            i = i5
        }
        val pair = list[i]
        val fFloatValue = pair.first
        val colorCurvePresetComponent2 = pair.second
        val pair2 = list[i + 1]
        val fFloatValue2 = pair2.first
        val colorCurvePresetComponent3 = pair2.second
        if (elevationPx <= fFloatValue) {
            return colorCurvePresetComponent2.getResource(context)
        }
        val f6 = fFloatValue2 - fFloatValue
        val fraction = if (f6 > 0.0f) ((elevationPx - fFloatValue) / f6).coerceIn(0.0f, 1.0f) else 1.0f
        return blurCurveEvaluator.evaluate(
            fraction,
            colorCurvePresetComponent2.getResource(context),
            colorCurvePresetComponent3.getResource(context)
        )
    }
}
