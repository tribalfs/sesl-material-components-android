package com.google.android.material.oneui.responsivepane

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.policy.BlurInfoState
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.appcompat.oneui.common.internal.semblurinfo.SemBlurInfoStateBuilder
import androidx.core.oneui.common.internal.semblurinfo.SemBlurInfoState
import androidx.core.view.SemBlurCompat
import androidx.core.view.SemBlurCompat.BLUR_MODE_CANVAS
import androidx.core.view.SemBlurCompat.SeslBlurMode
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn

/**
 * A drawer layout component for [ResponsivePaneLayout] in Samsung One UI interfaces that supports
 * elevation concepts, blur effects ([BlurSupportable]), and maximum width constraints.
 *
 * Use ID `responsive_pane_drawer` when placing this view inside [ResponsivePaneLayout].
 *
 * @param context context used to resolve drawer resources.
 * @param attrs optional XML attributes passed to [FrameLayout].
 * @param defStyleAttr default style attribute passed to [FrameLayout].
 */
class ResponsiveDrawerLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), BlurSupportable, ResponsivePaneTag {

	private var backgroundDrawable: Drawable? = null
	private var blurInfo: SemBlurInfoState? = null
	@SeslBlurMode
	private var blurMode: Int = BLUR_MODE_CANVAS
	/**
	 * Tag used for drawer log messages.
	 */
	override val logTag: String = "ResponsiveDrawerLayout"
	/**
	 * Width in pixels used to measure children while the drawer itself is resized.
	 *
	 * A value of -1 uses normal [FrameLayout] measurement. Setting this property does not
	 * request a layout; the responsive strategy updates layout parameters separately.
	 */
	var maxWidth: Int = -1

	private fun generateBlurInfo(context: Context): SemBlurInfoStateBuilder {
		val dimension =
			context.resources.getDimension(R.dimen.sesl_divider_button_layout_background_radius)
		val builder = BlurInfoState.generateDrawerComponentBlurInfoStateBuilder(context, blurMode)
		backgroundDrawable?.let {
			builder.nonBlurBackground(it)
		}
		builder.cornerRadius(dimension)
		return builder
	}

	/**
	 * Applies drawer blur and the parent pane's current elevation concept.
	 *
	 * @param context context used to resolve blur resources.
	 * @return whether blur remains applied; returns false below API 35 or if application fails.
	 */
	override fun applyBlurInfo(context: Context): Boolean {
		debug("applyBlurInfo($context)")
		if (Build.VERSION.SDK_INT < 35) return false

		applyBlurInfo(generateBlurInfo(context).build())
		val responsivePaneLayout = parent as? ResponsivePaneLayout

		if (responsivePaneLayout != null) {
			val isOpen = responsivePaneLayout.isOpen
			val controller = responsivePaneLayout.controller
			val concept = controller.getElevationConcept()
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

	/**
	 * Applies [curveParameter] as the drawer blur curve for both light and dark themes.
	 *
	 * @return whether blur was applied; returns false below API 35 or if application fails.
	 */
	override fun applyBlurInfo(curveParameter: SemBlurCompat.CurveParameter): Boolean {
		return Build.VERSION.SDK_INT >= 35 && applyBlurInfo(
			generateBlurInfo(context).colorCurvePreset(
				ColorCurvePreset(
					curveParameter,
					curveParameter
				)
			).build()
		)
	}

	private fun applyBlurInfo(blurInfoState: SemBlurInfoState): Boolean {
		if (Build.VERSION.SDK_INT < 35) return false

		clearBlurInfo(context)
		val applied = blurInfoState.applyBlurInfo(this)
		this.blurInfo = if (applied) blurInfoState else null
		return applied
	}

	/**
	 * Clears applied blur and its tracked state.
	 *
	 * @param context context supplied by the blur contract; unused by this implementation.
	 */
	override fun clearBlurInfo(context: Context) {
		blurInfo?.clearBlurInfo(this)
		blurInfo = null
	}

	/**
	 * Returns whether this drawer tracks a successfully applied blur state.
	 */
	override fun isBlurApplied(): Boolean = blurInfo != null

	/**
	 * Measures children at [maxWidth] when configured, otherwise using normal frame measurement.
	 *
	 * @param widthMeasureSpec framework width measurement specification.
	 * @param heightMeasureSpec framework height measurement specification.
	 */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (maxWidth == -1) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			child.measure(
				MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
				getChildMeasureSpec(
					heightMeasureSpec,
					paddingTop + paddingBottom,
					child.layoutParams.height
				)
			)
		}
		setMeasuredDimension(if (layoutParams.width >= 0) layoutParams.width else 0, heightMeasureSpec)
	}

	/**
	 * Consumes [event] so touches within the drawer do not fall through.
	 *
	 * @return true for every event.
	 */
	override fun onTouchEvent(event: MotionEvent): Boolean = true

	/**
	 * Sets [background] and retains it as the non-blur fallback.
	 */
	override fun setBackground(background: Drawable?) {
		super.setBackground(background)
		this.backgroundDrawable = background
	}

	/**
	 * Stores [semBlurInfoMode] and reapplies drawer blur.
	 *
	 * @param semBlurInfoMode blur mode accepted by the Samsung blur builder.
	 */
	override fun setBlurMode(semBlurInfoMode: Int) {
		this.blurMode = semBlurInfoMode
		applyBlurInfo(context)
	}
}
