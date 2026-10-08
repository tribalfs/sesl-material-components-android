package com.google.android.material.oneui.responsivepane.behavior

import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.oneui.responsivepane.ResponsiveDrawerLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

/**
 * Receives a nullable drawer and normalized progress, with 0 closed and 1 opened.
 */
typealias ResponsivePaneSlideOffsetCallback = (ResponsiveDrawerLayout?, Float) -> Unit

/**
 * Contract for drawer sizing, state transitions, and responsive constraints.
 */
interface ResponsivePaneBehaviorStrategy {
	/**
	 * Snaps an oversize drawer in [layout] back to its opened width.
	 *
	 * @param callback optional listener for width progress.
	 * @param onAnimationEnd completion callback; implementations may also invoke it on cancellation.
	 */
	fun animateOverWidthToOpened(
		layout: ResponsivePaneLayout,
		callback: ResponsivePaneSlideOffsetCallback? = null,
		onAnimationEnd: () -> Unit
	)

	/**
	 * Moves the drawer in [layout] to [toState].
	 *
	 * @param skipAnimation whether to move immediately to the final width.
	 * @param callback optional listener for width progress.
	 * @param onAnimationEnd transition-end callback; implementations may also invoke it on cancellation.
	 * The resize strategy does not invoke it if no drawer is available.
	 */
	fun animateStateTransition(
		layout: ResponsivePaneLayout,
		toState: PaneState,
		skipAnimation: Boolean,
		callback: ResponsivePaneSlideOffsetCallback? = null,
		onAnimationEnd: () -> Unit
	)

	/**
	 * Returns this strategy's default policy for interaction outside the drawer.
	 */
	fun canOutSideTouch(): Boolean
	/**
	 * Cancels animations and clears cached child views, retaining configurations.
	 */
	fun cleanup()
	/**
	 * Returns the signed width range for [layout].
	 *
	 * The resize strategy returns closed width minus opened width.
	 */
	fun getDragRange(layout: ConstraintLayout): Int
	/**
	 * Returns the drawer managed within [layout], or null when none is found.
	 */
	fun getDrawerLayout(layout: ViewGroup): ViewGroup?
	/**
	 * Returns the target drawer width in pixels for [state] within [layout].
	 *
	 * The resize strategy prefers positive match-constraint width limits over stored presets.
	 */
	fun getDrawerPaneWidth(layout: ConstraintLayout, state: PaneState): Int
	/**
	 * Returns the configuration for [paneState], or an unresolved default if not initialized.
	 */
	fun getPaneConfig(paneState: PaneState): ResponsiveConfig
	/**
	 * Returns the settled pane state maintained by this strategy.
	 */
	fun getPaneState(): PaneState
	/**
	 * Initializes state configurations from resources resolved using [context].
	 */
	fun initPaneConfig(context: Context)
	/**
	 * Resizes the drawer within [parent] by a drag delta.
	 *
	 * @param offset delta in pixels, with positive values widening the drawer; callers handle RTL.
	 * @param callback optional listener for resulting slide progress.
	 */
	fun onPaneDragged(
		parent: ResponsivePaneLayout,
		offset: Float,
		callback: ResponsivePaneSlideOffsetCallback? = null
	)

	/**
	 * Stores [responsiveConfig] for [paneState] without directly requesting a layout.
	 */
	fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig)

	/**
	 * Returns whether gesture-driven resizing is allowed.
	 */
	fun shouldAllowDrag(): Boolean

	/**
	 * Applies responsive drawer constraints to [parent] for the current state.
	 */
	fun updateConstraintConnect(parent: ConstraintLayout)
}
