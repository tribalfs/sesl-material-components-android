package com.google.android.material.oneui.responsivepane

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.policy.BlurInfoState
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.appcompat.oneui.common.internal.semblurinfo.SemBlurInfoStateBuilder
import androidx.core.oneui.common.internal.semblurinfo.SemBlurInfoState
import androidx.core.view.SemBlurCompat
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.responsivepane.controller.ResponsivePaneControllerImpl

/**
 * A drawer layout component for [ResponsivePaneLayout] in Samsung One UI interfaces that supports
 * elevation concepts, blur effects ([BlurSupportable]), and maximum width constraints.
 */
class ResponsiveDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), BlurSupportable, ResponsivePaneTag {

    private var backgroundDrawable: Drawable? = null
    private var blurInfo: SemBlurInfoState? = null
    private var blurMode: Int = 2
    override val logTag: String = "ResponsiveDrawerLayout"
    var maxWidth: Int = -1

    private fun generateBlurInfo(context: Context): SemBlurInfoStateBuilder {
        val dimension = context.resources.getDimension(R.dimen.sesl_floating_pane_background_corner_radius)
        val builder = BlurInfoState.generateDrawerComponentBlurInfoStateBuilder(context, blurMode)
        backgroundDrawable?.let {
            builder.nonBlurBackground(it)
        }
        builder.cornerRadius(dimension)
        return builder
    }

    override fun applyBlurInfo(context: Context): Boolean {
        debug("applyBlurInfo($context)")
        if (Build.VERSION.SDK_INT < 35) {
            return false
        }
        applyBlurInfo(generateBlurInfo(context).build())
        val responsivePaneLayout = parent as? ResponsivePaneLayout
        if (responsivePaneLayout != null) {
            val isOpen = responsivePaneLayout.isOpen
            val controller = responsivePaneLayout.controller
            val impl = controller as? ResponsivePaneControllerImpl
            val concept = impl?.getElevationConcept()
            if (concept == null) {
                warn("applyBlurInfo is failed.($controller, $concept)")
            } else {
                concept.applyConcept(this, if (isOpen) 1.0f else 0.0f)
            }
        } else {
            warn("applyBlurInfo is failed.(parent=$parent)")
        }
        return isBlurApplied()
    }

    override fun applyBlurInfo(curveParameter: SemBlurCompat.CurveParameter): Boolean {
        if (Build.VERSION.SDK_INT < 35) {
            return false
        }
        return applyBlurInfo(generateBlurInfo(context).colorCurvePreset(ColorCurvePreset(curveParameter, curveParameter)).build())
    }

    private fun applyBlurInfo(blurInfoState: SemBlurInfoState): Boolean {
        if (Build.VERSION.SDK_INT < 35) {
            return false
        }
        clearBlurInfo(context)
        val applied = blurInfoState.applyBlurInfo(this)
        this.blurInfo = if (applied) blurInfoState else null
        return applied
    }

    override fun clearBlurInfo(context: Context) {
        blurInfo?.clearBlurInfo(this)
        blurInfo = null
    }

    override fun isBlurApplied(): Boolean = blurInfo != null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (maxWidth == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
                getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom, child.layoutParams.height)
            )
        }
        setMeasuredDimension(if (layoutParams.width >= 0) layoutParams.width else 0, heightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        this.backgroundDrawable = background
    }

    override fun setBlurMode(semBlurInfoMode: Int) {
        this.blurMode = semBlurInfoMode
        applyBlurInfo(context)
    }
}
