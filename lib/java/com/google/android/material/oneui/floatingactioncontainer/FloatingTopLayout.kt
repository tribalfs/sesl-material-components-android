package com.google.android.material.oneui.floatingactioncontainer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.SESL_STATE_COLLAPSED
import com.google.android.material.appbar.AppBarLayout.SESL_STATE_EXPANDED
import com.google.android.material.appbar.AppBarLayout.SESL_STATE_HIDE
import com.google.android.material.appbar.AppBarLayout.SESL_STATE_IDLE
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.error
import com.google.android.material.oneui.common.internal.info
import com.google.android.material.oneui.floatingactioncontainer.manager.FloatingScrollableManager

/**
 * Top-aligned floating layout container positioned relative to an [AppBarLayout].
 *
 * Automatically manages available top scroll bounds and background projection alpha
 * transitions based on app bar scroll/collapse states.
 *
 * @param context The Context the view is running in.
 * @param attrs The attributes of the XML tag inflating the view.
 * @param defStyleAttr Default style attribute for styling.
 */
open class FloatingTopLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FloatingGroupLayout(context, attrs, defStyleAttr) {

	companion object {
		private const val DEBUG = true
		private const val TAG = "FloatingTopLayout"

		/** Constant representing an unspecified scroll boundary. */
		const val UNSPECIFIED_BOUND = -1

		/** App bar state flag for idle / no state. */
		const val APP_BAR_STATE_NONE = SESL_STATE_IDLE

		/** App bar state bitmask for expanded state. */
		const val APP_BAR_STATE_EXPANDED_MASK = SESL_STATE_EXPANDED

		/** App bar state bitmask for collapsed state. */
		const val APP_BAR_STATE_COLLAPSED_MASK = SESL_STATE_COLLAPSED

		/** App bar state bitmask for hidden state. */
		const val APP_BAR_STATE_HIDE_MASK = SESL_STATE_HIDE
	}

	/** Controls whether item projection background alpha transition animation is enabled. */
	var enablePrjAlphaTransition: Boolean = true

	/**
	 * CoordinatorLayout behavior for top-aligned floating layouts that listens to [AppBarLayout] state changes.
	 *
	 * @param T The type of [FloatingTopLayout].
	 * @param context The Context the behavior is running in.
	 * @param attrs The attributes of the XML tag inflating the behavior.
	 */
	open class FloatingTopBehavior<T : FloatingTopLayout> @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : FloatingActionBehavior<T>(context, attrs), MaterialLogTag {

		override val logTag: String = "FloatingTopBehavior"

		/** Reference to the attached [AppBarLayout]. */
		var appbarLayout: AppBarLayout? = null

		/** Flag indicating whether the behavior is executing its initial layout pass for the child view. */
		var isFirstLayoutChild: Boolean = true

		/**
		 * Configures default layout parameters and gravity when attached to CoordinatorLayout layout params.
		 *
		 * @param params Target layout parameters.
		 */
		override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
			super.onAttachedToLayoutParams(params)
			if (params.anchorId == UNSPECIFIED_BOUND) {
				error("anchorId is not set")
			}
			if (params.gravity == Gravity.NO_GRAVITY) {
				params.gravity = Gravity.TOP
			}
			if (params.anchorGravity == Gravity.NO_GRAVITY) {
				params.anchorGravity = Gravity.BOTTOM
			}
		}

		/**
		 * Positions the child view relative to its dependent [AppBarLayout] during layout pass.
		 *
		 * @param parent CoordinatorLayout parent view.
		 * @param child Target [FloatingTopLayout] child view.
		 * @param layoutDirection Current layout direction.
		 * @return `true` if handled by behavior, `false` otherwise.
		 */
		override fun onLayoutChild(parent: CoordinatorLayout, child: T, layoutDirection: Int): Boolean {
			val dependencies = parent.getDependencies(child)
			val appBar = with(child){ dependencies.getAppBarLayout() }
			if (appBar != null) {
				debug("onLayoutChild of Behavior AppBarState ${getStateString(appBar.seslGetAppBarState().state)}")
				if (isFirstLayoutChild) {
					performFirstLayoutChild(appBar, child)
					child.projectionView.startProjectionViewItemAnimation(false)
				}
				this.appbarLayout = appBar
			}
			return false
		}

		/**
		 * Sets up initial projection animation state and registers state change listener on [AppBarLayout].
		 *
		 * @param appBarLayout Attached [AppBarLayout].
		 * @param child Target [FloatingTopLayout].
		 */
		private fun performFirstLayoutChild(appBarLayout: AppBarLayout, child: T) {
			val state = appBarLayout.seslGetAppBarState().state
			if (state == APP_BAR_STATE_NONE) {
				// none
			} else if ((state and APP_BAR_STATE_HIDE_MASK) != 0) {
				child.startProjectionViewAlphaAnimationInternal(show = true, animate = false, force = false)
			} else {
				child.startProjectionViewAlphaAnimationInternal(show = false, animate = false, force = false)
			}
			appBarLayout.seslAddAppBarStateChangedListener { oldState, newState ->
				onAppBarStateChanged(oldState, newState, child)
			}
			isFirstLayoutChild = false
		}

		/**
		 * Callback invoked when [AppBarLayout] state changes.
		 *
		 * @param oldState Previous app bar state.
		 * @param newState New app bar state.
		 * @param child Target [FloatingTopLayout].
		 */
		open fun onAppBarStateChanged(oldState: Int, newState: Int, child: T) {
			info("AppBarState Changed old:${getStateString(oldState)} new:${getStateString(newState)}")
			startProjectionViewAlphaAnimationByAppBarStateChanged(child, oldState, newState)
		}

