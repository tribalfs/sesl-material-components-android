package com.google.android.material.oneui.floatingactioncontainer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.warn

/**
 * Bottom-aligned floating layout container for bottom action bars.
 *
 * Manages bottom scroll available bounds, fading edges, and GoToTop button offsets.
 *
 * @param context The Context the view is running in.
 * @param attrs The attributes of the XML tag inflating the view.
 * @param defStyleAttr Default style attribute for styling.
 */
open class FloatingBottomLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FloatingGroupLayout(context, attrs, defStyleAttr) {

	companion object {
		private const val TAG = "FloatingBottomLayout"
		private const val UNSPECIFIED_BOUND = -1
	}

	private var customGoToTopOffset: Int? = null
	private var useOffsetAvailRect: Boolean = true

	/**
	 * CoordinatorLayout behavior for [FloatingBottomLayout].
	 */
	class FloatingBottomBarBehavior<T : FloatingBottomLayout> @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : FloatingActionBehavior<T>(context, attrs)

	override val logTag: String = TAG

	/**
	 * The child view serving as the bottom bar component.
	 */
	val bottomBar: View
		get() = getChildAt(0)

	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return FloatingBottomBarBehavior<FloatingBottomLayout>(context, attrs)
	}

	/**
	 * Sets a custom offset for the GoToTop floating action button.
	 *
	 * @param offset Custom pixel offset or `null` to use default layout calculation.
	 */
	fun setCustomGoToTopOffset(offset: Int?) {
		this.customGoToTopOffset = offset
		getFloatingScrollableManager().setBottomBarAnimOffset(calculateGoToTopOffset(visibility))
	}

	private fun calculateGoToTopOffset(visibility: Int): Int {
		val scrollable = getFloatingScrollableManager().getFloatingScrollable()
		val defaultBottomPadding = scrollable?.seslGetGoToTopDefaultBottomPadding() ?: 0
		val topBottomPadding = resources.getDimensionPixelSize(R.dimen.sesl_floating_bottom_layout_top_bottom_padding_for_go_to_top)
		val customOffset = customGoToTopOffset
		if (customOffset != null) {
			return customOffset - defaultBottomPadding
		}
		val calcHeight = (topBottomPadding * 2) + (measuredHeight - paddingTop - paddingBottom - defaultBottomPadding)
		return if (visibility == VISIBLE) calcHeight else 0
	}

	override fun applyScrollableViewOptions() {
		super.applyScrollableViewOptions()
		getFloatingScrollableManager().setBottomBarAnimOffset(calculateGoToTopOffset(visibility))
		if (getFloatingScrollableManager().getFloatingScrollable() != null) {
			val availHeight = if (isVisible) measuredHeight else 0
			getFloatingScrollableManager().applyAvailBound(UNSPECIFIED_BOUND, availHeight, javaClass.simpleName, dispatchFakeScroll = false)
			val padding = resources.getDimensionPixelSize(R.dimen.sesl_floating_bottom_layout_top_bottom_padding_for_go_to_top)
			val height = if (isVisible) (height - paddingBottom) + padding else 0
			getFloatingScrollableManager().setFloatingBottomLayoutHeight(height)
		}
	}

	override fun onDetachedFromWindow() {
		getFloatingScrollableManager().applyAvailBound(UNSPECIFIED_BOUND, 0, javaClass.simpleName, dispatchFakeScroll = false)
		getFloatingScrollableManager().setFloatingBottomLayoutHeight(0)
		super.onDetachedFromWindow()
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		applyScrollableViewOptions()
		super.onLayout(changed, left, top, right, bottom)
	}

	override fun onVisibilityChanged(changedView: View, visibility: Int) {
		super.onVisibilityChanged(changedView, visibility)
		if (changedView == this) {
			val padding = resources.getDimensionPixelSize(R.dimen.sesl_floating_bottom_layout_top_bottom_padding_for_go_to_top)
			getFloatingScrollableManager().setBottomBarAnimOffset(calculateGoToTopOffset(visibility))
			val height = if (visibility == VISIBLE) (height - paddingBottom) + padding else 0
			getFloatingScrollableManager().setFloatingBottomLayoutHeight(height)
			applyScrollableViewOptions()
		}
	}

	fun testUseRecyclerViewAvailRectControl(use: Boolean) {
		warn("Changed control recyclerview avail rect option $use")
		this.useOffsetAvailRect = use
		if (use) {
			applyScrollableViewOptions()
		}
	}
}
