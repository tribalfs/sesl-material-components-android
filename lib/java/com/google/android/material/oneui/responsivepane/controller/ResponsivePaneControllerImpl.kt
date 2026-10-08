package com.google.android.material.oneui.responsivepane.controller

import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.common.internal.util.withRTLDirection
import com.google.android.material.oneui.responsivepane.ResponsiveDrawerLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.ResponsivePanePolicy.MIN_FLING_VELOCITY
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag
import com.google.android.material.oneui.responsivepane.behavior.ResponsiveDrawerResizeStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneBehaviorStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneSlideOffsetCallback
import com.google.android.material.oneui.responsivepane.helper.concept.ElevationConceptRange
import com.google.android.material.oneui.responsivepane.helper.concept.ResponsivePaneElevationConcept
import com.google.android.material.oneui.responsivepane.helper.concept.toElevationTierConcept
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import kotlin.math.abs

/**
 * Default controller using [ResponsiveDrawerResizeStrategy] for width-based drawer transitions.
 *
 * Call [initialize] before forwarding gesture or layout callbacks. Use the owning layout's
 * public APIs for normal state, configuration, and outside-touch changes.
 */
class ResponsivePaneControllerImpl : ResponsivePaneController, ResponsivePaneTag {

	/**
	 * Internal gesture thresholds used by the default controller.
	 */
	companion object {
		private const val HORIZONTAL_SCROLL_DY_MARGIN = 50
		const val LARGE_SCREEN_WIDTH = 960
	}

	/**
	 * Listener for transition-end notifications. The drawer argument may be null if unavailable.
	 *
	 * End notifications can also occur when a running animation is canceled.
	 */
	override var onStateChangedListener: ((ViewGroup?, PaneState) -> Unit)? = null
	/**
	 * Listener for width changes, receiving a nullable drawer and normalized slide progress.
	 *
	 * Progress is 0 for closed and 1 for opened.
	 */
	override var onSlideOffsetChangedListener: ((ViewGroup?, Float) -> Unit)? = null

	/**
	 * Strategy used for subsequent layout, drag, and state operations.
	 *
	 * Assignment alone does not clean up the old strategy, initialize the new strategy's
	 * configurations, or update outside-touch policy; callers replacing it must manage those steps.
	 */
	var currentBehaviorStrategy: ResponsivePaneBehaviorStrategy = ResponsiveDrawerResizeStrategy()
	/**
	 * Current settled state read directly from [currentBehaviorStrategy].
	 */
	val currentState: PaneState
		get() = currentBehaviorStrategy.getPaneState()
	private lateinit var dragHelper: ViewDragHelper
	/**
	 * Tag used for controller log messages.
	 */
	override val logTag: String = "ResponsivePaneController"
	/**
	 * Whether the most recent intercepted touch-down was within the drawer.
	 */
	var isDrawerTouchDown: Boolean = false
	private var isLargeWidth: Boolean = false
	/**
	 * Whether content outside an opened drawer may receive interaction.
	 */
	override var isOutsideTouchEnabled: Boolean = currentBehaviorStrategy.canOutSideTouch()
		private set
	/**
	 * Drawer width in pixels captured at the latest touch-down, or -1 before any capture.
	 */
	var prev: Int = -1
	private var prevMotionX: Float = -1.0f
	private var prevMotionY: Float = -1.0f
	/**
	 * Cached visual interpolation concept; refreshed by [getElevationConcept] when configurations change.
	 */
	var responsivePaneElevationConcept: ResponsivePaneElevationConcept? = null
	/**
	 * Layout associated by [initialize]; accessing it before initialization throws.
	 */
	lateinit var responsivePaneLayout: ResponsivePaneLayout
	private var shouldChangeState: Boolean = false

	private val dragHelperCallback = object : ViewDragHelper.Callback() {
		override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
			return child.left
		}

