package com.google.android.material.oneui.responsivepane

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.customview.widget.Openable
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.info
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneBehaviorStrategy
import com.google.android.material.oneui.responsivepane.controller.ResponsivePaneControllerImpl
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import com.google.android.material.oneui.responsivepane.util.ResponsivePaneCallbackNotifier

/**
 * An adaptive Samsung One UI container layout extending [ConstraintLayout] that manages side navigation drawers
 * and content panes, supporting responsive pane states (OPENED, CLOSED) across tablet, foldable, and desktop screens.
 *
 * Integrates with One UI elevation concepts, back handling via [OnBackInvokedDispatcher], and
 * drag gestures. Use a [ResponsiveDrawerLayout] with ID `responsive_pane_drawer` and a content
 * container with ID `responsive_pane_content`. The drawer must be a direct child for drag capture.
 *
 * Use [ResponsivePaneListener] to observe transitions, including those initiated by gestures.
 *
 * @param context context used to resolve resources and create child behavior.
 * @param attrs optional XML attributes passed to [ConstraintLayout].
 * @param defStyleAttr default style attribute passed to [ConstraintLayout].
 */
class ResponsivePaneLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Openable, ResponsivePaneTag {

	/**
	 * Receives state and slide notifications for the drawer currently managed by the layout.
	 */
	interface ResponsivePaneListener {
		/**
		 * Called when a transition to the closed state ends.
		 *
		 * @param drawer drawer associated with the transition.
		 */
		fun onDrawerClosed(drawer: View)
		/**
		 * Called when a transition to the opened state ends.
		 *
		 * @param drawer drawer associated with the transition.
		 */
		fun onDrawerOpened(drawer: View)
		/**
		 * Called as the drawer width changes.
		 *
		 * @param drawer drawer being resized.
		 * @param slideRatio normalized progress: 0 is closed and 1 is opened.
		 */
		fun onDrawerSlide(drawer: View, slideRatio: Float)
	}

	private var actionModeBackInvoked: OnBackInvokedCallback? = null
	/**
	 * Controller owned and initialized by this layout.
	 *
	 * Prefer the layout APIs; replacing its controller listeners disconnects the layout's own notifications, accessibility updates, and back handling.
	 */
	internal val controller = ResponsivePaneControllerImpl()
	private var isFirstLayout: Boolean = true
	/**
	 * Tag used for responsive pane log messages.
	 */
	override val logTag: String = "ResponsivePaneLayout"
	private val responsivePaneCallbackNotifier = ResponsivePaneCallbackNotifier(mutableListOf())

	init {
		setupControllerCallbacks()
		controller.initialize(this)
	}

