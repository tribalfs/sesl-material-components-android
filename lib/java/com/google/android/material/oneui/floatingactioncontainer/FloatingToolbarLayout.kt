package com.google.android.material.oneui.floatingactioncontainer

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Property
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.view.menu.ActionMenuItemViewBadgedWrapper
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.ViewStubCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.reflect.SeslBaseReflector
import com.google.android.material.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.internal.ThemeEnforcement
import com.google.android.material.oneui.common.internal.info
import com.google.android.material.oneui.common.internal.warn
import kotlin.math.abs
import android.view.ViewGroup.LayoutParams as ViewGroupLayoutParams

/**
 * Floating toolbar layout container integrating a [Toolbar] and optional action mode bar.
 *
 * Manages item projection backgrounds, title alpha transitions synchronized with
 * [AppBarLayout] states, and selection view animations.
 *
 * @param context The Context the view is running in.
 * @param attrs The attributes of the XML tag inflating the view.
 * @param defStyleAttr Default style attribute for styling.
 */
open class FloatingToolbarLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FloatingTopLayout(context, attrs, defStyleAttr) {

	companion object {
		private const val TAG = "FloatingTbLayout"
		private const val ANIM_SELECTION_VIEW_ALPHA_SHOW_DURATION = 150L
		private const val ANIM_SELECTION_VIEW_ALPHA_HIDE_DURATION = 50L
		private const val TITLE_ALPHA_ANIM_SHOW_DURATION = 150L
		private const val TITLE_ALPHA_ANIM_HIDE_DURATION = 100L
		private const val MAX_CHILD_POSITION_CHECKING_SIZE = 3
		private const val AVAIL_RECT_TAG_TOP = "FloatingToolbarLayout_availTop"
		private const val AVAIL_RECT_TAG_BOTTOM = "FloatingToolbarLayout_availBottom"

		const val APP_BAR_STATE_EXPANDED = 1
		const val APP_BAR_STATE_COLLAPSED = 2
		const val APP_BAR_STATE_HIDE = 4

		/**
		 * Animates a selection view into visibility.
		 *
		 * @param view Target selection view.
		 * @param startAction Optional action to execute before starting alpha animation.
		 */
		@JvmStatic
		fun showFloatingSelectionView(view: View, startAction: (() -> Unit)? = null) {
			view.animate()
				.alpha(1.0f)
				.setDuration(ANIM_SELECTION_VIEW_ALPHA_SHOW_DURATION)
				.withStartAction {
					if (startAction != null) {
						startAction.invoke()
					} else {
						view.visibility = VISIBLE
					}
				}
				.start()
		}

		/**
		 * Animates a selection view to hidden state.
		 *
		 * @param view Target selection view.
		 * @param endAction Optional action to execute after alpha animation completes.
		 */
		@JvmStatic
		fun hideFloatingSelectionView(view: View, endAction: (() -> Unit)? = null) {
			view.animate()
				.alpha(0.0f)
				.setDuration(ANIM_SELECTION_VIEW_ALPHA_HIDE_DURATION)
				.withEndAction {
					if (endAction != null) {
						endAction.invoke()
					} else {
						view.visibility = INVISIBLE
					}
				}
				.start()
		}
	}

	override val logTag: String = TAG

	/** The child [Toolbar] view managed by this layout. */
	var toolbar: Toolbar? = null
		get() {
			for (child in children) {
				if (child is Toolbar && child.isVisible) {
					field = child
					return child
				}
			}
			field?.let {
				if (it.parent == this) return it
			}
			for (child in children) {
				if (child is Toolbar) {
					field = child
					return child
				}
			}
			return null
		}

	/** The [ViewStubCompat] used to inflate the action mode bar. */
	private var actionModeViewStub: ViewStubCompat? = null

	/** The inflated [ActionBarContextView] for action mode support. */
	private var actionModeBar: ActionBarContextView? = null
	private var isActionModeGlobalLayoutListenerAdded: Boolean = false

	/** List of view IDs registered as custom title views whose alpha is synchronized. */
	private val toolbarTitleViCustomViewIds: MutableList<Int> = ArrayList()

	private var actionForAfterAddUseTransition: Boolean = false
	private var firstEnableToolbarItemBackgroundTransition: Boolean = false
	private var actionForAfterAddShowBackground: Boolean = false
	private var firstShowFloatingToolbarItemBackground: Boolean = false

	/** `true` if action mode is currently active and visible. */
	var isActionMode: Boolean = false

	private var defaultRecyclerViewHoverTopPadding: Int = -1
	private var forcedTopFadingEdge: Int = 0
	private var appBarVisibleHeight: Int = 0
	private var isFirstLayout: Boolean = true

