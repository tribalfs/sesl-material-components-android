/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.oneui.responsivepane.helper.concept

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.PathInterpolator
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.util.evaluator.BlurCurveEvaluator
import androidx.appcompat.oneui.common.internal.util.evaluator.SeslFloatEvaluator
import com.google.android.material.oneui.common.helper.concept.SeslConcept
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag
import kotlin.math.roundToInt

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

//Sesl9
class ResponsivePaneElevationConcept(
    val conceptRange: ElevationConceptRange
) : SeslConcept, ResponsivePaneTag {

    override val logTag: String = "ResponsivePaneElevationConcept"
    private val elevationEvaluator = SeslFloatEvaluator(PathInterpolator(0.03f, 0.86f, 0.0f, 1.0f))
    private val blurCurveEvaluator = BlurCurveEvaluator(PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f))

    private fun getBackgroundAlpha(view: View): Int? {
        return view.background?.alpha
    }

    private fun setBackgroundAlpha(view: View, newAlpha: Float): ColorValue {
        val background = view.background
        var defaultColor = -1
        if (background != null) {
            val drawableMutate = background.mutate()
            val gradientDrawable = drawableMutate as? GradientDrawable
            if (android.os.Build.VERSION.SDK_INT >= 24 && gradientDrawable != null) {
                gradientDrawable.color?.let { color ->
                    defaultColor = color.defaultColor
                }
            }
            drawableMutate.alpha = (255.0f * newAlpha).roundToInt().coerceIn(0, 255)
        }
        return ColorValue(newAlpha, defaultColor)
    }

    private fun setBackgroundAlphaConcept(view: View, ratio: Float): ColorValue {
        val minAlpha = conceptRange.minConcept.nonBlurBackgroundAlpha
        val maxAlpha = conceptRange.maxConcept.nonBlurBackgroundAlpha
        val alpha = ((maxAlpha - minAlpha) * ratio + minAlpha).coerceIn(0.0f, 1.0f)
        return setBackgroundAlpha(view, alpha)
    }

    override fun applyConcept(view: View, ratio: Float) {
        val minConcept = conceptRange.minConcept
        val maxConcept = conceptRange.maxConcept
        val fraction = ratio.coerceIn(0.0f, 1.0f)
        var hasBlur = true
        var colorVal: ColorValue? = null

        if (view is BlurSupportable && view.isBlurApplied()) {
            val startBlur = minConcept.blurPreset
            val endBlur = maxConcept.blurPreset
            val startResource = startBlur.getResource(view.context)
            val endResource = endBlur.getResource(view.context)
            val curveParam = blurCurveEvaluator.evaluate(fraction, startResource, endResource)
            view.applyBlurInfo(curveParam)
            hasBlur = curveParam.blurRadius != 0
            val bgAlpha = getBackgroundAlpha(view)
            if (bgAlpha == null) {
                warn("applyConcept: failed to get alpha from background")
            } else if (bgAlpha != 255) {
                debug("applyConcept blurApplied, Initialize alpha in the background")
                setBackgroundAlpha(view, 1.0f)
            }
        }

        if (colorVal == null) {
            colorVal = setBackgroundAlphaConcept(view, fraction)
        }

        val evalElevation = elevationEvaluator.evaluate(fraction, minConcept.elevationDimen, maxConcept.elevationDimen)
        val finalElevation = if (hasBlur) evalElevation else 0.0f
        view.elevation = finalElevation
        debug("applyConcept ratio=$ratio, elevation=$finalElevation, applyConcept=$colorVal, view=$view")
    }
}
//sesl9
