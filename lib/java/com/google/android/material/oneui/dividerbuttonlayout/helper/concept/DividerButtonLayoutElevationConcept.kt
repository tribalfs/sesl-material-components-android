package com.google.android.material.oneui.dividerbuttonlayout.helper.concept

import android.view.View
import com.google.android.material.oneui.common.helper.concept.SeslConcept
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.dividerbuttonlayout.DividerButtonLayoutTag

class DividerButtonLayoutElevationConcept(
    val tier: SeslElevationTier = SeslElevationTier.ELEVATION_MD
) : SeslConcept, DividerButtonLayoutTag {

    override val logTag: String = "DividerButtonLayoutElevationConcept"

    override fun applyConcept(view: View, ratio: Float) {
        view.elevation = tier.getElevation(view.context) * ratio.coerceIn(0.0f, 1.0f)
    }
}