	private var floatingAwarePositions: MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1, -1)
	private var toolbarChildrenPosition: MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1, -1)
	private var toolbarChildCount: Int = 0

	private var titleAlphaValueAnimator: ObjectAnimator = ObjectAnimator()

	private val titleAlphaAnimProperty =
		object : Property<Toolbar, Float>(Float::class.java, "titleAlphaAnimProperty") {
			override fun get(toolbar: Toolbar): Float =
				toolbar.titleTextView?.alpha ?: 0.0f

			override fun set(toolbar: Toolbar, value: Float) {
				setAlphaForToolbarTitleViGroup(value)
			}
		}

	private val defaultFloatingAware by lazy { FloatingToolbarAware(this) }

	/**
	 * Custom [FloatingAware] implementation providing reference views and bounds insets for item projection.
	 * Ensure that the relevant reference views are already visible when invoking this.
	 * */
	override var floatingAware: FloatingAware?
		get() = super.floatingAware
		set(value) {
			checkActionMode()
			if (value == null) {
				info("Use default FloatingToolbarAware FloatingAware")
			} else {
				info("Use custom CustomAware(Toolbar) FloatingAware")
			}
			super.floatingAware = value ?: defaultFloatingAware
		}

	@Deprecated("Use FloatingAware type")
	enum class ItemBackgroundType {
		START_FIRST,
		START_SECOND,
		END_FIRST
	}

	@Deprecated("Use [DefaultToolbarFloatingAware] instead", replaceWith = ReplaceWith("FloatingToolbarAware"))
	open class DefaultFloatingAware(floatingToolbarLayout: FloatingToolbarLayout) : FloatingToolbarAware(floatingToolbarLayout)

	/**
	 * CoordinatorLayout Behavior for [FloatingToolbarLayout] managing scroll events and title alpha transitions.
	 *
	 * @param T The type of [FloatingToolbarLayout].
	 * @param context The Context the behavior is running in.
	 * @param attrs The attributes of the XML tag inflating the behavior.
	 */
	class FloatingToolbarBehavior<T : FloatingToolbarLayout> @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : FloatingTopBehavior<T>(context, attrs) {

		/**
		 * Handles layout positioning and initial app bar state synchronization.
		 *
		 * @param parent CoordinatorLayout parent view.
		 * @param child Target [FloatingToolbarLayout] child view.
		 * @param layoutDirection Current layout direction.
		 * @return `true` if handled by behavior, `false` otherwise.
		 */
		override fun onLayoutChild(
			parent: CoordinatorLayout,
			child: T,
			layoutDirection: Int,
		): Boolean {
			if (isFirstLayoutChild) {
				val abl = child.getAppBarLayout()
				if (abl != null) {
					child.updateTitleAlphaForCurrentOffset(abl, true)
					child.toolbar?.seslSetEatingTouchOnly((abl.seslGetCurrentAppBarState() and APP_BAR_STATE_HIDE) == 0)
				}
			}
			updateStateToHideCondition(parent, child)
			return super.onLayoutChild(parent, child, layoutDirection)
		}

		/**
		 * Evaluates scroll conditions and returns `false` to prevent direct nested scroll interception.
		 *
		 * @param coordinatorLayout CoordinatorLayout parent.
		 * @param child Target [FloatingToolbarLayout].
		 * @param directTargetChild Direct target child view.
		 * @param target Target scroll view.
		 * @param axes Scroll axes.
		 * @param type Input type.
		 * @return `false`.
		 */
		override fun onStartNestedScroll(
			coordinatorLayout: CoordinatorLayout,
			child: T,
			directTargetChild: View,
			target: View,
			axes: Int,
			type: Int,
		): Boolean {
			updateStateToHideCondition(coordinatorLayout, child)
			return false
		}

		/**
		 * Updates app bar's internal allow-hide condition based on target scrollable view.
		 *
		 * @param parent CoordinatorLayout parent.
		 * @param child Target [FloatingToolbarLayout].
		 */
		private fun updateStateToHideCondition(parent: CoordinatorLayout, child: T) {
			val dependencies = parent.getDependencies(child)
			val appBarLayout = with(child){ dependencies.getAppBarLayout() }
			if (appBarLayout != null) {
				var recyclerView: View? = child.getRecyclerView()
				if (recyclerView == null) {
					recyclerView = child.getNestedScrollView()
				}
				if (!appBarLayout.useFloatingToolbar() || recyclerView == null) {
					return
				}
				if (child.isStateToHideCondition(recyclerView, parent)) {
					appBarLayout.seslInternalSetAllowStateToHide(true)
				} else {
					appBarLayout.seslInternalSetAllowStateToHide(false)
					info("Force disable floating appbar because of it is no scrollable")
				}
			}
		}

		/**
		 * Reacts to app bar state changes to animate title alpha and set eating touch state on toolbar.
		 *
		 * @param oldState Previous app bar state.
		 * @param newState New app bar state.
		 * @param child Target [FloatingToolbarLayout].
		 */
		override fun onAppBarStateChanged(oldState: Int, newState: Int, child: T) {
			super.onAppBarStateChanged(oldState, newState, child)
			startTitleAnimationForAppBarStateChanged(child, oldState, newState)
			child.toolbar?.seslSetEatingTouchOnly((newState and APP_BAR_STATE_HIDE) == 0)
		}

		/**
		 * Triggers title alpha animation based on app bar state transition.
		 *
		 * @param child Target [FloatingToolbarLayout].
		 * @param oldState Previous state.
		 * @param newState New state.
		 */
		private fun startTitleAnimationForAppBarStateChanged(
			child: T,
			oldState: Int,
			newState: Int,
		) {
			if (newState == APP_BAR_STATE_HIDE) {
				child.startTitleAlphaAnimation(show = false, immediately = true)
				return
			}
			if ((newState and APP_BAR_STATE_HIDE) != 0) {
				if ((oldState and APP_BAR_STATE_COLLAPSED) != 0) {
					child.startTitleAlphaAnimation(show = false, immediately = false)
				}
			} else if (newState == APP_BAR_STATE_COLLAPSED) {
				child.startTitleAlphaAnimation(show = true, immediately = false)
			}
		}
	}

	init {
		LayoutInflater.from(context)
			.inflate(R.layout.sesl_floating_appbar_action_mode_view_stub, this, true)
		actionModeViewStub = findViewById(R.id.action_mode_bar_stub)

		val a = ThemeEnforcement.obtainStyledAttributes(
			context,
			attrs,
			R.styleable.FloatingToolbarLayout,
			defStyleAttr,
			0
		)
		if (a.hasValue(R.styleable.FloatingToolbarLayout_seslEnableToolbarItemTransition)) {
			actionForAfterAddUseTransition = true
			firstEnableToolbarItemBackgroundTransition =
				a.getBoolean(
					R.styleable.FloatingToolbarLayout_seslEnableToolbarItemTransition,
					false
				)
		}
		if (a.hasValue(R.styleable.FloatingToolbarLayout_seslShowToolbarItemBackground)) {
			actionForAfterAddShowBackground = true
			firstShowFloatingToolbarItemBackground =
				a.getBoolean(R.styleable.FloatingToolbarLayout_seslShowToolbarItemBackground, false)
		}
		a.recycle()
	}

	/**
	 * Returns the CoordinatorLayout behavior managing scroll and app bar offset events.
	 *
	 * @return A new instance of [FloatingToolbarBehavior].
	 */
	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return FloatingToolbarBehavior<FloatingToolbarLayout>(context, attrs)
	}

	/**
	 * Returns the inflated action mode bar view if available.
	 *
	 * @return The [ActionBarContextView] or `null`.
	 */
	fun getActionModeBarView(): ActionBarContextView? {
		if (actionModeBar == null) {
			actionModeBar = findViewById(R.id.action_mode_bar)
		}
		return actionModeBar
	}

	/** Checks whether action mode is currently visible. */
	private fun checkActionMode() {
		isActionMode = actionModeBar?.isVisible == true
	}

	/** Brings the action mode view stub to front if present. */
	private fun ensureViewStub() {
		actionModeViewStub?.apply {
			bringToFront()
			invalidate()
		}
	}

	/** Finds the scrolling child view inside a CoordinatorLayout. */
	private fun getScrollingView(coordinatorLayout: CoordinatorLayout): View? {
		for (view in coordinatorLayout.children) {
			val lp = view.layoutParams as? CoordinatorLayout.LayoutParams
			if (lp?.behavior is AppBarLayout.ScrollingViewBehavior) {
				return view
			}
		}
		return null
	}

	/** Calculates current bounds coordinates for toolbar reference views. */
	private fun getToolbarChildPosition(): MutableList<Int> {
		val aware = floatingAware
		val ref1 = aware?.getReferenceView(FloatingAware.PositionType.START_FIRST)
		val ref2 = aware?.getReferenceView(FloatingAware.PositionType.START_SECOND)
		val ref3 = aware?.getReferenceView(FloatingAware.PositionType.END_FIRST)
		val list = mutableListOf(-1, -1, -1, -1, -1, -1)
		list[0] = ref1?.left ?: -1
		list[1] = ref1?.right ?: -1
		list[2] = ref2?.left ?: -1
		list[3] = ref2?.right ?: -1
		list[4] = ref3?.left ?: -1
		list[5] = ref3?.right ?: -1
		return list
	}

	/** Triggers projection view item animation if reference view positions changed. */
	private fun startProjectionViewItemAnimationIfAwareChanged() {
		val childPos = getToolbarChildPosition()
		if (childPos != floatingAwarePositions) {
			projectionView.startProjectionViewItemAnimation(true)
			floatingAwarePositions = childPos
		}
	}

	/** Triggers projection view item animation if toolbar child count or positions changed. */
	private fun startProjectionViewItemAnimationIfToolbarChanged() {
		val tb = toolbar
		if (tb != null) {
			if (toolbarChildCount != tb.childCount) {
				projectionView.startProjectionViewItemAnimation(true)
				toolbarChildCount = tb.childCount
				return
			}
			val currentChildrenPos = mutableListOf(-1, -1, -1, -1, -1, -1)
			val iMin = minOf(MAX_CHILD_POSITION_CHECKING_SIZE, tb.childCount)
			for (i in 0 until iMin) {
				val childAt = tb.getChildAt(i)
				val i5 = i * 2
				currentChildrenPos[i5] = childAt.left
				currentChildrenPos[i5 + 1] = childAt.right
			}
			if (toolbarChildrenPosition == currentChildrenPos) {
				return
			}
			projectionView.startProjectionViewItemAnimation(true)
			toolbarChildrenPosition = currentChildrenPos
		}
	}

	/**
	 * Handles adding child views, ensuring [Toolbar] configuration and projection view setup.
	 *
	 * @param child Child view being added.
	 * @param index Insertion index.
	 * @param params Layout parameters.
	 */
	override fun addView(child: View?, index: Int, params: ViewGroupLayoutParams?) {
		if (child !is Toolbar) {
			super.addView(child, index, params)
			return
		}
		val tb = child
		toolbar = tb
		if (actionForAfterAddUseTransition) {
			enableToolbarItemBackgroundTransition(firstEnableToolbarItemBackgroundTransition)
		}
		if (actionForAfterAddShowBackground) {
			showFloatingItemBackground(firstShowFloatingToolbarItemBackground, animate = false)
		}
		tb.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
			override fun onPreDraw(): Boolean {
				tb.viewTreeObserver.removeOnPreDrawListener(this)
				return true
			}
		})
		super.addView(child, index, params)
		floatingAware = null
		projectionView.startProjectionViewAlphaAnimation(0.0f, immediately = true)
		addFloatingBackgroundAnimatorListener(object : Animator.AnimatorListener {
			override fun onAnimationStart(animation: Animator) {}
			override fun onAnimationEnd(animation: Animator) {
				toolbar?.seslSetEatingHover(alpha != 1.0f)
				getActionModeBarView()?.seslSetEatingTouchOnly(alpha != 1.0f)
			}

			override fun onAnimationCancel(animation: Animator) {}
			override fun onAnimationRepeat(animation: Animator) {}
		})
	}

	/** Empty override as toolbar layout bounds are managed via scrollable options. */
	override fun applyHeightToAvailBounds() {}

	/** Empty override as toolbar elevation padding is handled by child items. */
	override fun setPaddingForElevation() {}

	/** Configures scrollable bounds and offsets for attached scrollable views. */
	override fun applyScrollableViewOptions() {
		super.applyScrollableViewOptions()
		val appBarLayout = getAppBarLayout()
		if (appBarLayout != null) {
			onAppBarOffsetChanged(appBarLayout, appBarLayout.top)
		} else {
			getFloatingScrollableManager().forceTopFadingEdgeClamped(forcedTopFadingEdge)
			applyScrollAvailBounds(-1, appBarVisibleHeight, AVAIL_RECT_TAG_BOTTOM, dispatchFakeScroll = true)
		}
		if (withAppBarLayout) {
			return
		}
		getFloatingScrollableManager().setFloatingToolbarLayoutHeight(measuredHeight)
	}

	/**
	 * Checks if blur views require invalidation based on toolbar and projection view alpha.
	 *
	 * @return `true` if blur views require invalidation, `false` otherwise.
	 */
	override fun needInvalidateBlurViews(): Boolean {
		return alpha != 0.0f && projectionView.alpha != 0.0f
	}

	/**
	 * Reacts to [AppBarLayout] scroll offset changes to update top/bottom fading edges and title alpha.
	 *
	 * @param appBarLayout Attached [AppBarLayout].
	 * @param verticalOffset Current vertical offset.
	 */
	override fun onAppBarOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
		appBarVisibleHeight = appBarLayout.height + verticalOffset
		val collapsedHeight = appBarLayout.seslGetCollapsedHeight().toInt()
		val appBarOffset = (collapsedHeight - appBarVisibleHeight).coerceAtLeast(0)
		forcedTopFadingEdge = (appBarOffset - appBarLayout.seslGetProportionExtraHeight()).coerceAtLeast(0)
		getFloatingScrollableManager().forceTopFadingEdgeClamped(forcedTopFadingEdge)
		applyScrollAvailBounds(-1, appBarVisibleHeight, AVAIL_RECT_TAG_BOTTOM, dispatchFakeScroll = true)
		applyScrollAvailBounds(appBarOffset, -1, AVAIL_RECT_TAG_TOP, dispatchFakeScroll = true)

		if (appBarLayout.useFloatingToolbar()) {
			getFloatingScrollableManager().setScrollBarTopOffset(collapsedHeight, appBarOffset)
			getFloatingScrollableManager().setFadingEdgeBottomOffset(appBarLayout.top + appBarLayout.height)
			if (collapsedHeight - appBarVisibleHeight >= 0) {
				getFloatingScrollableManager().setFloatingToolbarLayoutHeight(appBarOffset)
			}
			getFloatingScrollableManager().setAppBarOffset(appBarLayout.top + appBarLayout.measuredHeight)
		} else {
			getFloatingScrollableManager().setScrollBarTopOffset(0, appBarOffset)
			getFloatingScrollableManager().setFadingEdgeBottomOffset(((appBarLayout.top + appBarLayout.height) - appBarLayout.seslGetCollapsedHeight()).toInt())
			getFloatingScrollableManager().setAppBarOffset(((appBarLayout.top + appBarLayout.measuredHeight) - appBarLayout.seslGetCollapsedHeight()).toInt())
		}
		getFloatingScrollableManager().invalidateScrollableView()
		super.onAppBarOffsetChanged(appBarLayout, verticalOffset)
		updateTitleAlphaForCurrentOffset(appBarLayout, layout = false)
	}

	/**
	 * Draws the layout and ensures the action mode view stub is brought to front.
	 *
	 * @param canvas Canvas to draw onto.
	 */
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		ensureViewStub()
	}

	/**
	 * Measures child views, toolbar, projection view, and action mode bar.
	 *
	 * @param widthMeasureSpec Width measurement specification.
	 * @param heightMeasureSpec Height measurement specification.
	 */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		ensureViewStub()
		val tb = toolbar
		if (tb == null) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}
		measureChild(tb, widthMeasureSpec, heightMeasureSpec)
		setMeasuredDimension(
			paddingStart + paddingEnd + tb.measuredWidth,
			paddingTop + paddingBottom + tb.measuredHeight
		)
		projectionView.measure(
			MeasureSpec.makeMeasureSpec(measuredWidth - paddingStart - paddingEnd, MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(measuredHeight - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
		)
		getActionModeBarView()?.measure(
			MeasureSpec.makeMeasureSpec(measuredWidth - paddingStart - paddingEnd, MeasureSpec.EXACTLY),
			heightMeasureSpec
		)
		val abl = getAppBarLayout()
		if (abl != null) {
			for (view in abl.children) {
				if (view is CollapsingToolbarLayout) {
					view.minimumHeight = tb.measuredHeight
					return
				}
			}
		}
	}

	/**
	 * Positions child views and initializes child view tracking for projection animation checks.
	 *
	 * @param changed Whether layout bounds changed.
	 * @param left Left boundary coordinate.
	 * @param top Top boundary coordinate.
	 * @param right Right boundary coordinate.
	 * @param bottom Bottom boundary coordinate.
	 */
	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		val tb = toolbar
		if (tb != null) {
			if (isFirstLayout) {
				checkDependenceToAppBar()
				projectionView.startProjectionViewItemAnimation(false)
				applyScrollableViewOptions()
				isFirstLayout = false
				toolbarChildCount = tb.childCount
				val childCountClamped = tb.childCount.coerceAtMost(MAX_CHILD_POSITION_CHECKING_SIZE)
				for (i in 0 until childCountClamped) {
					val child = tb.getChildAt(i)
					val pos = i * 2
					toolbarChildrenPosition[pos] = child.left
					toolbarChildrenPosition[pos + 1] = child.right
				}
			}
			if (!withAppBarLayout) {
				projectionView.startProjectionViewItemAnimation(false)
			}
			val actionModeView = getActionModeBarView()
			if (actionModeView != null && !isActionModeGlobalLayoutListenerAdded) {
				isActionModeGlobalLayoutListenerAdded = true
				actionModeView.viewTreeObserver.addOnGlobalLayoutListener {
					val isActionModeBarVisible = actionModeView.isVisible
					if (isActionModeBarVisible != isActionMode) {
						isActionMode = isActionModeBarVisible
						toolbar?.apply {
							importantForAccessibility = if (isActionModeBarVisible) {
								IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
							} else {
								IMPORTANT_FOR_ACCESSIBILITY_AUTO
							}
						}
						projectionView.startProjectionViewItemAnimation(true)
						if (isActionModeBarVisible) {
							for (id in toolbarTitleViCustomViewIds) {
								actionModeView.findViewById<View>(id)?.post {
									getAppBarLayout()?.let { abl ->
										updateTitleAlphaForCurrentOffset(abl, true)
									}
								}
							}
						}
					}
					toolbar?.isInvisible = isActionMode
					getAppBarLayout()?.let { abl ->
						updateTitleAlphaForCurrentOffset(abl, true)
					}
				}
			}
		} else {
			projectionView.startProjectionViewItemAnimation(false)
		}
		toolbar?.seslSetEatingHover(alpha != 1.0f)
		getActionModeBarView()?.seslSetEatingTouchOnly(alpha != 1.0f)
		super.onLayout(changed, left, top, right, bottom)
		if (tb != null) {
			startProjectionViewItemAnimationIfToolbarChanged()
		}
	}

	/**
	 * Registers a view ID whose alpha is synchronized with the toolbar title alpha.
	 *
	 * @param viewId View ID of custom title view.
	 */
	fun addCustomViewIdsForTitleVi(viewId: Int) {
		toolbarTitleViCustomViewIds.add(viewId)
		val abl = getAppBarLayout()
		if (abl != null) {
			if (abl.seslGetCurrentAppBarState() == APP_BAR_STATE_HIDE) {
				setAlphaForToolbarTitleViGroup(0.0f)
			} else {
				updateTitleAlphaForCurrentOffset(abl, layout = false)
			}
		}
	}

	/**
	 * Unregisters a custom title view ID.
	 *
	 * @param viewId View ID of custom title view to unregister.
	 */
	fun removeCustomViewIdsForTitleVi(viewId: Int) {
		toolbarTitleViCustomViewIds.remove(viewId)
	}

	/**
	 * Forces hiding a specific projection background type.
	 *
	 * @param type Projection background position type to hide.
	 */
	fun addHideToolbarItemBackground(type: FloatingAware.PositionType) {
		info("Force hide projection view $type")
		projectionView.addHideBackgroundType(type)
	}

	/**
	 * Restores visibility for a hidden projection background type.
	 *
	 * @param type Projection background position type to unhide.
	 */
	fun removeHideToolbarItemBackground(type: FloatingAware.PositionType) {
		info("Force hide projection view $type")
		projectionView.removeHideBackgroundType(type)
	}

	/**
	 * Animates the toolbar title and custom title views' alpha.
	 *
	 * @param show Target visibility state (`true` to show title, `false` to hide).
	 * @param immediately Whether to apply the target alpha immediately without animation.
	 */
	fun startTitleAlphaAnimation(show: Boolean, immediately: Boolean = false) {
		val targetAlpha = if (show) 1.0f else 0.0f
		val tb = toolbar
		if (tb != null) {
			if (titleAlphaValueAnimator.isRunning || immediately) {
				titleAlphaValueAnimator.cancel()
			}
			val anim = ObjectAnimator.ofFloat(tb, titleAlphaAnimProperty, targetAlpha)
			titleAlphaValueAnimator = anim
			anim.duration = if (immediately) 0L else (if (show) TITLE_ALPHA_ANIM_SHOW_DURATION else TITLE_ALPHA_ANIM_HIDE_DURATION)
			anim.start()
		}
	}

	/**
	 * Evaluates current [AppBarLayout] collapse offset and updates title alpha accordingly.
	 *
	 * @param appbarLayout Attached [AppBarLayout].
	 * @param layout Whether this update is invoked from a layout pass.
	 */
	fun updateTitleAlphaForCurrentOffset(appbarLayout: AppBarLayout, layout: Boolean) {
		if (appbarLayout.bottom >= appbarLayout.seslGetCollapsedHeight()) {
			if (titleAlphaValueAnimator.isRunning) {
				titleAlphaValueAnimator.cancel()
			}
			setTitleAlphaByCollapsingToolbarLayoutPolicy(appbarLayout)
		} else if (layout) {
			val abl = getAppBarLayout()
			if (abl == null || abl.seslGetCurrentAppBarState() != APP_BAR_STATE_COLLAPSED) {
				setAlphaForToolbarTitleViGroup(0.0f)
			} else {
				setAlphaForToolbarTitleViGroup(1.0f)
			}
		}
	}

	/** Sets alpha for toolbar title, subtitle, and custom title views. */
	private fun setAlphaForToolbarTitleViGroup(alpha: Float) {
		setAlphaForToolbar(alpha)
		setAlphaForTitleViCustomView(alpha)
	}

	/** Sets alpha for toolbar title, subtitle, and background. */
	private fun setAlphaForToolbar(alpha: Float) {
		val tb = toolbar
		if (tb != null) {
			tb.seslSetTitleAlpha(alpha)
			val background = tb.background
			if (background != null) {
				background.mutate().alpha = (255 * alpha).toInt()
				tb.seslSetSubtitleAlpha(alpha)
			}
		}
	}

	/** Sets alpha for all registered custom title views. */
	private fun setAlphaForTitleViCustomView(alpha: Float) {
		for (id in toolbarTitleViCustomViewIds) {
			findViewById<View>(id)?.alpha = alpha
		}
	}

	/** Calculates title alpha based on CollapsingToolbarLayout collapse progress. */
	private fun setTitleAlphaByCollapsingToolbarLayoutPolicy(appbarLayout: AppBarLayout) {
		val collapsedHeight = if (appbarLayout.useCollapsedHeight()) {
			appbarLayout.seslGetCollapsedHeight()
		} else {
			resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_action_bar_height_with_padding).toFloat()
		}
		val h2 = appbarLayout.height * 0.143f
		val topAbs = abs(appbarLayout.top)
		var height = 255.0f
		if (appbarLayout.bottom > collapsedHeight.toInt() && appbarLayout.seslGetCurrentAppBarState() != APP_BAR_STATE_COLLAPSED) {
			height = (topAbs - (appbarLayout.height * 0.35f)) * (150f / h2)
			if (height < 0.0f) {
				height = 0.0f
			} else if (height > 255.0f) {
				height = 255.0f
			}
		}
		setAlphaForToolbarTitleViGroup(height / 255.0f)
		toolbar?.setTitleAccessibilityEnabled(true)
	}

	/**
	 * Determines if scrollable view state allows app bar hide transitions.
	 *
	 * @param floatingScrollableView Target scrollable view.
	 * @param coordinatorLayout Parent CoordinatorLayout.
	 * @return `true` if hide transitions are allowed, `false` otherwise.
	 */
	fun isStateToHideCondition(
		floatingScrollableView: View,
		coordinatorLayout: CoordinatorLayout,
	): Boolean {
		if (getFloatingScrollableManager().getFloatingScrollable() != floatingScrollableView) {
			warn("isStateToHideCondition floatingScrollableView is not synced ($floatingScrollableView) != (${getFloatingScrollableManager().getFloatingScrollable()})")
		}
		if (floatingScrollableView.canScrollVertically(-1)) {
			return true
		}
		val appBar = getAppBarLayout()
		val top: Int
		val minTop: Int
		if (appBar != null) {
			top = appBar.top + appBar.height
			minTop = maxOf(top - appBar.seslGetCollapsedHeight().toInt(), 0)
		} else {
			top = 0
			minTop = 0
		}
		val floatingScrollable = getFloatingScrollableManager().getFloatingScrollable()
		if (floatingScrollable is View && !getFloatingScrollableManager().isValidCondition(floatingScrollable) && getAppBarLayout() != null) {
			val appBar2 = getAppBarLayout()
			val topBarHeight = if (appBar2 != null) {
				if ((appBar2.seslGetAppBarState().state and APP_BAR_STATE_HIDE) != 0) appBar2.measuredHeight else 0
			} else {
				0
			}
			if (appBar2 != null) {
				appBarVisibleHeight = appBar2.top + appBar2.height
			}
			info("Update avail rect because avail bottom is zero. update top=$topBarHeight, bottom=$appBarVisibleHeight")
			applyScrollAvailBounds(-1, appBarVisibleHeight, AVAIL_RECT_TAG_BOTTOM, dispatchFakeScroll = true)
			applyScrollAvailBounds(topBarHeight, -1, AVAIL_RECT_TAG_TOP, dispatchFakeScroll = true)
		}
		return !getFloatingScrollableManager().isInScreen(coordinatorLayout.height, top, minTop)
	}

	/**
	 * [FloatingAware] implementation for [FloatingToolbarLayout] that inspects toolbar nav buttons,
	 * custom views, and menu views to place item projection background bounds.
	 *
	 * @param floatingToolbarLayout Attached [FloatingToolbarLayout].
	 */
	open class FloatingToolbarAware(
		private val floatingToolbarLayout: FloatingToolbarLayout,
	) : FloatingGroupAware(null) {

		private val context: Context = floatingToolbarLayout.context
		private var menuStartPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_start_padding_inset)
		private var menuStartTextPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_text_menu_start_padding_inset)
		private var menuStartOneIconPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_one_icon_menu_start_padding_inset)
		private var menuEndPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_end_padding_inset)
		private var menuEndTextPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_text_menu_end_padding_inset)
		private var menuMoreIconStartPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_more_icon_start_padding_inset)
		private var menuMoreIconEndPaddingInset: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_more_icon_end_padding_inset)
		private var navUpStartPadding: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_navup_start_padding)
		private var floatingComponentHeight: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_toolbar_component_height)
		private var singleCustomMenuIconItemDelta: Int =
			context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_single_custom_menu_icon_item_delta)
		private var oldConfiguration: Configuration = context.resources.configuration

		/** Returns current navigation button or close button in toolbar / action mode. */
		private fun getCurrentNavView(): View? {
			if (!floatingToolbarLayout.isActionMode) {
				val tb = floatingToolbarLayout.toolbar ?: return null
				val navButtonView = SeslBaseReflector.getDeclaredField(Toolbar::class.java, "mNavButtonView")?.get(tb) as? View
				if (navButtonView == null || navButtonView.parent == null) return null
				return navButtonView
			}
			val actionModeView = floatingToolbarLayout.getActionModeBarView()
			val closeBtn = actionModeView?.seslGetCloseButton()
			if (closeBtn != null && closeBtn.isVisible && closeBtn.parent != null) {
				return closeBtn
			}
			val tb = floatingToolbarLayout.toolbar
			val navButtonView = tb?.let { SeslBaseReflector.getDeclaredField(Toolbar::class.java, "mNavButtonView")?.get(it) as? View }
			if (navButtonView != null && navButtonView.parent != null) {
				return navButtonView
			}
			return null
		}

		/** Returns current custom view in toolbar / action mode. */
		private fun getCurrentCustomView(): View? {
			if (floatingToolbarLayout.isActionMode) {
				val actionModeView = floatingToolbarLayout.getActionModeBarView()
				val customView = actionModeView?.seslGetCustomView()
				if (customView != null) return customView
			}
			val tb = floatingToolbarLayout.toolbar ?: return null
			return tb.seslGetCustomView()
		}

		/** Returns current action menu view in toolbar / action mode. */
		private fun getCurrentMenuView(): View? {
			val menuView = if (floatingToolbarLayout.isActionMode) {
				val actionModeView = floatingToolbarLayout.getActionModeBarView()
				actionModeView?.seslGetMenuView() ?: floatingToolbarLayout.toolbar?.seslGetMenuView()
			} else {
				floatingToolbarLayout.toolbar?.seslGetMenuView()
			}
			if (menuView == null || menuView.childCount == 0) return null
			return menuView
		}

		/**
		 * Returns the target reference view for projection background matching for a given position type.
		 *
		 * @param type Target position type.
		 * @return The reference [View] or `null`.
		 */
		override fun getReferenceView(type: FloatingAware.PositionType): View? {
			val navView = getCurrentNavView()
			return when (type) {
				FloatingAware.PositionType.START_FIRST -> navView ?: getCurrentCustomView()
				FloatingAware.PositionType.START_SECOND -> {
					var customView: View? = null
					if (navView != null && getCurrentCustomView().also { customView = it } != null) customView else null
				}
				FloatingAware.PositionType.END_FIRST -> getCurrentMenuView()
			}
		}

		/**
		 * Returns the calculated padding inset rectangle for projection background bounds.
		 *
		 * @param type Target position type.
		 * @return Padding [Rect] insets.
		 */
		override fun getReferenceViewInset(type: FloatingAware.PositionType): Rect {
			return when (type) {
				FloatingAware.PositionType.START_FIRST -> {
					val navView = getCurrentNavView() ?: return Rect()
					val h = (navView.height - floatingComponentHeight) / 2
					Rect(navUpStartPadding, h, 0, h)
				}

				FloatingAware.PositionType.START_SECOND -> Rect()
				FloatingAware.PositionType.END_FIRST -> {
					val menuView = getCurrentMenuView()
					val actionMenuView = menuView as? ActionMenuView
					if (actionMenuView != null && actionMenuView.childCount > 0) {
						val firstChild = actionMenuView.getChildAt(0)
						val firstChildIsTextButton =
							(firstChild as? ActionMenuItemView)?.seslIsTextButtonVisible()
								?: (firstChild as? ActionMenuItemViewBadgedWrapper)?.innerItemView?.seslIsTextButtonVisible()//custom
								?: false

						val lastChild = actionMenuView.getChildAt(actionMenuView.childCount - 1)
						val lastChildIsTextButton =
							(lastChild as? ActionMenuItemView)?.seslIsTextButtonVisible()
								?: (lastChild as? ActionMenuItemViewBadgedWrapper)?.innerItemView?.seslIsTextButtonVisible()//custom
								?: false

						val center = (actionMenuView.height - floatingComponentHeight) / 2
						val isOverflowShowing = actionMenuView.isOverflowMenuShowing
						val hasOverflowButton = actionMenuView.seslIsShowOverflowButton()
						val actionMenuItemCount = actionMenuView.childCount

						var startInset = if (actionMenuItemCount == 1 && hasOverflowButton) {
							menuMoreIconStartPaddingInset
						} else if (actionMenuItemCount != 1 || firstChildIsTextButton) {
							if (firstChildIsTextButton) menuStartTextPaddingInset else menuStartPaddingInset
						} else {
							menuStartOneIconPaddingInset
						}

						var endInset = if (isOverflowShowing) {
							menuMoreIconEndPaddingInset
						} else {
							if (lastChildIsTextButton) menuEndTextPaddingInset else menuEndPaddingInset
						}

						if (actionMenuItemCount == 1 && !isOverflowShowing &&
							firstChild.width == floatingComponentHeight &&
							firstChild !is ActionMenuItemView &&
							firstChild !is ActionMenuItemViewBadgedWrapper//custom
						) {
							startInset += singleCustomMenuIconItemDelta
							endInset += singleCustomMenuIconItemDelta
						}
						Rect(startInset, center, endInset, center)
					} else {
						Rect()
					}
				}
			}
		}

		/**
		 * Updates dimension values on configuration/density changes.
		 *
		 * @param show Current show state.
		 * @param newConfig New configuration.
		 */
		override fun onFloatingViewConfigurationChanged(show: Boolean, newConfig: Configuration) {
			if (oldConfiguration.densityDpi != newConfig.densityDpi) {
				updateDimensionValues()
				oldConfiguration = newConfig
			}
			super.onFloatingViewConfigurationChanged(show, newConfig)
		}

		/** Re-reads dimension resources on configuration updates. */
		fun updateDimensionValues() {
			menuStartPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_start_padding_inset)
			menuStartTextPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_text_menu_start_padding_inset)
			menuEndPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_end_padding_inset)
			menuEndTextPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_text_menu_end_padding_inset)
			menuMoreIconStartPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_more_icon_start_padding_inset)
			menuMoreIconEndPaddingInset =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_menu_more_icon_end_padding_inset)
			navUpStartPadding =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_navup_start_padding)
			floatingComponentHeight =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_toolbar_component_height)
			singleCustomMenuIconItemDelta =
				context.resources.getDimensionPixelSize(R.dimen.sesl_projection_bg_single_custom_menu_icon_item_delta)
		}
	}
}
