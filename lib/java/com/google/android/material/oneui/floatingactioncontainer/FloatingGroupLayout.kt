@file:Suppress("NOTHING_TO_INLINE")

package com.google.android.material.oneui.floatingactioncontainer

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Property
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.policy.BlurInfoState
import androidx.appcompat.oneui.common.internal.resource.OpenThemeResourceDrawableRes
import androidx.appcompat.oneui.common.internal.resource.ThemeResourceDrawableRes
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.appcompat.oneui.common.internal.semblurinfo.SemBlurInfoStateBuilder
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.oneui.common.internal.semblurinfo.SemBlurInfoState
import androidx.core.view.SemBlurCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.SeslScrollable
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.reflect.DeviceInfo
import androidx.reflect.SeslBaseReflector
import com.google.android.material.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.ThemeEnforcement
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.common.internal.animation.ResizeAnimation
import com.google.android.material.oneui.common.internal.animation.SeslFloatingBlurElevationAnimator
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.error
import com.google.android.material.oneui.common.internal.info
import com.google.android.material.oneui.common.internal.policy.SeslFloatingBlurElevationPolicy
import com.google.android.material.oneui.common.internal.util.getViewRect
import com.google.android.material.oneui.common.internal.util.setViewRect
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.floatingactioncontainer.FloatingAware.PositionType
import com.google.android.material.oneui.floatingactioncontainer.behavior.AppBarScrollBehavior
import com.google.android.material.oneui.floatingactioncontainer.manager.FloatingScrollableManager
import com.google.android.material.oneui.floatingactioncontainer.manager.FloatingScrollableManager.Companion.UNSET
import com.google.android.material.oneui.floatingactioncontainer.manager.SeslScrollableListener
import com.google.android.material.oneui.floatingactioncontainer.manager.adapter.FloatingScrollableAdapter
import java.lang.ref.WeakReference
import android.view.ViewGroup.LayoutParams as ViewGroupLayoutParams

/**
 * Base layout container for Samsung OneUI floating components.
 *
 * Provides smooth show/hide transitions, scroll integration with [SeslScrollable] views,
 * background projection rendering, and blur support.
 *
 * @param context The Context the view is running in.
 * @param attrs The attributes of the XML tag inflating the view.
 * @param defStyleAttr Default style attribute for styling.
 */
