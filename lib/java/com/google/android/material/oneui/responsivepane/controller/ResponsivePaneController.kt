package com.google.android.material.oneui.responsivepane.controller

import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ViewGroup
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

/**
 * Coordinates pane state, gestures, layout, and visual effects.
 *
 * Initialize with a layout before forwarding framework callbacks. The owning layout installs
 * its own listeners; replacing them can disconnect accessibility and back handling.
 */
interface ResponsivePaneController {
	/**
	 * Listener for transition-end notifications. The drawer argument may be null if unavailable.
	 *
	 * End notifications can also occur when a running animation is canceled.
	 */
	var onStateChangedListener: ((ViewGroup?, PaneState) -> Unit)?
	/**
	 * Listener for width changes, receiving a nullable drawer and normalized slide progress.
	 *
	 * Progress is 0 for closed and 1 for opened.
	 */
	var onSlideOffsetChangedListener: ((ViewGroup?, Float) -> Unit)?

	/**
	 * Associates this controller with [responsivePaneLayout] and initializes gestures and presets.
	 */
	fun initialize(responsivePaneLayout: ResponsivePaneLayout)
	/**
	 * Reapplies the current strategy's constraints to [view].
	 */
	fun updateResponsiveLayout(view: ResponsivePaneLayout)
	/**
	 * Evaluates interception of [ev] within [viewGroup].
	 *
	 * @return true to intercept, false to decline, or null to defer to the layout superclass.
	 */
	fun onInterceptTouchEvent(viewGroup: ViewGroup, ev: MotionEvent): Boolean?
	/**
	 * Processes [event] within [viewGroup].
	 *
	 * @return true if consumed, false if unconsumed, or null to defer to the layout superclass.
	 */
	fun onTouchEvent(viewGroup: ViewGroup, event: MotionEvent): Boolean?
	/**
	 * Updates gesture tracking after [viewGroup] is laid out.
	 *
	 * @param changed whether the layout bounds changed.
	 * @param left left bound in parent coordinates.
	 * @param top top bound in parent coordinates.
	 * @param right right bound in parent coordinates.
	 * @param bottom bottom bound in parent coordinates.
	 */
	fun onLayout(viewGroup: ViewGroup, changed: Boolean, left: Int, top: Int, right: Int, bottom: Int)
	/**
	 * Updates the associated [responsivePaneLayout] for [newConfig].
	 */
	fun onConfigurationChanged(responsivePaneLayout: ResponsivePaneLayout, newConfig: Configuration)
	/**
	 * Advances gesture settling for [viewGroup] and schedules further frames when needed.
	 */
	fun computeScroll(viewGroup: ViewGroup)
	/**
	 * Whether content outside an opened drawer may receive interaction.
	 */
	val isOutsideTouchEnabled: Boolean
	/**
	 * Sets the controller's outside-touch policy to [enabled].
	 *
	 * Use the layout setter to also refresh accessibility and back handling.
	 */
	fun setOutsideTouchEnabled(enabled: Boolean)
	/**
	 * Requests [state] for [viewGroup].
	 *
	 * @param animate whether to animate the transition.
	 */
	fun setPaneState(viewGroup: ViewGroup, state: PaneState, animate: Boolean)
	/**
	 * Returns the visual and width configuration for [paneState].
	 */
	fun getPaneConfig(paneState: PaneState): ResponsiveConfig
	/**
	 * Stores [responsiveConfig] for [paneState] on [viewGroup].
	 *
	 * If the state is current, reapplies it without animation.
	 */
	fun setPaneConfig(
		viewGroup: ResponsivePaneLayout,
		paneState: PaneState,
		responsiveConfig: ResponsiveConfig
	)
	/**
	 * Returns the strategy's current settled pane state.
	 */
	fun getCurrentPaneState(): PaneState
	/**
	 * Cancels strategy animations and clears cached views and elevation effects.
	 *
	 * Does not unregister listeners or discard stored pane configurations.
	 */
	fun cleanup()
}