		/**
		 * Evaluates app bar state flags and starts projection background alpha animations.
		 *
		 * @param child Target [FloatingTopLayout].
		 * @param oldState Previous app bar state.
		 * @param newState New app bar state.
		 */
		private fun startProjectionViewAlphaAnimationByAppBarStateChanged(child: T, oldState: Int, newState: Int) {
			val newHide = (newState and APP_BAR_STATE_HIDE_MASK) != 0
			val oldHide = (oldState and APP_BAR_STATE_HIDE_MASK) != 0
			if (newHide && !oldHide) {
				child.startProjectionViewAlphaAnimationInternal(show = true, animate = false, force = false)
			} else if (!newHide && oldHide) {
				child.startProjectionViewAlphaAnimationInternal(show = false, animate = false, force = false)
			}
		}

		/**
		 * Formats numerical app bar state bitmask into a human-readable log string.
		 *
		 * @param state App bar state bitmask.
		 * @return Formatted state string.
		 */
		private fun getStateString(state: Int): String {
			var str = if ((state and APP_BAR_STATE_HIDE_MASK) != 0) "HIDE " else ""
			if ((state and APP_BAR_STATE_COLLAPSED_MASK) != 0) str += "COLLAPSED "
			if ((state and APP_BAR_STATE_EXPANDED_MASK) != 0) str += "EXPANDED"
			return "[ ${str.trim()} ]"
		}
	}

	override val logTag: String = TAG

	/**
	 * Returns the default [FloatingTopBehavior] for layout positioning and app bar coordination.
	 *
	 * @return A new instance of [FloatingTopBehavior].
	 */
	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return FloatingTopBehavior<FloatingTopLayout>(context, attrs)
	}

	/**
	 * Applies this layout's measured height to the available scroll top boundary.
	 */
	open fun applyHeightToAvailBounds() {
		applyScrollAvailBounds(
			top = if (isVisible) measuredHeight else 0,
			bottom = 0,
			tag = javaClass.simpleName,
			dispatchFakeScroll = false
		)
	}

	/**
	 * Updates top and bottom scroll available bounds via [FloatingScrollableManager].
	 *
	 * @param top Top boundary pixel value, or [UNSPECIFIED_BOUND] to leave unchanged.
	 * @param bottom Bottom boundary pixel value, or [UNSPECIFIED_BOUND] to leave unchanged.
	 * @param tag Tag identifying the caller.
	 * @param dispatchFakeScroll Whether to dispatch a fake scroll event to refresh scrollbars.
	 */
	fun applyScrollAvailBounds(
		top: Int = UNSPECIFIED_BOUND,
		bottom: Int = UNSPECIFIED_BOUND,
		tag: String = javaClass.simpleName,
		dispatchFakeScroll: Boolean = true,
	) {
		getFloatingScrollableManager().applyAvailBound(top, bottom, tag, dispatchFakeScroll)
	}

	/**
	 * Configures scrollable view options and updates available top boundaries when a scrollable is attached.
	 */
	override fun applyScrollableViewOptions() {
		super.applyScrollableViewOptions()
		if (getFloatingScrollableManager().getFloatingScrollable() != null) {
			applyHeightToAvailBounds()
		}
	}

	/**
	 * Enables or disables alpha transition animation for toolbar item projection backgrounds.
	 *
	 * @param enable `true` to enable item background alpha transitions, `false` to disable.
	 */
	fun enableToolbarItemBackgroundTransition(enable: Boolean) {
		info("enable Toolbar Item BG Transition enabled:$enable")
		this.enablePrjAlphaTransition = enable
		if (enable) {
			  forceSendAwareCallback()
		}
	}

	/**
	 * Returns whether item background transition animations are currently enabled.
	 *
	 * @return `true` if transitions are enabled, `false` otherwise.
	 */
	fun isEnabledToolbarItemBackgroundTransition(): Boolean = enablePrjAlphaTransition

	/**
	 * Determines whether background blur views need to be invalidated based on layout and projection alpha.
	 *
	 * @return `true` if blur views require invalidation, `false` otherwise.
	 */
	override fun needInvalidateBlurViews(): Boolean {
		return alpha != 0f && projectionView.alpha != 0f
	}

	/**
	 * Handles view layout and updates scrollable view options on layout changes.
	 *
	 * @param changed Whether layout bounds changed.
	 * @param left Left boundary coordinate.
	 * @param top Top boundary coordinate.
	 * @param right Right boundary coordinate.
	 * @param bottom Bottom boundary coordinate.
	 */
	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		if (changed) {
			applyScrollableViewOptions()
		}
	}

	/**
	 * Reacts to view visibility changes to update scrollable boundaries.
	 *
	 * @param changedView Target view whose visibility changed.
	 * @param visibility New visibility state.
	 */
	override fun onVisibilityChanged(changedView: View, visibility: Int) {
		super.onVisibilityChanged(changedView, visibility)
		if (changedView == this) {
			applyScrollableViewOptions()
		}
	}

	/**
	 * Animates projection view item alpha internally based on target visibility and transition config.
	 *
	 * @param show Target visibility state (`true` to show, `false` to hide).
	 * @param animate Whether to animate the alpha transition.
	 * @param force Force animation execution regardless of [enablePrjAlphaTransition].
	 */
	fun startProjectionViewAlphaAnimationInternal(
		show: Boolean,
		animate: Boolean = true,
		force: Boolean = false,
	) {
		if (force || enablePrjAlphaTransition) {
			val targetAlpha = if (show) 1f else 0f
			projectionView.startProjectionViewItemAnimation(animate = true)
			projectionView.startProjectionViewAlphaAnimation(targetAlpha, immediately = !animate)
		}
	}
}