open class FloatingGroupLayout @JvmOverloads constructor(
	context: Context,
	protected val attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr),
	CoordinatorLayout.AttachedBehavior,
	BlurSupportable,
	MaterialLogTag {

	/**
	 * Listener interface for observing layout alpha animation events.
	 */
	interface OnAlphaAnimationListener {
		/**
		 * Called when alpha animation starts.
		 *
		 * @param toShow `true` if animating towards visible (alpha 1.0), `false` otherwise.
		 */
		fun onStart(toShow: Boolean) {}

		/**
		 * Called on every frame of alpha animation progress.
		 *
		 * @param alpha Current alpha value ranging from 0f to 1f.
		 */
		fun onProgress(alpha: Float) {}

		/**
		 * Called when alpha animation finishes.
		 */
		fun onEnd() {}
	}

	companion object {
		/** Child view index for the projection background view. */
		private const val VIEW_INDEX_PROJECTION_VIEW = 0

		/** Child view index for the primary content view. */
		private const val VIEW_INDEX_CONTENT = 1

		private const val UPDATE_POST_DELAY = 10L
		private const val AWARE_CALLBACK_NONE = 0
		private const val AWARE_CALLBACK_SHOW = 1
		private const val AWARE_CALLBACK_HIDE = 2
		private const val ANDROID_XML_NS = "http://schemas.android.com/apk/res/android"
		private const val TAG = "FloatingGroupLayout"
		private const val DEBUG = false
		private const val DEBUG_ANIMATION = true
		private const val DEBUG_PRE_DRAW = false

		private const val APP_BAR_STATE_HIDE = 4

		private const val RESET_HIDE_TRANSITION_DELAY_MS = 50L
		private const val POST_SHOW_DELAY_MS = 300L

		private const val SPRING_SCALE_MIN = 0.94f
		private const val SPRING_SCALE_MAX = 1.0f
		private const val SPRING_DAMPING_RATIO = 1.0f
		private const val SPRING_STIFFNESS = 1200.0f
		private const val SPRING_SCALE_FACTOR = 10000.0f

		private const val PROJECTION_BG_TAG_START_FIRST = "start_first"
		private const val PROJECTION_BG_TAG_START_SECOND = "start_second"
		private const val PROJECTION_BG_TAG_END_FIRST = "end_first"
		private const val PROJECTION_VIEW_TAG = "ProjectionView"

		private const val BLUR_MODE_DEFAULT = 2
		private const val RESIZE_ANIMATION_RATIO = 100.0f
		private const val PREV_TARGET_ALPHA_UNSET = -1.0f

		/** Enables or disables expanded canvas blur on the specified view via reflection. */
		private fun setEnableExpandedCanvasBlur(view: View, enable: Boolean) {
			if (DeviceInfo.isOneUI()) {
				val method = SeslBaseReflector.getDeclaredMethod(
					View::class.java,
					"hidden_semSetEnableExpandedCanvasBlur",
					Boolean::class.javaPrimitiveType
				) ?: SeslBaseReflector.getDeclaredMethod(
					View::class.java,
					"semSetEnableExpandedCanvasBlur",
					Boolean::class.javaPrimitiveType
				)
				if (method != null) {
					SeslBaseReflector.invoke(view, method, enable)
				}
			}
		}
	}

	override val logTag: String = TAG

	private var layoutAlphaAnimator: ObjectAnimator? = null
	private var scaleSpringAnimation: SpringAnimation? = null
	private val layoutStateListener: MutableList<FloatingLayoutStateChangeListener> = ArrayList()
	private val blurInvalidateTargetViews: MutableMap<View, Rect> = LinkedHashMap()
	private var recyclerViewRef: WeakReference<RecyclerView>? = null
	private var nestedScrollViewRef: WeakReference<NestedScrollView>? = null
	private var scrollabelViewRef: WeakReference<SeslScrollable>? = null
	private var currentFloatingScrollableManager: FloatingScrollableManager? = null
	private var recyclerAttachStateChangeListener: OnAttachStateChangeListener? = null
	private var nestedScrollAttachStateChangeListener: OnAttachStateChangeListener? = null
	private var lifeCycleObserver: LifecycleObserver? = null
	private var isFirstLayout: Boolean = true
	private var needUpdateProjectionBackgroundBounds: Boolean = false
	private var isGoToTopSuppressed: Boolean = false
	private var totalScrollY: Int = 0
	private var touchSlop: Int = 0
	private var appBarVerticalOffset: Int = 0
	private var startBackgroundItemAnimationOnAnimEnd: Boolean = false
	private var requestUpdatePosted: Boolean = false

	private val hideStartScrollRange: Int =
		context.resources.getDimensionPixelOffset(R.dimen.sesl_floating_layout_hide_start_scroll_range)
	private val scrollIdleCheckHandler = Handler(Looper.getMainLooper())
	private val resetHideTransitionRunnable = Runnable { resetHideTransitionCondition() }
	private val postShowHandler = Handler(Looper.getMainLooper())
	private val delayShowRunnable = Runnable {
		resetHideTransitionCondition()
		startViewAlphaAnimation(show = true, dispatchedByScroll = true)
	}
	private val postInvalidateHandler = Handler(Looper.getMainLooper())

	/** Bottom window inset pixel value passed down to [FloatingScrollableManager].
	 * for adjusting bottom boundary for scrollbars, hover scroller and goToTop button.*/
	var windowInsetBottom: Int = UNSET

	/** `true` if this floating layout coordinates with an [AppBarLayout]. */
	var withAppBarLayout: Boolean = true

	/** The inner [SeslProjectionView] hosting projection background shapes. */
	val projectionView: SeslProjectionView = SeslProjectionView(context)

	/** `true` to animate item projection backgrounds into view on initial layout. */
	var showBackgroundAtFirst: Boolean = true

	/** `true` to skip layout and projection background animations. */
	var skipAnimation: Boolean = false
		set(value) {
			field = value
			warn("new set skipAnimation $value ${javaClass.simpleName}")
		}

	/** `true` to enable hardware-accelerated canvas blur on target views. */
	var expandedCanvasBlurEnabled: Boolean = true
		set(value) {
			field = value
			warn("new set expandedCanvasBlurEnabled $value ${javaClass.simpleName}")
			for (targetView in blurInvalidateTargetViews.keys) {
				setEnableExpandedCanvasBlur(targetView, value)
			}
		}

	/** `true` to allow show/hide transitions triggered by content scrolling. */
	var enableScrollTransition: Boolean = true

	/** `true` to apply blur effects to item projection background shapes. */
	var applyFloatingItemBackgroundBlur: Boolean = true

	/** Current target alpha value for layout visibility animations. */
	var viewTargetAlpha: Float = 1f

	/**
	 * Custom [FloatingAware] implementation providing reference views and bounds insets for item projection.
	 * Ensure that the relevant reference views are already visible when invoking this.
	 * */
	open var floatingAware: FloatingAware? = null
		get() = field ?: FloatingGroupAware(this)
		set(value) {
			field = value ?: FloatingGroupAware(this)
			forceSendAwareCallback()
		}

	/** Custom override setting whether GoToTop button offset is managed automatically. */
	var manageGoToTopOffset: Boolean? = null

	/** Custom override setting whether fading edge bottom offset is managed automatically. */
	var manageFadingEdgeBottomOffset: Boolean? = null

	/** `true` to block pre-draw blur invalidations during hidden or detached states. */
	var blockBlurInvalidateOnPreDraw: Boolean = false
		set(value) {
			if (field != value) {
				debug("set blockBlurInvalidate=$value $this")
				field = value
			}
		}

	/** Custom elevation padding rectangle applied to this container. */
	var customPadding: Rect? = null

	/** [FloatingAnimationConfig] defining animation durations and interpolators. */
	var floatingAnimationConfigs: FloatingAnimationConfig = FloatingAnimationConfig()
		set(value) {
			field = value
			projectionView.setAnimationConfig(value)
		}

	private var layoutAlphaAnimationListener: OnAlphaAnimationListener? = null

	private val alphaAnimListener = object : Animator.AnimatorListener {
		override fun onAnimationStart(animation: Animator) {}
		override fun onAnimationEnd(animation: Animator) {
			updateVisibleLayoutState()
		}

		override fun onAnimationCancel(animation: Animator) {}
		override fun onAnimationRepeat(animation: Animator) {}
	}

	private val alphaAnimProperty = object : Property<View, Float>(Float::class.java, "FloatingLayoutAlphaAnim") {
		override fun get(view: View): Float = view.alpha
		override fun set(view: View, value: Float) {
			view.alpha = value
			onAlphaAnimationProgress(value)
		}
	}

	private val scrollableListener = object : SeslScrollableListener {
		override fun onFastScrollStart(view: View?) {
			val appBarLayout = getAppBarLayout()
			if (appBarLayout != null && (appBarLayout.seslGetCurrentAppBarState() and APP_BAR_STATE_HIDE) == 0) {
				if (this@FloatingGroupLayout !is FloatingBottomLayout) {
					updateGoToTopVisibility(false)
					return
				}
			}
			stopPostShowRunnable()
			startViewAlphaAnimation(show = false)
		}

		override fun onFastScrollEnd(view: View?) {
			startViewAlphaAnimation(show = true)
			updateGoToTopVisibility(true)
		}

		override fun onScrolled(view: View?, dx: Int, dy: Int) {
			scrollIdleCheckHandler.removeCallbacks(resetHideTransitionRunnable)
			scrollIdleCheckHandler.postDelayed(resetHideTransitionRunnable, RESET_HIDE_TRANSITION_DELAY_MS)
			invalidateBlurViewsIfNeed()
			val appBarLayout = getAppBarLayout()
			if (appBarLayout != null) {
				if (appBarLayout.seslIsHided()) {
					checkAndRunHideTransition(dy)
				}
			} else {
				checkAndRunHideTransition(dy)
			}
			if (getVisibleState() == FloatingLayoutState.STATE_HIDE ||
				getVisibleState() == FloatingLayoutState.STATE_ANIMATING_TO_HIDE ||
				shouldRestoreGoToTop()
			) {
				startPostShowRunnable()
			}
		}
	}

	/** Pre-draw listener used to invalidate registered blur target views when position changes. */
	val onPreDrawListener = ViewTreeObserver.OnPreDrawListener {
		if (blockBlurInvalidateOnPreDraw) {
			debug("onPreDrawListener blockBlurInvalidate ${javaClass.simpleName}")
			return@OnPreDrawListener true
		}
		val changedViews = ArrayList<View>()
		val positionsMap = LinkedHashMap<View, Rect>()
		blurInvalidateTargetViews.forEach { (view, prePos) ->
			if (needInvalidate(view)) {
				val rect = Rect()
				if (view.getGlobalVisibleRect(rect) && prePos != rect) {
					changedViews.add(view)
					positionsMap[view] = rect
				}
			}
		}
		if (changedViews.isNotEmpty() && !requestUpdatePosted) {
			requestUpdatePosted = true
			postInvalidateHandler.postDelayed({
				for (v in changedViews) {
					if (needInvalidate(v)) {
						debug("onPreDraw position Change invalidateBlurTargetView ${v.javaClass.simpleName} ${v.tag}")
						v.invalidate()
					}
				}
				requestUpdatePosted = false
			}, UPDATE_POST_DELAY)
			positionsMap.forEach { (view, rect) ->
				debug("OnPreDrawListener invalidateRect ${view.javaClass.simpleName} ${view.tag} $rect")
			}
			positionsMap.forEach { (view, rect) ->
				if (blurInvalidateTargetViews.containsKey(view)) {
					blurInvalidateTargetViews[view] = rect
				}
			}
		}
		true
	}

	/**
	 * Default [FloatingAware] implementation for [FloatingGroupLayout].
	 *
	 * @param floatingGroupLayout Attached [FloatingGroupLayout].
	 */
	open class FloatingGroupAware @JvmOverloads constructor(
		private val floatingGroupLayout: FloatingGroupLayout? = null
	) : FloatingAware {
		/**
		 * Returns reference view for projection matching.
		 *
		 * @param type Target position type.
		 * @return Primary content view if position is [PositionType.START_FIRST].
		 */
		override fun getReferenceView(type: PositionType): View? {
			if (type == PositionType.START_FIRST && floatingGroupLayout != null) {
				return floatingGroupLayout.getChildAt(VIEW_INDEX_CONTENT)
			}
			return null
		}
	}

	/**
	 * CoordinatorLayout Behavior for [FloatingGroupLayout] reacting to [AppBarLayout] changes.
	 *
	 * @param T The type of [FloatingGroupLayout].
	 * @param context The Context the behavior is running in.
	 * @param attrs The attributes of the XML tag inflating the behavior.
	 */
	open class FloatingActionBehavior<T : FloatingGroupLayout> @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null
	) : AppBarScrollBehavior<T>(context, attrs), MaterialLogTag {

		override val logTag: String = "FloatingActionBehavior"

		private var prevProgress: Float = -1.0f
		private var reserveLayoutHide: Boolean = false
		private val tmpRect = Rect()

		/**
		 * Handles child layout pass, animating initial projection background if configured.
		 *
		 * @param parent CoordinatorLayout parent view.
		 * @param child Target [FloatingGroupLayout] child.
		 * @param layoutDirection Current layout direction.
		 * @return `true` if handled by behavior, `false` otherwise.
		 */
		override fun onLayoutChild(
			parent: CoordinatorLayout,
			child: T,
			layoutDirection: Int
		): Boolean {
			if (child.showBackgroundAtFirst && child.projectionView.parent != null) {
				child.projectionView.startProjectionViewItemAnimation(true)
				child.projectionView.startProjectionViewAlphaAnimation(1.0f, true)
			}
			return super.onLayoutChild(parent, child, layoutDirection)
		}
	}

	init {
		val a = ThemeEnforcement.obtainStyledAttributes(
			context,
			attrs,
			R.styleable.FloatingGroupLayout,
			defStyleAttr,
			0
		)
		if (a.hasValue(R.styleable.FloatingGroupLayout_showFloatingItemBackground)) {
			showBackgroundAtFirst =
				a.getBoolean(R.styleable.FloatingGroupLayout_showFloatingItemBackground, true)
		}
		if (a.hasValue(R.styleable.FloatingGroupLayout_skipAnimation)) {
			skipAnimation = a.getBoolean(R.styleable.FloatingGroupLayout_skipAnimation, false)
			if (skipAnimation) {
				warn("Skip Animation On ${javaClass.simpleName}")
			}
		}
		if (a.hasValue(R.styleable.FloatingGroupLayout_expandedCanvasBlurEnabled)) {
			expandedCanvasBlurEnabled =
				a.getBoolean(R.styleable.FloatingGroupLayout_expandedCanvasBlurEnabled, true)
			warn("set expanded CanvasBlur: $expandedCanvasBlurEnabled, ${javaClass.simpleName}")
			for (bgView in projectionView.prjBgViewList) {
				setEnableExpandedCanvasBlur(bgView, expandedCanvasBlurEnabled)
			}
		}
		if (a.hasValue(R.styleable.FloatingGroupLayout_enableScrollTransition)) {
			enableScrollTransition =
				a.getBoolean(R.styleable.FloatingGroupLayout_enableScrollTransition, true)
		}
		if (a.hasValue(R.styleable.FloatingGroupLayout_applyFloatingItemBackgroundBlur)) {
			applyFloatingItemBackgroundBlur =
				a.getBoolean(R.styleable.FloatingGroupLayout_applyFloatingItemBackgroundBlur, true)
		}
		a.recycle()

		if (applyFloatingItemBackgroundBlur) {
			projectionView.prjBgEndFirstView.applyBlurInfo(context)
			projectionView.prjBgStartFirstView.applyBlurInfo(context)
			projectionView.prjBgStartSecondView.applyBlurInfo(context)
		}

		val anim = ObjectAnimator.ofFloat(this, alphaAnimProperty, alpha)
		anim.duration = getDuration(floatingAnimationConfigs.layoutAlphaAnimationDuration)
		anim.interpolator = floatingAnimationConfigs.layoutAnimationInterpolator
		anim.addListener(alphaAnimListener)
		layoutAlphaAnimator = anim

		touchSlop = ViewConfiguration.get(context).scaledTouchSlop

		if (attrs?.getAttributeValue(ANDROID_XML_NS, "clipChildren") == null) {
			clipChildren = false
		}
		if (attrs?.getAttributeValue(ANDROID_XML_NS, "clipToPadding") == null) {
			clipToPadding = false
		}
	}

	/** Ensures [projectionView] is attached at child index 0. */
	private fun addProjectionView() {
		val layoutParams = ViewGroup.LayoutParams(ViewGroupLayoutParams.MATCH_PARENT, ViewGroupLayoutParams.WRAP_CONTENT)
		if (indexOfChild(projectionView) != VIEW_INDEX_PROJECTION_VIEW) {
			removeView(projectionView)
			super.addView(projectionView, VIEW_INDEX_PROJECTION_VIEW, layoutParams)
		}
		registerScrollInvalidate()
	}

	/** Registers projection background views as blur invalidate target views on Android 15+. */
	private fun registerScrollInvalidate() {
		if (Build.VERSION.SDK_INT >= 35) {
			addBlurInvalidateTargetViews(
				listOf(
					projectionView.prjBgEndFirstView,
					projectionView.prjBgStartFirstView,
					projectionView.prjBgStartSecondView
				)
			)
		}
	}

	/**
	 * Intercepts child view addition to ensure projection view is placed at index 0.
	 *
	 * @param child Child view being added.
	 * @param index Insertion index.
	 * @param params Layout parameters.
	 */
	override fun addView(child: View?, index: Int, params: ViewGroupLayoutParams?) {
		addProjectionView()
		super.addView(child, index, params)
	}

	/**
	 * Returns the CoordinatorLayout behavior for this floating container.
	 *
	 * @return A new instance of [FloatingActionBehavior].
	 */
	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return FloatingActionBehavior<FloatingGroupLayout>(context, attrs)
	}

	/**
	 * Finds the [AppBarLayout] among dependency views in a CoordinatorLayout.
	 *
	 * @return The [AppBarLayout] instance or `null`.
	 */
	fun List<View>.getAppBarLayout(): AppBarLayout? {
		for (view in this) {
			if (view is AppBarLayout) return view
		}
		return null
	}

	/**
	 * Finds the attached [AppBarLayout] from parent [CoordinatorLayout].
	 *
	 * @return Attached [AppBarLayout] or `null`.
	 */
	open fun getAppBarLayout(): AppBarLayout? {
		if (parent !is CoordinatorLayout) return null
		val coordinatorLayout = parent as CoordinatorLayout
		val dependencies = coordinatorLayout.getDependencies(this)
		return dependencies.getAppBarLayout()
	}

	/** Checks dependency on [AppBarLayout] and updates [withAppBarLayout]. */
	fun checkDependenceToAppBar() {
		withAppBarLayout = getAppBarLayout() != null
	}

	/**
	 * Called when the attached [AppBarLayout] offset changes.
	 *
	 * @param appBarLayout Attached [AppBarLayout].
	 * @param verticalOffset Current vertical offset.
	 */
	open fun onAppBarOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
		if (verticalOffset != 0) {
			invalidateBlurViewsIfNeed()
		}
	}

	/** Called to apply layout options to the managed scrollable view, namely:
	 * - [windowInsetBottom]
	 * - [manageGoToTopOffset]
	 * - [manageFadingEdgeBottomOffset]
	 * */
	open fun applyScrollableViewOptions() {
		val floatingScrollableManager = currentFloatingScrollableManager ?: getFloatingScrollableManager()
		if (windowInsetBottom != UNSET) {
			floatingScrollableManager.windowInsetBottom = windowInsetBottom
		}
		manageGoToTopOffset?.let { floatingScrollableManager.manageGoToTopOffset = it }
		manageFadingEdgeBottomOffset?.let { floatingScrollableManager.manageFadingEdgeBottomOffset = it }
	}

	/**
	 * Returns whether target blur views need invalidation.
	 *
	 * @return `true` if blur views need invalidation, `false` otherwise.
	 */
	open fun needInvalidateBlurViews(): Boolean = true

	private fun needInvalidate(view: View): Boolean {
		if (view.isVisible && view is BlurSupportable) {
			return view.isBlurApplied()
		}
		return false
	}

	private fun invalidateBlurViewsIfNeed() {
		if (needInvalidateBlurViews()) {
			for (view in blurInvalidateTargetViews.keys) {
				if (needInvalidate(view)) {
					var targetView = view
					val blurSupportable = view as? BlurSupportable
					if (blurSupportable != null && blurSupportable.getBlurTargetView() != null) {
						targetView = blurSupportable.getBlurTargetView()!!
					}
					targetView.invalidate()
				}
			}
		}
	}

	/** Invalidates all target views that depend on background blur recalculation. */
	fun invalidateBlurTargetView() {
		for (targetView in blurInvalidateTargetViews.keys) {
			targetView.invalidate()
		}
	}

	/**
	 * Adds views to be invalidated when blur region updates.
	 *
	 * @param views List of views to add.
	 */
	fun addBlurInvalidateTargetViews(views: List<View>) {
		for (view in views) {
			blurInvalidateTargetViews[view] = Rect()
		}
	}

	/**
	 * Adds a view to be invalidated when blur region updates.
	 *
	 * @param view Target view.
	 * @param bounds Optional bounds' rectangle.
	 */
	fun addBlurInvalidateTargetViews(view: View, bounds: Rect? = null) {
		blurInvalidateTargetViews[view] = bounds ?: Rect()
	}

	/**
	 * Removes blur target views from invalidation tracking.
	 *
	 * @param views List of views to remove.
	 */
	fun removeBlurInvalidateTargetViews(views: List<View>) {
		for (view in views) {
			blurInvalidateTargetViews.remove(view)
		}
	}

	/**
	 * Removes a blur target view from invalidation tracking.
	 *
	 * @param view View to remove.
	 */
	fun removeBlurInvalidateTargetViews(view: View) {
		blurInvalidateTargetViews.remove(view)
	}

	/** Clears all registered blur target views. */
	fun clearBlurInvalidateTargetView() {
		blurInvalidateTargetViews.clear()
	}

	/**
	 * Registers a [FloatingLayoutStateChangeListener].
	 *
	 * @param listener Listener instance to add.
	 */
	fun addLayoutStateListener(listener: FloatingLayoutStateChangeListener) {
		layoutStateListener.add(listener)
	}

	/**
	 * Removes a registered [FloatingLayoutStateChangeListener].
	 *
	 * @param listener Listener instance to remove.
	 */
	fun removeLayoutStateListener(listener: FloatingLayoutStateChangeListener) {
		layoutStateListener.remove(listener)
	}

	/** Clears all registered [FloatingLayoutStateChangeListener] instances. */
	fun clearLayoutStateListener() {
		layoutStateListener.clear()
	}

	/**
	 * Attaches a [RecyclerView] to this floating layout container.
	 *
	 * ### What Attaching Does
	 * - **Scroll Integration**: Registers the [RecyclerView] (cast as [SeslScrollable]) with [FloatingScrollableManager]
	 *   to observe scroll events (`onScrolled`, `onFastScrollStart`, `onFastScrollEnd`).
	 * - **Auto Show/Hide Transitions**: Automatically animates the floating layout's alpha and visibility in response to
	 *   scrolling content (e.g., hiding when scrolling down past a threshold, showing when scrolling up or idle).
	 * - **Fast Scroll & Go-To-Top Handling**: Intercepts fast scrolling events to manage the container's visibility and
	 *   coordinate the "Go to Top" button state.
	 * - **Blur Invalidation**: Automatically triggers background blur target view invalidation as scrolling occurs.
	 * - **Option Propagation**: Applies scrollable options such as [windowInsetBottom], [manageGoToTopOffset],
	 *   and [manageFadingEdgeBottomOffset] to [FloatingScrollableManager].
	 *
	 * ### Attachment Lifecycle Management
	 * - **Weak Reference**: Holds the [RecyclerView] instance via [WeakReference] to prevent memory leaks.
	 * - **Window Detachment**: Registers an [OnAttachStateChangeListener] on the [RecyclerView]. When the view is
	 *   detached from the window (`onViewDetachedFromWindow`), [clearFloatingScrollableView] is called automatically.
	 * - **Lifecycle Awareness**: If the view's [Context] is a [LifecycleOwner], a [DefaultLifecycleObserver] is registered
	 *   to automatically detach the scrollable view upon `onDestroy`.
	 * - **Re-binding Cleanup**: Clears any existing scrollable view association before attaching the new instance.
	 *
	 * @param recyclerView Target [RecyclerView] instance to attach.
	 */
	fun setRecyclerView(recyclerView: RecyclerView) {
		if (recyclerView == getRecyclerView()) return
		clearFloatingScrollableView()
		recyclerViewRef = WeakReference(recyclerView)
		val manager = getFloatingScrollableManager()
		manager.setFloatingScrollableView(recyclerView as SeslScrollable)
		manager.addSeslScrollableListener(scrollableListener)
		applyScrollableViewOptions()

		if (recyclerAttachStateChangeListener == null) {
			recyclerAttachStateChangeListener = object : OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(v: View) {}
				override fun onViewDetachedFromWindow(view: View) {
					if (view == getRecyclerView()) {
						clearFloatingScrollableView()
					}
				}
			}
		}
		recyclerView.addOnAttachStateChangeListener(recyclerAttachStateChangeListener!!)
		addOrReplaceLifeCycleObserverForScrollable(recyclerView.context)
	}

	/**
	 * Attaches a [NestedScrollView] to this floating layout container.
	 *
	 * ### What Attaching Does
	 * - **Scroll Integration**: Registers the [NestedScrollView] (cast as [SeslScrollable]) with [FloatingScrollableManager]
	 *   to observe scroll events (`onScrolled`, `onFastScrollStart`, `onFastScrollEnd`).
	 * - **Auto Show/Hide Transitions**: Automatically animates the floating layout's alpha and visibility in response to
	 *   scrolling content (e.g., hiding when scrolling down past a threshold, showing when scrolling up or idle).
	 * - **Fast Scroll & Go-To-Top Handling**: Intercepts fast scrolling events to manage the container's visibility and
	 *   coordinate the "Go to Top" button state.
	 * - **Blur Invalidation**: Automatically triggers background blur target view invalidation as scrolling occurs.
	 * - **Option Propagation**: Applies scrollable options such as [windowInsetBottom], [manageGoToTopOffset],
	 *   and [manageFadingEdgeBottomOffset] to [FloatingScrollableManager].
	 *
	 * ### Attachment Lifecycle Management
	 * - **Weak Reference**: Holds the [NestedScrollView] instance via [WeakReference] to prevent memory leaks.
	 * - **Window Detachment**: Registers an [OnAttachStateChangeListener] on the [NestedScrollView]. When the view is
	 *   detached from the window (`onViewDetachedFromWindow`), [clearFloatingScrollableView] is called automatically.
	 * - **Lifecycle Awareness**: If the view's [Context] is a [LifecycleOwner], a [DefaultLifecycleObserver] is registered
	 *   to automatically detach the scrollable view upon `onDestroy`.
	 * - **Re-binding Cleanup**: Clears any existing scrollable view association before attaching the new instance.
	 *
	 * @param nestedScrollView Target [NestedScrollView] instance to attach.
	 */
	fun setNestedScrollView(nestedScrollView: NestedScrollView) {
		if (nestedScrollView == getNestedScrollView()) return
		clearFloatingScrollableView()
		nestedScrollViewRef = WeakReference(nestedScrollView)
		val manager = getFloatingScrollableManager()
		manager.setFloatingScrollableView(nestedScrollView as SeslScrollable)
		manager.addSeslScrollableListener(scrollableListener)
		applyScrollableViewOptions()

		if (nestedScrollAttachStateChangeListener == null) {
			nestedScrollAttachStateChangeListener = object : OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(v: View) {}
				override fun onViewDetachedFromWindow(view: View) {
					if (view == getNestedScrollView()) {
						clearFloatingScrollableView()
					}
				}
			}
		}
		nestedScrollView.addOnAttachStateChangeListener(nestedScrollAttachStateChangeListener!!)
		addOrReplaceLifeCycleObserverForScrollable(nestedScrollView.context)
	}

	/**
	 * Sets the scrollable adapter for interacting with scrollable content views.
	 *
	 * @param floatingScrollableAdapter Target [FloatingScrollableAdapter].
	 */
	fun setFloatingScrollableAdapter(floatingScrollableAdapter: FloatingScrollableAdapter) {
		clearFloatingScrollableView()
		val scrollable = floatingScrollableAdapter.getFloatingScrollable()
		if (scrollable == null) {
			warn(
				"setFloatingScrollableAdapter fail(getFloatingScrollable return null), scrollableAdapter=$floatingScrollableAdapter"
			)
		} else {
			val manager = FloatingScrollableManager.getInstance(this, floatingScrollableAdapter)
			currentFloatingScrollableManager = manager
			scrollabelViewRef = WeakReference(scrollable)
			manager.addSeslScrollableListener(scrollableListener)
			applyScrollableViewOptions()
		}
	}

	private fun addOrReplaceLifeCycleObserverForScrollable(context: Context) {
		if (context is LifecycleOwner) {
			lifeCycleObserver?.let { context.lifecycle.removeObserver(it) }
			val observer = getLifeCycleObserver(context)
			lifeCycleObserver = observer
			context.lifecycle.addObserver(observer)
		}
	}

	private fun getLifeCycleObserver(context: Context): DefaultLifecycleObserver {
		return object : DefaultLifecycleObserver {
			override fun onDestroy(owner: LifecycleOwner) {
				clearFloatingScrollableView()
				owner.lifecycle.removeObserver(this)
			}
		}
	}

	/** Unregisters the attached [RecyclerView]. */
	fun clearRecyclerView() {
		val recyclerView = recyclerViewRef?.get()
		recyclerAttachStateChangeListener?.let { recyclerView?.removeOnAttachStateChangeListener(it) }
		recyclerAttachStateChangeListener = null
		FloatingScrollableManager.clearInstance(this, recyclerView as? SeslScrollable)
		recyclerViewRef?.clear()
		recyclerViewRef = null
	}

	/** Unregisters the attached [NestedScrollView]. */
	fun clearNestedScroll() {
		val nestedScrollView = nestedScrollViewRef?.get()
		nestedScrollAttachStateChangeListener?.let { nestedScrollView?.removeOnAttachStateChangeListener(it) }
		nestedScrollAttachStateChangeListener = null
		FloatingScrollableManager.clearInstance(this, nestedScrollView as? SeslScrollable)
		nestedScrollViewRef?.clear()
		nestedScrollViewRef = null
	}

	/** Unregisters the attached scrollable content view. */
	fun clearScrollable() {
		FloatingScrollableManager.clearInstance(this, getScrollable())
		scrollabelViewRef?.clear()
		scrollabelViewRef = null
	}

	/** Clears the attached scrollable view from the manager. */
	fun clearFloatingScrollableView() {
		currentFloatingScrollableManager?.removeSeslScrollableListener(scrollableListener)
		clearRecyclerView()
		clearNestedScroll()
		clearScrollable()
		currentFloatingScrollableManager = null
	}

	private fun clearPostInvalidateHandler() {
		postInvalidateHandler.removeCallbacksAndMessages(null)
		requestUpdatePosted = false
	}

	/** Active [FloatingScrollableManager] coordinating scroll events and bounds. */
	fun getFloatingScrollableManager(): FloatingScrollableManager {
		return FloatingScrollableManager.getInstance(this, getScrollableView())
				.also { currentFloatingScrollableManager = it }
	}

	/** Returns active registered [RecyclerView] or `null`. */
	fun getRecyclerView(): RecyclerView? = recyclerViewRef?.get()

	/** Returns active registered [NestedScrollView] or `null`. */
	fun getNestedScrollView(): NestedScrollView? = nestedScrollViewRef?.get()

	private fun getScrollable(): SeslScrollable? = scrollabelViewRef?.get()

	private fun getScrollableView(): SeslScrollable? = (getRecyclerView() as? SeslScrollable)
			?: (getNestedScrollView() as? SeslScrollable) ?: getScrollable()

	/** Resets accumulated scroll distance used for hide transition triggers. */
	fun resetHideTransitionCondition() {
		scrollIdleCheckHandler.removeCallbacks(resetHideTransitionRunnable)
		totalScrollY = 0
	}

	/**
	 * Evaluates scroll delta and triggers show or hide transitions accordingly.
	 *
	 * @param dy Vertical scroll distance delta.
	 */
	fun checkAndRunHideTransition(dy: Int) {
		if (dy <= 0) {
			resetHideTransitionCondition()
			if (!isVisible() || shouldRestoreGoToTop()) {
				startViewAlphaAnimation(show = true, dispatchedByScroll = true)
				return
			}
			return
		}
		if (isVisible()) {
			totalScrollY += dy
		}
		if (totalScrollY > hideStartScrollRange) {
			startViewAlphaAnimation(show = false, dispatchedByScroll = true)
			resetHideTransitionCondition()
		}
	}

	private fun shouldRestoreGoToTop(): Boolean = !enableScrollTransition && isGoToTopSuppressed

	/**
	 * Sets the visibility state of the floating layout.
	 *
	 * @param show Target visibility (`true` to show, `false` to hide).
	 * @param animation Whether to animate the transition.
	 * @param force Force execution regardless of current state.
	 */
	fun setLayoutVisibility(show: Boolean, animation: Boolean = true, force: Boolean = false) {
		if (needUpdateProjectionBackgroundBounds) {
			projectionView.startProjectionViewItemAnimation(false)
			needUpdateProjectionBackgroundBounds = false
		}
		startViewAlphaAnimation(
			show = show,
			immediately = !animation,
			force = force,
			dispatchedByScroll = false,
			dispatchedByVisibilityChange = true
		)
	}

	/**
	 * Returns the current visible state of this layout.
	 *
	 * @return The active [FloatingLayoutState].
	 */
	fun getVisibleState(): FloatingLayoutState {
		layoutAlphaAnimator?.let { animator ->
			if (animator.isRunning) {
				return when (viewTargetAlpha) {
					1f -> FloatingLayoutState.STATE_ANIMATING_TO_SHOW
					0f -> FloatingLayoutState.STATE_ANIMATING_TO_HIDE
					else -> FloatingLayoutState.STATE_SHOW
				}
			}
		}
		return when (alpha) {
			1f -> FloatingLayoutState.STATE_SHOW
			0f -> FloatingLayoutState.STATE_HIDE
			else -> {
				error("Invalid State on getVisibleState from:$alpha to:$viewTargetAlpha now:$alpha")
				FloatingLayoutState.STATE_SHOW
			}
		}
	}

	/**
	 * Starts the alpha transition animation to show or hide the view.
	 *
	 * @param show `true` to show, `false` to hide.
	 * @param immediately If `true`, applies alpha without animation.
	 * @param force Force execution regardless of current target alpha.
	 * @param dispatchedByScroll Indicates if animation was triggered by scroll events.
	 * @param dispatchedByVisibilityChange Indicates if animation was triggered by visibility changes.
	 */
	open fun startViewAlphaAnimation(
		show: Boolean,
		immediately: Boolean = false,
		force: Boolean = false,
		dispatchedByScroll: Boolean = false,
		dispatchedByVisibilityChange: Boolean = false
	) {
		if (!enableScrollTransition && !force) {
			if (!(dispatchedByScroll && show) && show) {
				return
			}
			updateGoToTopVisibility(show)
			return
		}
		val animator = layoutAlphaAnimator ?: return
		if (!show || alpha == 0f || force || (animator.isRunning && viewTargetAlpha == 0f)) {
			if (show) {
				stopPostShowRunnable()
			}
			info("StartViewAlphaAnimation show:$show immediately:$immediately")
			val targetAlpha = if (show) 1f else 0f
			if (targetAlpha == 1f) {
				startSpringScaleAnimation(SPRING_SCALE_MIN, SPRING_SCALE_MAX)
			} else {
				startSpringScaleAnimation(SPRING_SCALE_MAX, SPRING_SCALE_MIN)
			}
			animator.duration = if (immediately) 0L else getDuration(floatingAnimationConfigs.layoutAlphaAnimationDuration)
			if (!animator.isRunning) {
				animator.setFloatValues(alpha, targetAlpha)
				viewTargetAlpha = targetAlpha
				animator.start()
				onAlphaAnimationStarted(targetAlpha == 1f, dispatchedByVisibilityChange)
				return
			}
			if (viewTargetAlpha == targetAlpha) {
				return
			}
			viewTargetAlpha = targetAlpha
			animator.end()
			animator.setFloatValues(alpha, targetAlpha)
			animator.start()
			onAlphaAnimationStarted(targetAlpha == 1f, dispatchedByVisibilityChange)
		}
	}

	/**
	 * Starts a spring scale animation on the floating container and its reference views.
	 *
	 * @param oldValue Initial scale factor.
	 * @param newValue Target scale factor.
	 */
	fun startSpringScaleAnimation(oldValue: Float, newValue: Float) {
		val aware = floatingAware
		val set = LinkedHashSet<View>()
		set.addAll(projectionView.prjBgViewList)
		if (aware != null && (aware.getFloatingAwareOptions() and FloatingAware.FLOATING_AWARE_OPTION_SCALE_WITHOUT_CONTENT_VIEW) == 0) {
			for (positionType in PositionType.entries) {
				val referenceViews = projectionView.getVisibleReferenceViews(
					aware.getReferenceViews(positionType),
					aware.getReferenceView(positionType),
					(aware.getFloatingAwareOptions() and FloatingAware.FLOATING_AWARE_OPTION_SKIP_REFER_VISIBLE_CHECK) != 0
				)
				if (referenceViews != null) {
					set.addAll(referenceViews)
				}
			}
		} else {
			debug("Not added floating content scale animation")
		}
		startSpringScaleAnimationInternal(oldValue, newValue, set.toList())
	}

	/**
	 * Internal implementation of spring scale animation using [SpringAnimation].
	 *
	 * @param oldValue Initial scale value.
	 * @param newValue Target scale value.
	 * @param targetViews List of views to apply scale animation to.
	 */
	fun startSpringScaleAnimationInternal(oldValue: Float, newValue: Float, targetViews: List<View>) {
		debug("startSpringScaleAnimationInternal $oldValue-> $newValue, targetViews=$targetViews")
		val springAnimation = SpringAnimation(FloatValueHolder())
		val springForce = SpringForce()
		springForce.dampingRatio = SPRING_DAMPING_RATIO
		springForce.stiffness = SPRING_STIFFNESS
		springForce.finalPosition = newValue * SPRING_SCALE_FACTOR
		springAnimation.spring = springForce
		springAnimation.setStartValue(oldValue * SPRING_SCALE_FACTOR)
		springAnimation.addUpdateListener { _, value, _ ->
			val s = value / SPRING_SCALE_FACTOR
			for (targetView in targetViews) {
				targetView.scaleX = s
				targetView.scaleY = s
			}
		}
		springAnimation.addEndListener { _, _, _, _ ->
			if (startBackgroundItemAnimationOnAnimEnd) {
				projectionView.startProjectionViewItemAnimation(true)
				startBackgroundItemAnimationOnAnimEnd = false
			}
		}
		springAnimation.start()
		scaleSpringAnimation = springAnimation
	}

	private fun getDuration(require: Long): Long = if (skipAnimation) 0L else require

	private fun onAlphaAnimationProgress(value: Float) {
		layoutAlphaAnimationListener?.onProgress(value)
	}

	private fun onAlphaAnimationStarted(toShow: Boolean, dispatchedByVisibilityChange: Boolean = false) {
		layoutAlphaAnimationListener?.onStart(toShow)
		if (dispatchedByVisibilityChange && toShow) {
			return
		}
		updateGoToTopVisibility(toShow)
	}

	private fun onAlphaAnimationEnded() {
		layoutAlphaAnimationListener?.onEnd()
	}

	private fun updateGoToTopVisibility(toShow: Boolean) {
		val manager = getFloatingScrollableManager()
		isGoToTopSuppressed = !toShow
		manager.setGoToTopSuppressed(!toShow)
		if (toShow) {
			manager.showGoToTop()
		} else {
			manager.hideGoToTop()
		}
	}

	private fun updateVisibleLayoutState() {
		var lastState = FloatingLayoutState.STATE_NONE
		for (listener in layoutStateListener) {
			val currentState = if (alpha == 0f) {
				FloatingLayoutState.STATE_HIDE
			} else {
				FloatingLayoutState.STATE_SHOW
			}
			if (currentState != lastState) {
				listener.onStateChange(this, currentState)
				lastState = currentState
			}
		}
	}

	/**
	 * Sends callbacks to [FloatingAware] for starting show or hide transitions.
	 */
	fun forceSendAwareCallback() {
		val appBarLayout = getAppBarLayout()
		if (appBarLayout != null) {
			projectionView.sendFloatingAwareStartCallbackIfNeed(
				if ((appBarLayout.seslGetCurrentAppBarState() and APP_BAR_STATE_HIDE) != 0) AWARE_CALLBACK_SHOW else AWARE_CALLBACK_HIDE,
				force = true
			)
		}
	}

	/** Starts the delayed post-show runnable. */
	fun startPostShowRunnable() {
		postShowHandler.removeCallbacks(delayShowRunnable)
		postShowHandler.postDelayed(delayShowRunnable, POST_SHOW_DELAY_MS)
	}

	/** Cancels the delayed post-show runnable. */
	fun stopPostShowRunnable() {
		postShowHandler.removeCallbacks(delayShowRunnable)
	}

	/** Calculates and applies elevation padding. */
	open fun setPaddingForElevation() {
		val fMax = maxOf(
			projectionView.prjBgEndFirstView.elevation,
			projectionView.prjBgStartFirstView.elevation,
			projectionView.prjBgStartSecondView.elevation
		)
		val customPad = customPadding
		if (customPad != null) {
			setPadding(customPad.left, customPad.top, customPad.right, customPad.bottom)
		} else if (fMax > 0f) {
			val pad = fMax.toInt()
			setPadding(
				paddingLeft,
				if (paddingTop == 0) pad else paddingTop,
				paddingRight,
				if (paddingBottom == 0) pad * 2 else paddingBottom
			)
		}
	}

	/**
	 * Sets the background color for all floating projection items.
	 *
	 * @param color Color integer value.
	 */
	fun setColorForFloatingBackground(color: Int) {
		val colorStateList = ColorStateList.valueOf(color)
		projectionView.prjBgEndFirstView.setGradientBackgroundColor(colorStateList)
		projectionView.prjBgStartFirstView.setGradientBackgroundColor(colorStateList)
		projectionView.prjBgStartSecondView.setGradientBackgroundColor(colorStateList)
	}

	/**
	 * Sets tint color for all floating background projection shapes.
	 *
	 * @param tintColor Tint color integer.
	 */
	fun setTintForFloatingBackground(tintColor: Int) {
		projectionView.prjBgEndFirstView.setBackgroundTint(tintColor)
		projectionView.prjBgStartFirstView.setBackgroundTint(tintColor)
		projectionView.prjBgStartSecondView.setBackgroundTint(tintColor)
	}

	/**
	 * Sets corner radius for floating background projection shapes.
	 *
	 * @param radius Corner radius in pixels.
	 */
	fun setFloatingBackgroundRadius(radius: Float) {
		for (bgView in projectionView.prjBgViewList) {
			bgView.setGradientBackgroundCornerRadius(radius)
		}
	}

	/**
	 * Sets elevation for floating background projection shapes.
	 *
	 * @param elevation Elevation pixel value or `null` for default.
	 */
	fun setElevationForFloatingBackground(elevation: Float?) {
		projectionView.setElevation(elevation)
	}

	/**
	 * Animates elevation and blur curve for floating background shapes.
	 *
	 * @param elevation Target elevation.
	 * @param animate Whether to animate the transition.
	 */
	fun animateElevationNBlurForFloatingBackground(elevation: Float, animate: Boolean = true) {
		debug("animateElevationNBlurForFloatingBackground elevation=$elevation animate=$animate skipAnimation=$skipAnimation")
		projectionView.setElevationAndBlur(elevation, skipAnimation || !animate)
	}

	/**
	 * Configures whether scroll transitions are enabled.
	 *
	 * @param enable `true` to enable scroll transitions, `false` to disable.
	 * @param show Target visibility when disabled.
	 */
	fun enableScrollTransition(enable: Boolean, show: Boolean = true) {
		info("FloatingLayout Transition enabled:$enable show:$show")
		enableScrollTransition = enable
		if (!enable) {
			setLayoutVisibility(show, animation = false)
		}
	}

	/**
	 * Enables dynamic GoToTop offset adjustments.
	 * This is disabled by default.
	 */
	fun enableAutoGoToTopOffsetMove() {
		manageGoToTopOffset = true
		getFloatingScrollableManager().manageGoToTopOffset = true
	}

	/** Disables dynamic GoToTop offset adjustments. */
	fun disableAutoGoToTopOffsetMove() {
		manageGoToTopOffset = false
		getFloatingScrollableManager().manageGoToTopOffset = false
	}

	/**
	 * Configures whether automatic fading edge bottom offset is enabled.
	 * This is enabled by default.
	 */
	fun enableAutoFadingEdgeBottomOffset(enable: Boolean) {
		manageFadingEdgeBottomOffset = enable
		getFloatingScrollableManager().manageFadingEdgeBottomOffset = enable
	}

	/**
	 * Sets the bottom window inset pixel value for adjusting bottom
	 * boundary for scrollbars, hover scroller and goToTop button.
	 * Requires [manageGoToTopOffset] to be enabled.
	 *
	 * @param offset Bottom inset in pixels.
	 */
	fun setWindowBottomInset(offset: Int) {
		windowInsetBottom = offset
		getFloatingScrollableManager().windowInsetBottom = offset
	}


	/** Registers an [OnAlphaAnimationListener]. */
	fun setLayoutAlphaAnimationListener(listener: OnAlphaAnimationListener?) {
		layoutAlphaAnimationListener = listener
	}

	/** Adds an animator listener for projection background matching animations. */
	fun addFloatingBackgroundAnimatorListener(listener: Animator.AnimatorListener) {
		projectionView.addAnimatorListener(listener)
	}

	/**
	 * Shows or hides floating item projection backgrounds.
	 *
	 * @param show `true` to show projection shapes, `false` to hide.
	 * @param animate Whether to animate the alpha transition.
	 */
	fun showFloatingItemBackground(show: Boolean, animate: Boolean = true) {
		projectionView.startProjectionViewItemAnimation(animate)
		projectionView.startProjectionViewAlphaAnimation(if (show) 1.0f else 0.0f, !animate)
		if (show) {
			invalidateBlurTargetView()
		}
	}

	/**
	 * Starts item projection background matching animation.
	 *
	 * @param animate Whether to animate background bounds transitions.
	 */
	fun startFloatingItemBackgroundRectAnimation(animate: Boolean = true) {
		projectionView.startProjectionViewItemAnimation(animate)
	}

	/** Returns `true` if floating item background is visible (alpha == 1.0). */
	fun isShowingFloatingItemBackground(): Boolean = projectionView.alpha == 1.0f

	/** Returns `true` if this layout container is visible (alpha == 1.0). */
	fun isVisible(): Boolean = alpha == 1.0f

	/** Applies background blur info across registered blur target views. */
	override fun applyBlurInfo(context: Context): Boolean {
		val blurTargetSet = blurInvalidateTargetViews.keys.filterIsInstance<BlurSupportable>()
		var result = true
		for (blurSupportable in blurTargetSet) {
			if (!blurSupportable.applyBlurInfo(context)) {
				result = false
			}
		}
		return result
	}

	/** Clears background blur info across registered blur target views. */
	override fun clearBlurInfo(context: Context) {
		val blurTargetSet = blurInvalidateTargetViews.keys.filterIsInstance<BlurSupportable>()
		for (blurSupportable in blurTargetSet) {
			blurSupportable.clearBlurInfo(context)
		}
	}

	/** Sets blur mode across registered blur target views. */
	override fun setBlurMode(semBlurInfoMode: Int) {
		val blurTargetSet = blurInvalidateTargetViews.keys.filterIsInstance<BlurSupportable>()
		for (blurSupportable in blurTargetSet) {
			blurSupportable.setBlurMode(semBlurInfoMode)
		}
	}

	/** Returns `true` if blur is currently applied to any registered blur target view. */
	override fun isBlurApplied(): Boolean {
		val blurTargetSet = blurInvalidateTargetViews.keys.filterIsInstance<BlurSupportable>()
		if (blurTargetSet.isEmpty()) return false
		for (blurSupportable in blurTargetSet) {
			if (blurSupportable.isBlurApplied()) return true
		}
		return false
	}

	/** Measures child views, elevation padding, and projection view bounds. */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (childCount <= 1) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}
		if (showBackgroundAtFirst) {
			setPaddingForElevation()
		}
		val childAt1 = getChildAt(VIEW_INDEX_CONTENT)
		measureChild(childAt1, widthMeasureSpec, heightMeasureSpec)
		val paddingBottom = paddingTop + paddingBottom + childAt1.measuredHeight
		setMeasuredDimension(widthMeasureSpec, paddingBottom)
		projectionView.measure(
			MeasureSpec.makeMeasureSpec(measuredWidth - paddingStart - paddingEnd, MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(childAt1.measuredHeight, MeasureSpec.EXACTLY)
		)
	}

	/** Handles view layout and registers offset change listeners on [AppBarLayout]. */
	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		if (isFirstLayout) {
			getAppBarLayout()?.addOnOffsetChangedListener { appBarLayout, offset ->
				if (offset == 0 || appBarVerticalOffset != offset) {
					appBarVerticalOffset = offset
					onAppBarOffsetChanged(appBarLayout, offset)
				}
			}
			isFirstLayout = false
		}
		if (showBackgroundAtFirst && parent !is CoordinatorLayout) {
			showFloatingItemBackground(show = true, animate = false)
		} else if (projectionView.parent != null && projectionView.parent is FloatingToolbarLayout) {
			startFloatingItemBackgroundRectAnimation(false)
		}
		super.onLayout(changed, left, top, right, bottom)
	}

	/** Reacts to window visibility changes to enable or block pre-draw blur invalidation. */
	override fun onWindowVisibilityChanged(visibility: Int) {
		super.onWindowVisibilityChanged(visibility)
		debug("onWindowVisibilityChanged visibility=$visibility $this")
		if (visibility == VISIBLE) {
			blockBlurInvalidateOnPreDraw = false
			viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
		} else {
			blockBlurInvalidateOnPreDraw = true
			clearPostInvalidateHandler()
			viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
			projectionView.removeProjectionItemAnimationPreDrawListener()
		}
	}

	/** Cleans up handlers, lifecycle observers, and projection view state when detached. */
	override fun onDetachedFromWindow() {
		debug("onDetachedFromWindow $this")
		clearFloatingScrollableView()
		if (context is LifecycleOwner && lifeCycleObserver != null) {
			(context as LifecycleOwner).lifecycle.removeObserver(lifeCycleObserver!!)
		}
		lifeCycleObserver = null
		scrollIdleCheckHandler.removeCallbacks(resetHideTransitionRunnable)
		postShowHandler.removeCallbacks(delayShowRunnable)
		projectionView.resetOldRect()
		projectionView.endElevationBlurAnimation()
		projectionView.removeProjectionItemAnimationPreDrawListener()
		clearPostInvalidateHandler()
		blockBlurInvalidateOnPreDraw = false
		viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
		super.onDetachedFromWindow()
	}

	/** Intercepts touch events when layout is not fully visible. */
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		if (getVisibleState() != FloatingLayoutState.STATE_SHOW) {
			return true
		}
		return super.onInterceptTouchEvent(ev)
	}

	/**
	 * Information about reference view location and size on screen.
	 *
	 * @param left Left screen coordinate.
	 * @param right Right screen coordinate.
	 * @param height Height in pixels.
	 * @param locationScreenX X coordinate on screen.
	 * @param locationScreenY Y coordinate on screen.
	 */
	data class ReferViewInformation(
		val left: Int = 0,
		val right: Int = 0,
		val height: Int = 0,
		val locationScreenX: Int = 0,
		val locationScreenY: Int = 0
	)

	/**
	 * Container view that hosts projection background items for items inside floating toolbars.
	 *
	 * @param context The Context the view is running in.
	 */
	inner class SeslProjectionView(context: Context) : FrameLayout(context), MaterialLogTag {

		override val logTag: String = PROJECTION_VIEW_TAG

		private val forceHideTypes = ArrayList<PositionType>()
		private val blurElevationPolicy = SeslFloatingBlurElevationPolicy(context)
		private val floatingBlurElevationAnimator = SeslFloatingBlurElevationAnimator(blurElevationPolicy)

		/** Projection background view for [FloatingAware.PositionType.END_FIRST]. */
		val prjBgEndFirstView = SeslProjectionBackgroundView(context, blurElevationPolicy)

		/** Projection background view for [FloatingAware.PositionType.START_FIRST]. */
		val prjBgStartFirstView = SeslProjectionBackgroundView(context, blurElevationPolicy)

		/** Projection background view for [FloatingAware.PositionType.START_SECOND]. */
		val prjBgStartSecondView = SeslProjectionBackgroundView(context, blurElevationPolicy)

		/** List of all projection background views. */
		val prjBgViewList: List<SeslProjectionBackgroundView> =
			listOf(prjBgStartFirstView, prjBgStartSecondView, prjBgEndFirstView)

		private val oldRectStartFirst = Rect()
		private val oldRectStartSecond = Rect()
		private val oldRectEndFirst = Rect()
		private val oldRects = mapOf(
			PROJECTION_BG_TAG_START_FIRST to oldRectStartFirst,
			PROJECTION_BG_TAG_START_SECOND to oldRectStartSecond,
			PROJECTION_BG_TAG_END_FIRST to oldRectEndFirst
		)

		private var prjViewAlphaAnimator: ObjectAnimator
		private val mPrjAlphaAnimProperty = object : Property<View, Float>(Float::class.java, "SeslProjectionViewAlpha") {
			override fun get(view: View): Float = view.alpha
			override fun set(view: View, value: Float) {
				view.alpha = value
			}
		}

		private var prjViewAnimationConfig = FloatingAnimationConfig()
		private var lastFloatingBackgroundElevationPx: Float = Float.NaN
		private var pendingProjectionItemAnimationAnimate: Boolean = true
		private val projectionItemAnimationPreDrawListener = ViewTreeObserver.OnPreDrawListener {
			removeProjectionItemAnimationPreDrawListener()
			startBackgroundMatchingAnimationImpl(!pendingProjectionItemAnimationAnimate)
			true
		}

		private var projectionItemAnimationPreDrawRegistered: Boolean = false
		private var configChanged: Boolean = false
		private var lastAwareCallback: Int = AWARE_CALLBACK_NONE

		/** Target end alpha value for projection view alpha animation. */
		private var prjViewAnimateAlphaEndValue: Float = 0f

		init {
			prjViewAlphaAnimator = ObjectAnimator.ofFloat(this, mPrjAlphaAnimProperty, alpha)
			prjViewAlphaAnimator.duration = prjViewAnimationConfig.backgroundAlphaAnimationDuration
			prjViewAlphaAnimator.interpolator = prjViewAnimationConfig.backgroundAlphaAnimationInterpolator
			alpha = 0f

			prjViewAlphaAnimator.addListener(object : Animator.AnimatorListener {
				override fun onAnimationStart(animation: Animator) {
					sendFloatingAwareStartCallbackIfNeed(
						if (prjViewAnimateAlphaEndValue == 1f) AWARE_CALLBACK_SHOW else AWARE_CALLBACK_HIDE,
						force = false
					)
				}

				override fun onAnimationEnd(animation: Animator) {}
				override fun onAnimationCancel(animation: Animator) {}
				override fun onAnimationRepeat(animation: Animator) {}
			})

			prjBgStartFirstView.initialize(
				PROJECTION_BG_TAG_START_FIRST,
				R.id.floating_toolbar_item_background_start_first
			)
			prjBgStartSecondView.initialize(
				PROJECTION_BG_TAG_START_SECOND,
				R.id.floating_toolbar_item_background_start_second
			)
			prjBgEndFirstView.initialize(
				PROJECTION_BG_TAG_END_FIRST,
				R.id.floating_toolbar_item_background_end_first
			)

			addView(prjBgEndFirstView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
			addView(prjBgStartFirstView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
			addView(prjBgStartSecondView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
		}

		private fun ensureItemBgVisible() {
			prjBgStartFirstView.isVisible = !forceHideTypes.contains(PositionType.START_FIRST)
			prjBgStartSecondView.isVisible = !forceHideTypes.contains(PositionType.START_SECOND)
			prjBgEndFirstView.isVisible = !forceHideTypes.contains(PositionType.END_FIRST)
		}

		private fun getProjectionBackgroundView(type: PositionType): SeslProjectionBackgroundView {
			return when (type) {
				PositionType.START_FIRST -> prjBgStartFirstView
				PositionType.START_SECOND -> prjBgStartSecondView
				PositionType.END_FIRST -> prjBgEndFirstView
			}
		}

		/** Returns the projection background view for a position type. */
		fun getBackgroundView(type: PositionType): View = getProjectionBackgroundView(type)

		private fun getReferGroupViewsLocationOnScreen(referGroupViews: List<View>): ReferViewInformation {
			val loc = IntArray(2)
			var maxBottom = Int.MIN_VALUE
			var minLeft = Int.MAX_VALUE
			var minRightX = Int.MAX_VALUE
			var minTopY = Int.MAX_VALUE
			var maxRight = Int.MIN_VALUE

			for (view in referGroupViews) {
				view.getLocationOnScreen(loc)
				if (view.scaleX != 1.0f || view.scaleY != 1.0f) {
					loc[0] += ((view.scaleX - 1.0f) * view.pivotX).toInt()
					loc[1] += ((view.scaleY - 1.0f) * view.pivotY).toInt()
					debug(
						"getReferGroupViewsLocationOnScreen view=$view, scale(${view.scaleX},${view.scaleY}), location=[${loc[0]},${loc[1]}]"
					)
				}
				minLeft = minOf(minLeft, loc[0])
				maxRight = maxOf(maxRight, view.width + loc[0])
				maxBottom = maxOf(maxBottom, view.height + loc[1])
				minRightX = minOf(minRightX, loc[0])
				minTopY = minOf(minTopY, loc[1])
			}
			return ReferViewInformation(minLeft, maxRight, maxBottom - minTopY, minRightX, minTopY)
		}

		/** Returns the parent [FloatingGroupLayout] container safely. */
		fun getSafeParentFloatingLayout(): FloatingGroupLayout? {
			val parentView = parent
			if (parentView is FloatingGroupLayout) {
				return parentView
			}
			warn(
				"SeslProjectionView must have a FloatingGroupLayout as its parent, but found: ${parentView?.javaClass?.simpleName}"
			)
			return null
		}

		private fun getTargetRect(
			bgView: SeslProjectionBackgroundView,
			referGroupViews: List<View>,
			animation: Boolean,
			paddingRect: Rect
		): Rect? {
			for (v in referGroupViews) {
				if (!v.isAttachedToWindow || v.parent == null) {
					warn("${bgView.tag} Floating ReferView is detached")
					return null
				}
			}
			val info = getReferGroupViewsLocationOnScreen(referGroupViews)
			val refLoc = intArrayOf(info.locationScreenX, info.locationScreenY)
			val parentLoc = IntArray(2)
			val safeParent = getSafeParentFloatingLayout() ?: return Rect()
			safeParent.getLocationOnScreen(parentLoc)

			val oldRect = oldRects[bgView.tag as? String] ?: Rect()
			bgView.getViewRect(oldRect)

			val topOffset = (refLoc[1] - parentLoc[1]) - top
			val leftPadding = if (layoutDirection == LAYOUT_DIRECTION_RTL) paddingRect.right else paddingRect.left
			val rightPadding = if (layoutDirection == LAYOUT_DIRECTION_RTL) paddingRect.left else paddingRect.right

			return Rect(
				((info.left - parentLoc[0]) - safeParent.paddingLeft) + leftPadding,
				topOffset + paddingRect.top,
				((info.right - parentLoc[0]) - safeParent.paddingLeft) - rightPadding,
				(info.height + topOffset) - paddingRect.bottom
			)
		}

		private fun matchingLayoutPosition(
			bgView: SeslProjectionBackgroundView,
			referGroupViews: List<View>,
			animation: Boolean,
			paddingRect: Rect
		) {
			val rect = oldRects[bgView.tag as? String] ?: Rect()
			var shouldAnimate = if (rect.isEmpty) false else animation
			if (rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0) {
				shouldAnimate = false
			}
			if (bgView.alpha == 0f) {
				shouldAnimate = false
			}
			val targetRect = getTargetRect(bgView, referGroupViews, animation, paddingRect) ?: return
			val widthChanged = if (rect.width() != targetRect.width()) shouldAnimate else false

			info(
				"[FloatingItemBG Animation: anim:$animation should:$widthChanged tag[${bgView.tag}] hashCode{${bgView.hashCode()}} visible:${bgView.visibility} alpha:${bgView.alpha} $rect -> $targetRect, paddingRect:${paddingRect.toShortString()} ref:$referGroupViews, ${bgView.parent?.parent} ${bgView.parent?.parent?.hashCode()}"
			)

			if (widthChanged) {
				bgView.animateToFinalPosition(targetRect)
				bgView.setOnResizeUpdate {
					val safeParent = getSafeParentFloatingLayout()
					if (safeParent != null) {
						val realTimeTarget = getTargetRect(bgView, referGroupViews, animation, paddingRect)
						if (realTimeTarget != null && bgView.lastFinalRect != realTimeTarget) {
							bgView.animateToFinalPosition(realTimeTarget)
							info(
								"[FloatingItemBG Animation] ${bgView.tag} changed newRect:$targetRect viewRect:${bgView.lastFinalRect} realTimeRect:$realTimeTarget"
							)
						}
					}
				}
			} else {
				bgView.setFinalPosition(targetRect)
				bgView.setOnResizeUpdate {}
			}
		}

		private fun matchingOrHideBackgroundView(type: PositionType, animate: Boolean) {
			val safeParent = getSafeParentFloatingLayout() ?: return
			val aware = safeParent.floatingAware
			val refView = aware?.getReferenceView(type)
			val projectionBgView = getProjectionBackgroundView(type)
			val visibleRefViews = aware?.let {
				getVisibleReferenceViews(
					it.getReferenceViews(type),
					refView,
					(it.getFloatingAwareOptions() and FloatingAware.FLOATING_AWARE_OPTION_SKIP_REFER_VISIBLE_CHECK) != 0
				)
			}
			val inset = aware?.getReferenceViewInset(type) ?: Rect()
			if (!forceHideTypes.contains(type) && visibleRefViews != null) {
				matchingLayoutPosition(projectionBgView, visibleRefViews, animate, inset)
			}
			projectionBgView.startBackgroundViewAlphaAnimation(visibleRefViews != null, animate)
		}

		private fun startBackgroundMatchingAnimationImpl(immediately: Boolean = false) {
			val safeParent = getSafeParentFloatingLayout() ?: return
			val animate = !(immediately || configChanged || safeParent.skipAnimation)
			configChanged = false
			if (safeParent.scaleX != 1.0f) {
				safeParent.startBackgroundItemAnimationOnAnimEnd = true
				return
			}
			for (positionType in PositionType.entries) {
				matchingOrHideBackgroundView(positionType, animate)
			}
		}

		/**
		 * Animates projection view container alpha.
		 *
		 * @param toValue Target alpha value.
		 * @param immediately Whether to apply target alpha without animation.
		 */
		fun startProjectionViewAlphaAnimation(toValue: Float, immediately: Boolean = false) {
			val targetView = prjViewAlphaAnimator.target as? View
			if (targetView == null || targetView.alpha != toValue || prjViewAlphaAnimator.isRunning) {
				if (prjViewAlphaAnimator.isRunning && toValue == prjViewAnimateAlphaEndValue) {
					return
				}
				if (prjViewAlphaAnimator.isRunning && prjViewAnimateAlphaEndValue + toValue == 1.0f) {
					prjViewAlphaAnimator.cancel()
				}
				ensureItemBgVisible()
				val safeParent = getSafeParentFloatingLayout() ?: return
				if (immediately || safeParent.skipAnimation) {
					Log.d(PROJECTION_VIEW_TAG, "ProjectionBackgroundAnimation: SKIP ANIMATION to=$toValue")
					if (prjViewAlphaAnimator.isRunning) {
						prjViewAlphaAnimator.cancel()
					}
					sendFloatingAwareStartCallbackIfNeed(if (alpha != 0f) AWARE_CALLBACK_HIDE else AWARE_CALLBACK_SHOW, force = false)
					prjViewAnimateAlphaEndValue = toValue
					alpha = toValue
					// custom (fix): do not snap pills without a visible reference (e.g. a menu
					// that became empty) to the container target alpha; otherwise the stale
					// pill of the previous menu re-appears and then fades = blink
					val aware = safeParent.floatingAware
					val skipVisibleCheck = aware != null &&
						(aware.getFloatingAwareOptions() and FloatingAware.FLOATING_AWARE_OPTION_SKIP_REFER_VISIBLE_CHECK) != 0
					for (type in PositionType.entries) {
						val bgView = getProjectionBackgroundView(type)
						if (toValue == 1.0f && aware != null && bgView.alpha != toValue) {
							val visibleRefViews = getVisibleReferenceViews(
								aware.getReferenceViews(type),
								aware.getReferenceView(type),
								skipVisibleCheck
							)
							if (visibleRefViews == null) {
								bgView.alpha = 0f
								continue
							}
						}
						bgView.alpha = toValue
					}
					return
				}
				prjViewAlphaAnimator.duration = safeParent.getDuration(prjViewAnimationConfig.backgroundAlphaAnimationDuration)
				Log.d(PROJECTION_VIEW_TAG, "ProjectionBackgroundAnimation: to=$toValue, duration=${prjViewAlphaAnimator.duration}, isRunning=${prjViewAlphaAnimator.isRunning}")
				if (!prjViewAlphaAnimator.isRunning) {
					prjViewAnimateAlphaEndValue = toValue
					prjViewAlphaAnimator.setFloatValues(alpha, toValue)
					prjViewAlphaAnimator.start()
				} else {
					if (prjViewAnimateAlphaEndValue == toValue) return
					prjViewAnimateAlphaEndValue = toValue
					prjViewAlphaAnimator.end()
					prjViewAlphaAnimator.setFloatValues(alpha, toValue)
					prjViewAlphaAnimator.start()
				}
			}
		}

		/**
		 * Shows or hides item projection backgrounds.
		 *
		 * @param animate Whether to animate background bounds matching.
		 */
		fun startProjectionViewItemAnimation(animate: Boolean = true) {
			val safeParent = getSafeParentFloatingLayout() ?: return
			pendingProjectionItemAnimationAnimate = animate
			removeProjectionItemAnimationPreDrawListener()
			safeParent.viewTreeObserver.addOnPreDrawListener(projectionItemAnimationPreDrawListener)
			projectionItemAnimationPreDrawRegistered = true
		}

		/** Sends callbacks to [FloatingAware] for background show/hide. */
		fun sendFloatingAwareStartCallbackIfNeed(callbackType: Int, force: Boolean = false) {
			if (force || (lastAwareCallback != callbackType && callbackType != AWARE_CALLBACK_NONE)) {
				val safeParent = getSafeParentFloatingLayout()
				val aware = safeParent?.floatingAware
				if (aware != null) {
					if (callbackType == AWARE_CALLBACK_SHOW) {
						aware.onStartShowFloatingBackground()
					} else if (callbackType == AWARE_CALLBACK_HIDE) {
						aware.onStartHideFloatingBackground()
					}
					lastAwareCallback = callbackType
				}
			}
		}

		/** Forces hiding a projection background position type. */
		fun addHideBackgroundType(type: PositionType) {
			forceHideTypes.add(type)
			ensureItemBgVisible()
		}

		/** Restores visibility for a projection background position type. */
		fun removeHideBackgroundType(type: PositionType) {
			forceHideTypes.remove(type)
		}

		/** Sets elevation on all projection background views. */
		fun setElevation(elevation: Float?) {
			val elevPx = elevation ?: resources.getDimension(R.dimen.sesl_floating_toolbar_projection_background_elevation)
			prjBgEndFirstView.elevation = elevPx
			prjBgStartFirstView.elevation = elevPx
			prjBgStartSecondView.elevation = elevPx
			lastFloatingBackgroundElevationPx = elevPx
		}

		/** Animates elevation and applies blur curves. */
		fun setElevationAndBlur(newElevation: Float, skipAnimation: Boolean) {
			val currentElev = if (lastFloatingBackgroundElevationPx.isNaN()) {
				maxOf(prjBgEndFirstView.elevation, 0f)
			} else {
				lastFloatingBackgroundElevationPx
			}
			lastFloatingBackgroundElevationPx = newElevation
			floatingBlurElevationAnimator.start(
				currentElev,
				newElevation,
				skipAnimation,
				context
			) { elev, blurCurve ->
				for (bgView in prjBgViewList) {
					bgView.elevation = elev
					bgView.applyBlurCurveIfApplied(blurCurve)
				}
			}
		}

		/** Cancels running elevation and blur animations. */
		fun endElevationBlurAnimation() {
			floatingBlurElevationAnimator.skipToEnd()
		}

		/** Unregisters pre-draw animation listener. */
		fun removeProjectionItemAnimationPreDrawListener() {
			if (projectionItemAnimationPreDrawRegistered) {
				val safeParent = getSafeParentFloatingLayout()
				if (safeParent != null && safeParent.viewTreeObserver.isAlive) {
					safeParent.viewTreeObserver.removeOnPreDrawListener(projectionItemAnimationPreDrawListener)
				}
				projectionItemAnimationPreDrawRegistered = false
			}
		}

		/** Resets cached previous position rectangles. */
		fun resetOldRect() {
			for (rect in oldRects.values) {
				rect.setEmpty()
			}
		}

		/** Sets animation config on projection views. */
		fun setAnimationConfig(animationConfigs: FloatingAnimationConfig) {
			prjViewAnimationConfig = animationConfigs
			for (bgView in prjBgViewList) {
				bgView.setAnimationConfig(animationConfigs)
			}
		}

		/** Registers an animator listener for projection view alpha animation. */
		fun addAnimatorListener(listener: Animator.AnimatorListener) {
			prjViewAlphaAnimator.addListener(listener)
		}

		/** Filters visible reference views for a position type. */
		fun getVisibleReferenceViews(
			referGroupView: List<View>?,
			referView: View?,
			skipVisibleCheck: Boolean
		): List<View>? {
			if (skipVisibleCheck) {
				if (!referGroupView.isNullOrEmpty()) return referGroupView
				if (referView != null) return listOf(referView)
				return null
			}
			if (referGroupView.isNullOrEmpty()) {
				if (referView == null || referView.visibility != VISIBLE) return null
				return listOf(referView)
			}
			val visibleViews = referGroupView.filter { it.visibility == VISIBLE }
			return visibleViews.ifEmpty { null }
		}

		override fun onConfigurationChanged(newConfig: Configuration) {
			super.onConfigurationChanged(newConfig)
			configChanged = true
			debug("onConfigurationChanged $this")
			blurElevationPolicy.update(context)
			val safeParent = getSafeParentFloatingLayout()
			safeParent?.floatingAware?.onFloatingViewConfigurationChanged(alpha == 1f, newConfig)
		}

		override fun onDetachedFromWindow() {
			removeProjectionItemAnimationPreDrawListener()
			endElevationBlurAnimation()
			super.onDetachedFromWindow()
		}

		override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
			if (!changed) return
			val safeParent = getSafeParentFloatingLayout() ?: return
			safeParent.needUpdateProjectionBackgroundBounds = true
			if (alpha == 1f) {
				startProjectionViewItemAnimation(false)
			}
		}

		/**
		 * Individual projection background item supporting blur effects and theme drawables.
		 *
		 * @param context The Context the view is running in.
		 * @param blurElevationPolicy Policy for computing elevation blur curves.
		 * @param attrs Attribute set.
		 * @param defStyleAttr Default style attribute.
		 */
		inner class SeslProjectionBackgroundView(
			context: Context,
			private val blurElevationPolicy: SeslFloatingBlurElevationPolicy,
			attrs: AttributeSet? = null,
			defStyleAttr: Int = 0
		) : View(context, attrs, defStyleAttr), BlurSupportable, MaterialLogTag {

			override val logTag: String = "SeslProjectionBgView"

			/** Last computed final position rectangle. */
			var lastFinalRect: Rect = Rect()

			private var blurMode: Int = BLUR_MODE_DEFAULT
			private var blurInfo: SemBlurInfoState? = null
			private var mBackgroundDrawable: Drawable? = null
			private var onResizeUpdate: () -> Unit = {}
			private var prevTargetAlpha: Float = PREV_TARGET_ALPHA_UNSET
			private var prjBgViewAlphaAnimator: ObjectAnimator
			private var prjBgViewAnimationConfig = FloatingAnimationConfig()
			private val ratio: Float = RESIZE_ANIMATION_RATIO

			/** [ResizeAnimation] for animating position and size changes. */
			val anim: ResizeAnimation

			/** Default background drawable resource ID based on theme. */
			val defaultBgId: Int = OpenThemeResourceDrawableRes(
				ThemeResourceDrawableRes(
					R.drawable.sesl_floating_appbar_round_background_light,
					R.drawable.sesl_floating_appbar_round_background_dark
				),
				ThemeResourceDrawableRes(
					R.drawable.sesl_floating_appbar_round_background_for_theme,
					R.drawable.sesl_floating_appbar_round_background_dark_for_theme
				)
			).getResource(context)

			inline fun setBackgroundTint(@ColorInt tint: Int) {
				background?.mutate()?.setTint(tint)
			}

			inline fun setGradientBackgroundColor(color: ColorStateList) {
				(background?.mutate() as? GradientDrawable)?.color = color
			}

			inline fun setGradientBackgroundCornerRadius(radius: Float) {
				(background?.mutate() as? GradientDrawable)?.cornerRadius = radius
			}

			private val mPrjBgViewAlphaAnimProperty = object : Property<View, Float>(Float::class.java, "SeslPrjBgViewAlpha") {
				override fun get(view: View): Float = view.alpha
				override fun set(view: View, value: Float) {
					view.alpha = value
				}
			}

			init {
				val resizeAnim = ResizeAnimation(ratio)
				resizeAnim.setSpringForce(SPRING_DAMPING_RATIO, SPRING_STIFFNESS)
				resizeAnim.updater = { rectF ->
					onResizeUpdate.invoke()
					val r = Rect()
					rectF.roundOut(r)
					setViewRect(r)
				}
				anim = resizeAnim

				val objAnimator = ObjectAnimator.ofFloat(this, mPrjBgViewAlphaAnimProperty, alpha)
				prjBgViewAlphaAnimator = objAnimator
			}

			/** Initializes tag, view ID, background drawable, and default elevation. */
			fun initialize(tag: String, viewId: Int) {
				setTag(tag)
				replaceAnimator(tag)
				id = viewId
				setBackground(resources.getDrawable(defaultBgId, context.theme))
				elevation = resources.getDimension(R.dimen.sesl_floating_toolbar_projection_background_elevation)
			}

			/** Replaces alpha property animator with a named property. */
			fun replaceAnimator(propertyName: String) {
				val objAnimator = ObjectAnimator.ofFloat(this, object : Property<View, Float>(Float::class.java, propertyName) {
					override fun get(view: View): Float = view.alpha
					override fun set(view: View, value: Float) {
						view.alpha = value
					}
				}, alpha)
				prjBgViewAlphaAnimator = objAnimator
				objAnimator.duration = prjBgViewAnimationConfig.backgroundAlphaAnimationDuration
				objAnimator.interpolator = prjBgViewAnimationConfig.backgroundAlphaAnimationInterpolator
			}

			/** Configures animation parameters. */
			fun setAnimationConfig(config: FloatingAnimationConfig) {
				prjBgViewAnimationConfig = config
				prjBgViewAlphaAnimator.interpolator = config.backgroundAlphaAnimationInterpolator
			}

			/** Sets callback invoked on each resize animation frame. */
			fun setOnResizeUpdate(update: () -> Unit) {
				onResizeUpdate = update
			}

			/** Animates view position and size to target rectangle. */
			fun animateToFinalPosition(rect: Rect) {
				lastFinalRect = rect
				anim.animateToFinalPosition(RectF(rect))
			}

			/** Sets view position and size directly to target rectangle without animation. */
			fun setFinalPosition(rect: Rect) {
				lastFinalRect = rect
				anim.cancel()
				anim.init(RectF(rect))
				setViewRect(rect)
			}

			/** Animates background item alpha to visible or hidden state. */
			fun startBackgroundViewAlphaAnimation(show: Boolean, animate: Boolean) {
				val targetAlpha = if (show) 1f else 0f
				if (alpha != targetAlpha || (prjBgViewAlphaAnimator.isRunning && prevTargetAlpha != targetAlpha)) {
					val safeParent = getSafeParentFloatingLayout()
					if (!animate || (safeParent != null && safeParent.skipAnimation)) {
						if (prjBgViewAlphaAnimator.isRunning) {
							prjBgViewAlphaAnimator.cancel()
						}
						prevTargetAlpha = targetAlpha
						alpha = targetAlpha
						return
					}
					if (prjBgViewAlphaAnimator.isRunning) {
						if (prevTargetAlpha == targetAlpha) {
							return
						} else {
							prjBgViewAlphaAnimator.cancel()
						}
					}
					prevTargetAlpha = targetAlpha
					prjBgViewAlphaAnimator.setFloatValues(alpha, targetAlpha)
					prjBgViewAlphaAnimator.duration = safeParent?.getDuration(prjBgViewAnimationConfig.backgroundAlphaAnimationDuration)
						?: prjBgViewAnimationConfig.backgroundAlphaAnimationDuration
					prjBgViewAlphaAnimator.start()
				}
			}

			override fun setBackground(background: Drawable?) {
				super.setBackground(background)
				mBackgroundDrawable = background
			}

			/** Applies blur curve if background blur is currently enabled. */
			fun applyBlurCurveIfApplied(blurCurve: SemBlurCompat.CurveParameter) {
				if (isBlurApplied()) {
					applyBlurCurveInfo(blurCurve)
				}
			}

			override fun applyBlurInfo(context: Context): Boolean {
				if (Build.VERSION.SDK_INT >= 35) {
					return applyBlurCurveInfo(blurElevationPolicy.curveParameterForElevation(elevation, context))
				}
				return false
			}

			/** Applies curve info for background blur on Android 15+. */
			fun applyBlurCurveInfo(curveParameter: SemBlurCompat.CurveParameter): Boolean {
				if (Build.VERSION.SDK_INT < 35) return false
				val semBlurInfo = generateBlurInfo(context)
					.colorCurvePreset(ColorCurvePreset(curveParameter))
					.build()
				clearBlurInfo(context)
				val applied = semBlurInfo.applyBlurInfo(this)
				blurInfo = if (applied) semBlurInfo else null
				return applied
			}

			override fun clearBlurInfo(context: Context) {
				blurInfo?.clearBlurInfo(this)
				blurInfo = null
			}

			override fun setBlurMode(semBlurInfoMode: Int) {
				blurMode = semBlurInfoMode
				applyBlurInfo(context)
			}

			override fun isBlurApplied(): Boolean = blurInfo != null

			private fun generateBlurInfo(context: Context): SemBlurInfoStateBuilder {
				val radius = context.resources.getDimension(R.dimen.sesl_projection_bg_radius)
				val builder = BlurInfoState.generateFloatingComponentBlurInfoStateBuilder(context, blurMode)
				mBackgroundDrawable?.let { builder.nonBlurBackground(it) }
				builder.cornerRadius(radius)
				return builder
			}
		}
	}
}