		override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
			return child.top
		}

		override fun getViewHorizontalDragRange(child: View): Int {
			return currentBehaviorStrategy.getDragRange(responsivePaneLayout)
		}

		override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
			val drawerLayout = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) ?: return
			if (drawerLayout.parent != responsivePaneLayout) {
				debug("onEdgeDragStarted: Cannot capture view because it is not a direct child of responsivePaneLayout")
				return
			}
			dragHelper.captureChildView(drawerLayout, pointerId)
		}

		override fun tryCaptureView(child: View, pointerId: Int): Boolean {
			return true
		}
	}

	private val internalSlideOffsetChangedListener: ResponsivePaneSlideOffsetCallback =
		{ drawer, offset ->
			if (drawer != null) updateDrawer(drawer, offset)
			onSlideOffsetChangedListener?.invoke(drawer, offset)
		}

	private fun animateOverWidthToOpened(view: ViewGroup) {
		val layout = view as ResponsivePaneLayout
		currentBehaviorStrategy.animateOverWidthToOpened(layout, internalSlideOffsetChangedListener) {
			onStateChangedListener?.invoke(
				currentBehaviorStrategy.getDrawerLayout(view),
				PaneState.OPENED
			)
		}
	}

	/**
	 * Transitions [view] to [toState], forwarding slide and transition-end notifications.
	 *
	 * @param view owning layout, which must be a [ResponsivePaneLayout].
	 * @param skipAnimation whether to jump to the target width without animating.
	 */
	fun animateStateTransition(view: ViewGroup, toState: PaneState, skipAnimation: Boolean) {
		currentBehaviorStrategy.animateStateTransition(
			view as ResponsivePaneLayout,
			toState,
			skipAnimation,
			internalSlideOffsetChangedListener
		) {
			onStateChangedListener?.invoke(currentBehaviorStrategy.getDrawerLayout(view), toState)
		}
	}

	/**
	 * Cancels strategy animations and clears cached views and elevation effects.
	 *
	 * Does not unregister listeners or discard stored pane configurations.
	 */
	override fun cleanup() {
		responsivePaneElevationConcept = null
		currentBehaviorStrategy.cleanup()
	}

	/**
	 * Advances gesture settling for [viewGroup] and schedules further frames when needed.
	 */
	override fun computeScroll(viewGroup: ViewGroup) {
		if (dragHelper.continueSettling(true)) {
			responsivePaneLayout.postInvalidateOnAnimation()
		}
	}

	/**
	 * Returns the strategy's current settled pane state.
	 */
	override fun getCurrentPaneState(): PaneState {
		return currentState
	}

	/**
	 * Returns or regenerates the visual interpolation concept from both state configurations.
	 *
	 * @return null if either configuration has unresolved negative elevation.
	 */
	fun getElevationConcept(): ResponsivePaneElevationConcept? {
		val openConfig = getPaneConfig(PaneState.OPENED)
		val closeConfig = getPaneConfig(PaneState.CLOSED)
		if (closeConfig.elevation < 0.0f || openConfig.elevation < 0.0f) {
			warn("The Config is not complete yet close[$closeConfig], open[$openConfig]")
			return null
		}
		val minTier = closeConfig.toElevationTierConcept()
		val maxTier = openConfig.toElevationTierConcept()
		val currentMin = responsivePaneElevationConcept?.conceptRange?.minConcept
		val currentMax = responsivePaneElevationConcept?.conceptRange?.maxConcept
		val changed = minTier != currentMin || maxTier != currentMax
		if (responsivePaneElevationConcept == null || changed) {
			responsivePaneElevationConcept =
				ResponsivePaneElevationConcept(ElevationConceptRange(minTier, maxTier))
			if (changed) {
				debug("conceptChanged min($currentMin->$minTier), max($currentMax->$maxTier)")
			}
			debug("generate New Concept=$responsivePaneElevationConcept")
		}
		return responsivePaneElevationConcept
	}

	/**
	 * Returns the visual and width configuration for [paneState].
	 */
	override fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
		return currentBehaviorStrategy.getPaneConfig(paneState)
	}

	/**
	 * Associates this controller with [responsivePaneLayout] and initializes gestures and presets.
	 */
	override fun initialize(responsivePaneLayout: ResponsivePaneLayout) {
		debug("updateConstraintConnect layout=$responsivePaneLayout")
		this.responsivePaneLayout = responsivePaneLayout
		val helper = ViewDragHelper.create(responsivePaneLayout, 0.5f, dragHelperCallback)
		helper.minVelocity = MIN_FLING_VELOCITY * responsivePaneLayout.resources.displayMetrics.density
		this.dragHelper = helper
		this.isLargeWidth =
			responsivePaneLayout.resources.configuration.screenWidthDp >= LARGE_SCREEN_WIDTH
		this.shouldChangeState = true
		currentBehaviorStrategy.initPaneConfig(responsivePaneLayout.context)
		updateDrawerState(responsivePaneLayout)
		updateResponsiveLayout(responsivePaneLayout)
	}

	/**
	 * Updates the associated [responsivePaneLayout] for [newConfig].
	 */
	override fun onConfigurationChanged(
		responsivePaneLayout: ResponsivePaneLayout,
		newConfig: Configuration
	) {
		debug("onConfigurationChanged responsivePaneLayout=$responsivePaneLayout, newConfig=$newConfig")
		val large = newConfig.screenWidthDp >= LARGE_SCREEN_WIDTH
		if (isLargeWidth != large) {
			updateDrawerState(responsivePaneLayout)
		}
		isLargeWidth = large
	}

	/**
	 * Evaluates interception of [ev] within [viewGroup].
	 *
	 * @return true to intercept, false to decline, or null to defer to the layout superclass.
	 */
	override fun onInterceptTouchEvent(viewGroup: ViewGroup, ev: MotionEvent): Boolean? {
		val drawer = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout)
		if (drawer == null || drawer.visibility != View.VISIBLE) return false
		if (!currentBehaviorStrategy.shouldAllowDrag()) {
			dragHelper.cancel()
			return null
		}

		val x = ev.x
		val y = ev.y
		var interceptDrag = false
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				dragHelper.abort()
				prevMotionX = x
				prevMotionY = y
				isDrawerTouchDown = dragHelper.isViewUnder(drawer, x.toInt(), y.toInt())
				if (!isDrawerTouchDown && currentState == PaneState.OPENED && !isOutsideTouchEnabled) {
					dragHelper.cancel()
					dragHelper.abort()
					setPaneState(viewGroup, PaneState.CLOSED, true)
					return true
				}
			}

			MotionEvent.ACTION_MOVE -> {
				val dx = x - prevMotionX
				val dy = y - prevMotionY
				val horizontalScroll = abs(dx) > abs(dy) + HORIZONTAL_SCROLL_DY_MARGIN
				if (isDrawerTouchDown && abs(dx) > dragHelper.touchSlop && horizontalScroll) {
					prevMotionX = x
					prevMotionY = y
					interceptDrag = true
				}
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				dragHelper.cancel()
				return false
			}
		}
		return dragHelper.shouldInterceptTouchEvent(ev) || interceptDrag
	}

	/**
	 * Updates gesture tracking after [viewGroup] is laid out.
	 *
	 * @param changed whether the layout bounds changed.
	 * @param left left bound in parent coordinates.
	 * @param top top bound in parent coordinates.
	 * @param right right bound in parent coordinates.
	 * @param bottom bottom bound in parent coordinates.
	 */
	override fun onLayout(
		viewGroup: ViewGroup,
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		val rtl = viewGroup.layoutDirection == View.LAYOUT_DIRECTION_RTL
		dragHelper.setEdgeTrackingEnabled(if (rtl) ViewDragHelper.EDGE_RIGHT else ViewDragHelper.EDGE_LEFT)
	}

	/**
	 * Resizes the associated drawer by [slideOffset] pixels.
	 *
	 * Despite its name, the argument is a width delta, not normalized progress. Positive values
	 * widen the drawer; the caller must account for RTL. Does nothing if no responsive drawer exists.
	 */
	fun onPaneDragged(slideOffset: Float) {
		if (currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) !is ResponsiveDrawerLayout) return
		currentBehaviorStrategy.onPaneDragged(
			responsivePaneLayout,
			slideOffset,
			internalSlideOffsetChangedListener
		)
	}

	/**
	 * Processes [event] within [viewGroup].
	 *
	 * @return true if consumed, false if unconsumed, or null to defer to the layout superclass.
	 */
	override fun onTouchEvent(viewGroup: ViewGroup, event: MotionEvent): Boolean? {
		val x = event.x
		dragHelper.processTouchEvent(event)
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				prevMotionX = x
				val drawer = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout)
				prev = drawer?.width ?: 0
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> snapToDrawerIfNeed()
			MotionEvent.ACTION_MOVE -> {
				onPaneDragged((x - prevMotionX).withRTLDirection(viewGroup))
				prevMotionX = x
			}
		}
		return false
	}

	/**
	 * Sets the controller's outside-touch policy to [enabled].
	 *
	 * Use the layout setter to also refresh accessibility and back handling.
	 */
	override fun setOutsideTouchEnabled(enabled: Boolean) {
		isOutsideTouchEnabled = enabled
	}

	/**
	 * Stores [responsiveConfig] for [paneState] on [viewGroup].
	 *
	 * If the state is current, reapplies it without animation.
	 */
	override fun setPaneConfig(
		viewGroup: ResponsivePaneLayout,
		paneState: PaneState,
		responsiveConfig: ResponsiveConfig
	) {
		currentBehaviorStrategy.setPaneConfig(paneState, responsiveConfig)
		if (getCurrentPaneState() == paneState) {
			debug("setPaneConfig: current Config is changed. update pane")
			setPaneState(viewGroup, paneState, false)
		}
	}

	/**
	 * Requests [state] for [viewGroup].
	 *
	 * @param animate whether to animate the transition.
	 */
	override fun setPaneState(viewGroup: ViewGroup, state: PaneState, animate: Boolean) {
		animateStateTransition(viewGroup, state, !animate)
	}

	private fun snapToDrawerIfNeed() {
		val drawer = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) ?: return
		val openWidth =
			currentBehaviorStrategy.getDrawerPaneWidth(responsivePaneLayout, PaneState.OPENED)
		val closedWidth =
			currentBehaviorStrategy.getDrawerPaneWidth(responsivePaneLayout, PaneState.CLOSED)
		if (drawer.width > openWidth) {
			animateOverWidthToOpened(responsivePaneLayout)
			return
		}
		val threshold = ((openWidth - closedWidth) * 0.5f) + closedWidth
		if (drawer.width < threshold) {
			animateStateTransition(responsivePaneLayout, PaneState.CLOSED, drawer.width == closedWidth)
		} else {
			animateStateTransition(responsivePaneLayout, PaneState.OPENED, drawer.width == openWidth)
		}
	}

	private fun updateDrawer(drawer: ResponsiveDrawerLayout, slideRatio: Float) {
		getElevationConcept()?.applyConcept(drawer, slideRatio)
	}

	private fun updateDrawerState(view: ResponsivePaneLayout) {
		val defaultOpen =
			view.resources.getBoolean(R.bool.sesl_responsive_pane_navigation_drawer_default_open)
		debug("updateDrawer State:$defaultOpen")
		setPaneState(view, if (defaultOpen) PaneState.OPENED else PaneState.CLOSED, false)
	}

	/**
	 * Reapplies the current strategy's constraints to [view].
	 */
	override fun updateResponsiveLayout(view: ResponsivePaneLayout) {
		currentBehaviorStrategy.updateConstraintConnect(view)
	}
}