	private fun findResponsiveDrawerLayout(): ResponsiveDrawerLayout? {
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child is ResponsiveDrawerLayout) {
				return child
			}
		}
		return null
	}

	private fun getPaneState(): PaneState {
		return controller.getCurrentPaneState()
	}

	private fun isModalOpenCase(drawer: ResponsiveDrawerLayout?): Boolean {
		return drawer != null && drawer.isVisible && isOpen && !isOutsideTouchEnabled()
	}

	private fun onSlideOffsetChanged(viewGroup: ViewGroup?, slideRatio: Float) {
		if (viewGroup != null) {
			responsivePaneCallbackNotifier.onDrawerSlide(viewGroup, slideRatio)
		}
	}

	private fun onStateChanged(viewGroup: ViewGroup?, state: PaneState) {
		if (viewGroup != null) {
			if (state == PaneState.OPENED) {
				responsivePaneCallbackNotifier.onDrawerOpened(viewGroup)
			} else {
				responsivePaneCallbackNotifier.onDrawerClosed(viewGroup)
			}
		}
		updateChildrenImportantForAccessibility()
	}

	private fun registerOnBackInvokedCallback(currentDispatcher: OnBackInvokedDispatcher) {
		if (Build.VERSION.SDK_INT >= 33) {
			debug("registerOnBackInvokedCallback currentDispatcher=$currentDispatcher")
			if (actionModeBackInvoked == null) {
				actionModeBackInvoked = OnBackInvokedCallback {
					debug("actionModeBackInvoked isOpen=$isOpen")
					if (isOpen) {
						close()
					}
					unregisterOnBackInvokedCallback(currentDispatcher)
				}
			}
			actionModeBackInvoked?.let {
				currentDispatcher.registerOnBackInvokedCallback(1_000_000, it)
			}
		}
	}

	/**
	 * Requests a drawer state transition.
	 *
	 * @param state target state.
	 * @param animate whether to animate the width change; defaults to true.
	 */
	private fun setPaneState(state: PaneState, animate: Boolean = true) {
		controller.setPaneState(this, state, animate)
	}

	private fun setupControllerCallbacks() {
		controller.onStateChangedListener = { viewGroup, state ->
			updateBackInvokedCallbackState()
			onStateChanged(viewGroup, state)
		}
		controller.onSlideOffsetChangedListener = { viewGroup, ratio ->
			onSlideOffsetChanged(viewGroup, ratio)
		}
	}

	private fun unregisterOnBackInvokedCallback(currentDispatcher: OnBackInvokedDispatcher) {
		if (Build.VERSION.SDK_INT >= 33) {
			val callback = actionModeBackInvoked ?: return
			debug("unregisterOnBackInvokedCallback currentDispatcher=$currentDispatcher")
			currentDispatcher.unregisterOnBackInvokedCallback(callback)
			actionModeBackInvoked = null
		}
	}

	private fun updateBackInvokedCallbackState() {
		if (Build.VERSION.SDK_INT >= 33) {
			val dispatcher = findOnBackInvokedDispatcher()
			if (dispatcher == null) {
				info("updateBackInvokedCallbackState backInvoked=null, parent=$parent")
			} else if (isModalOpenCase(findResponsiveDrawerLayout())) {
				registerOnBackInvokedCallback(dispatcher)
			} else {
				unregisterOnBackInvokedCallback(dispatcher)
			}
		}
	}

	private fun updateChildrenImportantForAccessibility() {
		val drawer = findResponsiveDrawerLayout()
		if (!isModalOpenCase(drawer)) {
			for (i in 0 until childCount) {
				getChildAt(i).importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO
			}
			return
		}
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child == drawer) {
				child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
			} else {
				child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
			}
		}
	}

	/**
	 * Adds focusable views, restricting focus to the drawer while it is opened in modal mode.
	 *
	 * @param views collection receiving focusable views.
	 * @param direction requested focus direction.
	 * @param focusableMode framework focusability filter.
	 */
	override fun addFocusables(views: ArrayList<View>, direction: Int, focusableMode: Int) {
		val drawer = findResponsiveDrawerLayout()
		if (drawer == null || !isModalOpenCase(drawer)) {
			super.addFocusables(views, direction, focusableMode)
		} else {
			if (descendantFocusability == FOCUS_BLOCK_DESCENDANTS) {
				return
			}
			drawer.addFocusables(views, direction, focusableMode)
		}
	}

	/**
	 * Registers [listener] for drawer state and slide changes. Duplicate registrations are ignored.
	 */
	fun addResponsivePaneListener(listener: ResponsivePaneListener) {
		debug("addResponsivePaneListener listener=$listener, listeners=$responsivePaneCallbackNotifier")
		if (!responsivePaneCallbackNotifier.contains(listener)) {
			responsivePaneCallbackNotifier.add(listener)
		}
	}

	/**
	 * Adds a child, clearing cached drawer behavior and applying elevation for drawer children.
	 *
	 * @param child view to add.
	 * @param index insertion index, or -1 to append.
	 * @param params layout parameters for the child.
	 */
	override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
		if (child is ResponsiveDrawerLayout) {
			controller.cleanup()
			controller.getElevationConcept()?.applyConcept(child, if (isOpen) 1.0f else 0.0f)
		}
		super.addView(child, index, params)
	}

	/**
	 * Closes the drawer with animation by calling [close] with `animate = true`.
	 */
	override fun close() {
		close(true)
	}

	/**
	 * Advances controller settling and schedules another animation frame when needed.
	 */
	override fun computeScroll() {
		controller.computeScroll(this)
	}

	/**
	 * Returns the configuration currently stored for [paneState].
	 */
	fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
		return controller.getPaneConfig(paneState)
	}

	/**
	 * Returns whether the controller reports [PaneState.OPENED].
	 *
	 * During an animation, the reported state can remain the previous state until the transition ends.
	 */
	override fun isOpen(): Boolean = getPaneState() == PaneState.OPENED

	/**
	 * Returns whether interaction with content outside an opened drawer is enabled.
	 */
	fun isOutsideTouchEnabled(): Boolean = controller.isOutsideTouchEnabled

	/**
	 * Refreshes accessibility and back handling after attachment.
	 */
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		updateChildrenImportantForAccessibility()
		updateBackInvokedCallbackState()
	}

	/**
	 * Updates responsive behavior for [newConfig].
	 */
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		controller.onConfigurationChanged(this, newConfig)
	}

	/**
	 * Unregisters back handling and releases cached behavior on detachment.
	 */
	override fun onDetachedFromWindow() {
		if (Build.VERSION.SDK_INT >= 33) {
			findOnBackInvokedDispatcher()?.let { unregisterOnBackInvokedCallback(it) }
		}
		controller.cleanup()
		super.onDetachedFromWindow()
	}

	/**
	 * Asks the controller whether to intercept [event], falling back to the superclass if undecided.
	 *
	 * @return true when this layout intercepts the event.
	 */
	override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
		val result = controller.onInterceptTouchEvent(this, event)
		return result ?: super.onInterceptTouchEvent(event)
	}

	/**
	 * Lays out children and updates controller edge tracking.
	 *
	 * @param changed whether the layout bounds changed.
	 * @param left left bound in parent coordinates.
	 * @param top top bound in parent coordinates.
	 * @param right right bound in parent coordinates.
	 * @param bottom bottom bound in parent coordinates.
	 */
	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		controller.onLayout(this, changed, left, top, right, bottom)
	}

	/**
	 * Measures children and applies responsive constraints on the first measurement.
	 *
	 * @param widthMeasureSpec framework width measurement specification.
	 * @param heightMeasureSpec framework height measurement specification.
	 */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		if (isFirstLayout) {
			controller.updateResponsiveLayout(this)
			isFirstLayout = false
		}
	}

	/**
	 * Passes [event] to the controller, falling back to the superclass if undecided.
	 *
	 * @return whether the event was consumed.
	 */
	override fun onTouchEvent(event: MotionEvent): Boolean {
		val result = controller.onTouchEvent(this, event)
		return result ?: super.onTouchEvent(event)
	}

	/**
	 * Opens the drawer with animation by calling [open] with `animate = true`.
	 */
	override fun open() {
		open(true)
	}

	/**
	 * Unregisters [listener]; removing an unregistered listener has no effect.
	 */
	fun removeResponsivePaneListener(listener: ResponsivePaneListener) {
		debug("removeResponsivePaneListener listener=$listener, listeners=$responsivePaneCallbackNotifier")
		responsivePaneCallbackNotifier.remove(listener)
	}

	/**
	 * Controls interaction with content outside an opened drawer.
	 *
	 * When disabled, the opened drawer is modal: focus and accessibility are restricted to it,
	 * back handling closes it, and an outside touch requests closure. Updates those policies
	 * immediately.
	 *
	 * @param enabled true to allow outside interaction, false for modal behavior.
	 */
	fun setOutsideTouchEnabled(enabled: Boolean) {
		debug("setOutsideTouchEnabled enabled=$enabled")
		controller.setOutsideTouchEnabled(enabled)
		updateBackInvokedCallbackState()
		updateChildrenImportantForAccessibility()
	}

	/**
	 * Stores a configuration for [paneState], applying it without animation if that state is current.
	 *
	 * @param paneState state to configure.
	 * @param responsiveConfig width in pixels and elevation, blur, and background alpha settings.
	 */
	fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig) {
		controller.setPaneConfig(this, paneState, responsiveConfig)
	}

	/**
	 * Reapplies responsive constraints for the current pane state.
	 */
	fun updateResponsiveLayout() {
		debug("updateResponsiveLayout")
		controller.updateResponsiveLayout(this)
	}

	/**
	 * Requests [PaneState.CLOSED].
	 *
	 * @param animate whether to animate the width change.
	 */
	fun close(animate: Boolean) {
		debug("close animate=$animate")
		setPaneState(PaneState.CLOSED, animate)
	}

	/**
	 * Requests [PaneState.OPENED].
	 *
	 * @param animate whether to animate the width change.
	 */
	fun open(animate: Boolean) {
		debug("open animate=$animate")
		setPaneState(PaneState.OPENED, animate)
	}

	//custom
	/**
	 * Swaps the pane behavior strategy driving width, drag and elevation.
	 *
	 * @param strategy the strategy to delegate pane behavior to from now on.
	 */
	fun setStrategy(strategy: ResponsivePaneBehaviorStrategy) {
		controller.currentBehaviorStrategy = strategy
	}
}
