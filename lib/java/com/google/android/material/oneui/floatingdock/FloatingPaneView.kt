package com.google.android.material.oneui.floatingdock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationUpdateListener
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector
import com.google.android.material.R
import com.google.android.material.internal.RectEvaluator
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_ALL
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_BOTTOM
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_FLOATING
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_NONE
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_SIDE
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState.Companion.STATE_IDLE
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState.Companion.STATE_MOVE
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState.Companion.STATE_RESIZE
import com.google.android.material.oneui.floatingdock.animation.ScaleSpringAnimation
import com.google.android.material.oneui.floatingdock.behavior.BottomBehavior
import com.google.android.material.oneui.floatingdock.behavior.CommonBehavior
import com.google.android.material.oneui.floatingdock.behavior.FloatingBehavior
import com.google.android.material.oneui.floatingdock.behavior.SideBehavior
import com.google.android.material.oneui.floatingdock.controller.DragHandlerController
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import com.google.android.material.oneui.floatingdock.util.doOnGlobalLayout
import com.google.android.material.oneui.floatingdock.util.getCurrentLayoutBounds
import com.google.android.material.oneui.floatingdock.util.getFloat
import com.google.android.material.oneui.floatingdock.util.isPhoneSize
import com.google.android.material.oneui.floatingdock.util.moveInsideAndIntersect
import com.google.android.material.oneui.floatingdock.util.scaledTouchSlop
import com.google.android.material.oneui.floatingdock.util.updateViewBounds
import com.google.android.material.oneui.floatingdock.widget.FloatingMenuItemView
import org.jetbrains.annotations.NotNull
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Represents a floating pane view that can be displayed in different modes (bottom, side, floating)
 * and can be minimized. It handles touch events for moving, resizing, and changing modes.
 *
 * @param context The context in which the view is created.
 * @param parentView The parent layout that contains this floating pane view.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 * that supplies default values for the view. Can be 0 to not look for defaults.
 */
class FloatingPaneView @JvmOverloads constructor(
    context: Context,
    val parentView: FloatingPaneLayout,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SeslFloatingPaneView"

        private const val ANIM_DURATION = 400L
        private val RECT_EVALUATOR = RectEvaluator(Rect())
        private val INTERPOLATOR = PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f)

        private const val HANDLER_MENU_POPUP_DISMISS_DELAY_TIME = 2_000L
        private const val LONG_PRESS_ANIM_DURATION = 300L
        private const val PRESS_SCALE_ANIM_FINAL_VALUE = 0.98f
        private const val PRESS_SCALE_ANIM_START_VALUE = 1.0f
        private const val LONG_PRESS_AND_DRAG_SCALE_ANIM_FINAL_VALUE = 1.02f
        private const val LONG_PRESS_AND_DRAG_SCALE_ANIM_START_VALUE = 1.0f

        const val RESIZE_PIN_ALL = 15
        const val RESIZE_PIN_BOTTOM = 8
        const val RESIZE_PIN_LEFT = 1
        const val RESIZE_PIN_NONE = 0
        const val RESIZE_PIN_RIGHT = 4
        const val RESIZE_PIN_TOP = 2

        private val DEBUG: Boolean
        private val ENG: Boolean

        init {
            val type = Build.TYPE.lowercase(Locale.ROOT)
            ENG = type == "eng" || type == "userdebug"
            DEBUG = ENG
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val resizeTouchSize =
        resources.getDimensionPixelSize(R.dimen.sesl_resize_touch_area_size)
    private val floatLayoutElevation =
        resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_elevation).toFloat()
    private val modeChangeBottomThreshold = context.getFloat(
        R.dimen.sesl_floating_pane_mode_change_bottom_threshold,
        0.42f
    )
    private val modeChangeSideThreshold = context.getFloat(
        R.dimen.sesl_floating_pane_mode_change_side_threshold,
        0.32f
    )
    private val bottomToFloatingBottomMargin =
        resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_mode_change_bottom_to_floating_bottom_margin)
    private var resizePinPoint = RESIZE_PIN_ALL
    private val originBounds = Rect()
    private val endBounds = Rect()
    private var originRect = Rect()
    private val prevParentRect = Rect()
    private val callbackNotifier = FloatingPaneCallbackNotifier(ArrayList())
    internal var mode: FloatingPaneMode
    private var allowedMode: FloatingPaneMode
    private var behavior: CommonBehavior
    private val touchSlop = context.scaledTouchSlop
    private val rootView: View
    private val contentContainer: FrameLayout
    private val minimizeViewContainer: FrameLayout
    private val dragHandlerView: ImageView
    private var minimizeToolbarView: Toolbar? = null
    private val minimizeGestureDetector: GestureDetector
    private val pressScaleAnimation: ValueAnimator
    private val moveScaleAnimation: ScaleSpringAnimation
    private val dragHandlerController: DragHandlerController
    private val enterMinimizeAlphaAnimation: AnimatorSet
    private val exitMinimizeAlphaAnimation: AnimatorSet
    private var animator: ValueAnimator? = null
    private var prevConfiguration: Configuration? = null
    private var haveAnotherMinimizeView = false
    private var isDragging = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastTouchRawX = 0f
    private var lastTouchRawY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var topLimitSize = 0
    private var popupWindow: PopupWindow? = null
    private val hideRunnable: Runnable

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val viewModel = FloatingPaneViewModel(STATE_IDLE, callbackNotifier)

    private val behaviors: Map<FloatingPaneMode, CommonBehavior> = mapOf(
        MODE_BOTTOM to BottomBehavior(
            context,
            MODE_BOTTOM,
            callbackNotifier,
            viewModel,
            resizeTouchSize
        ),
        MODE_SIDE to SideBehavior(
            context,
            MODE_SIDE,
            callbackNotifier,
            viewModel,
            resizeTouchSize
        ),
        MODE_FLOATING to FloatingBehavior(
            context,
            MODE_FLOATING,
            callbackNotifier,
            viewModel,
            resizeTouchSize
        )
    )

    private val onMenuItemClickListener = OnClickListener { view ->
        val id = view.id
        val mode = when (id) {
            R.id.bottom_view -> MODE_BOTTOM
            R.id.side_view -> MODE_SIDE
            R.id.floating_view -> MODE_FLOATING
            else -> MODE_NONE
        }
        changePaneLayoutMode(mode, invalidate = false, isLongPress = false, skipAnimate = false)
        popupWindow?.dismiss()
    }

    init {
        mode = getDefaultLayoutMode(resources.configuration)
        allowedMode = MODE_ALL
        behavior = getBehavior(mode)

        rootView = layoutInflater.inflate(R.layout.sesl_floating_pane_container, null)
        contentContainer = rootView.findViewById(R.id.float_content)
        minimizeViewContainer = rootView.findViewById(R.id.float_minimize_content)
        dragHandlerView = rootView.findViewById(R.id.float_panel_drag_handler)
        addView(rootView)

        minimizeToolbarView = getToolbar(contentContainer)

        minimizeGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = behavior.isMinimized
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (behavior is FloatingBehavior && behavior.isMinimized) {
                        enterMinimizeView(false)
                    }
                    return super.onSingleTapConfirmed(e)
                }
            })

        pressScaleAnimation =
            ValueAnimator.ofFloat(PRESS_SCALE_ANIM_START_VALUE, PRESS_SCALE_ANIM_FINAL_VALUE)
                .apply {
                    interpolator = PathInterpolator(0.83f, 0.0f, 0.83f, 0.83f)
                    duration = ViewConfiguration.getLongPressTimeout().toLong()
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        this@FloatingPaneView.scaleX = value
                        this@FloatingPaneView.scaleY = value
                    }
                }

        moveScaleAnimation = ScaleSpringAnimation(this).apply {
            setSpringForce(1.0f, 361.0f)
        }

        dragHandlerController = DragHandlerController(
            view = dragHandlerView,
            onTouchListener = OnTouchListener { _, event ->
                if (behavior is FloatingBehavior && !behavior.isMinimized) {
                    pressScaleAnimation.start()
                }
                true
            },
            onClickListener = {
                initEffect()
                if (behavior is FloatingBehavior && behavior.isMinimized) {
                    enterMinimizeView(false)
                } else {
                    showPopup()
                }
                startReleaseAnimation()
            },
            onLongPress = {
                if (behavior is FloatingBehavior && !behavior.isMinimized) {
                    startLongPressOrDragStartAnimation()
                } else {
                    tryChangeFloatingModeByLongPress()
                }
            }
        )


        enterMinimizeAlphaAnimation = AnimatorSet().apply {
            playTogether(
                ValueAnimator.ofFloat(1.0f, 0.0f).apply {
                    addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            contentContainer.visibility = GONE
                        }
                    })
                    interpolator = PathInterpolator(0.0f, 0.0f, 1.0f, 1.0f)
                    setDuration(100L)
                },
                ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                    addUpdateListener {
                        val animatedValue = it.animatedValue as Float
                        alpha = animatedValue
                        /*scaleX = animatedValue
                        scaleY = animatedValue*/
                    }
                    interpolator = PathInterpolator(0.0f, 0.0f, 1.0f, 1.0f)
                    setStartDelay(100L)
                    setDuration(200L)
                }
            )
        }

        exitMinimizeAlphaAnimation = AnimatorSet().apply {
            playTogether(
                ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                    addUpdateListener {
                        val animatedValue = it.animatedValue as Float
                        alpha = animatedValue
                        /*scaleX = animatedValue
                        scaleY = animatedValue*/
                    }
                    interpolator = PathInterpolator(0.0f, 0.0f, 1.0f, 1.0f)
                    setStartDelay(100L)
                    setDuration(200L)
                },
                ValueAnimator.ofFloat(1.0f, 0.0f).apply {
                    addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            minimizeViewContainer.visibility = GONE
                        }
                    })
                    interpolator = PathInterpolator(0.0f, 0.0f, 1.0f, 1.0f)
                    setDuration(100L)
                }
            )
        }

        setClipToOutline(true)
        visibility = GONE

        parentView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                behaviors.values.forEach { it.updateBehavior(parentView) }
                updateView(behavior)
                parentView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        hideRunnable = Runnable { popupWindow?.dismiss() }
    }

    private fun initEffect() {
        parentView.seslStopDrawAllRequested()
    }

    private fun startLongPressOrDragStartAnimation() {
        if (pressScaleAnimation.isRunning) {
            pressScaleAnimation.cancel()
        }
        moveScaleAnimation.animateToFinalPosition(
            LONG_PRESS_AND_DRAG_SCALE_ANIM_FINAL_VALUE,
            LONG_PRESS_AND_DRAG_SCALE_ANIM_FINAL_VALUE
        )
    }

    private fun tryChangeFloatingModeByLongPress() {
        if (!isAllowedMode(MODE_FLOATING)) {
            Log.e(TAG, "Can't change to floatingMode. It is not allowed")
            return
        }

        if (mode == MODE_FLOATING) return

        val commonBehavior = behaviors[MODE_FLOATING]
        if (commonBehavior != null) {
            if (!commonBehavior.isSupported(context)) {
                return
            }
        }

        viewModel.state = STATE_MOVE
        updateFloatingPosition()
        changePaneLayoutMode(
            requestMode = MODE_FLOATING,
            invalidate = false,
            isLongPress = true,
            skipAnimate = false
        )
    }

    private fun updateFloatingPosition() {
        val floatingBehavior = behaviors[MODE_FLOATING] as? FloatingBehavior ?: return
        floatingBehavior.lastPosY = top
        if (mode == MODE_BOTTOM || mode == MODE_SIDE) {
            val requestedWidth = floatingBehavior.requestedWidth / 2
            floatingBehavior.lastPosX = ((left + lastTouchX) - requestedWidth).toInt()
        }
    }


    private fun getCurrentRect(): Rect {
        return Rect(left, top, width + left, height + top)
    }

    private fun showPopup() {
        if (context.isPhoneSize()) return

        val inflater = LayoutInflater.from(context)
        val contentView = inflater.inflate(getMenuLayoutResId(), null)
        contentView.measure(MeasureSpec.makeMeasureSpec(0, 0), MeasureSpec.makeMeasureSpec(0, 0))

        val viewGroup = contentView.findViewById<ViewGroup>(R.id.floating_pane_menu_container)
        val res = viewGroup.resources
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is FloatingMenuItemView) {
                val (mode, label) = when (child.id) {
                    R.id.bottom_view -> MODE_BOTTOM to res.getString(R.string.sesl_floating_pane_menu_item_bottom_view)
                    R.id.side_view -> MODE_SIDE to res.getString(R.string.sesl_floating_pane_menu_item_side_view)
                    R.id.floating_view -> MODE_FLOATING to res.getString(R.string.sesl_floating_pane_menu_item_floating_view)
                    else -> MODE_NONE to ""
                }
                child.isEnabled = isAllowedMode(mode)
                child.setOnClickListener(onMenuItemClickListener)
                TooltipCompat.setTooltipText(child, label)
                SeslViewReflector.semSetHoverPopupType(
                    child,
                    SeslHoverPopupWindowReflector.getField_TYPE_NONE()
                )
            }
        }

        val popupWindow =
            PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                isOutsideTouchable = true
                elevation =
                    context.resources.getDimension(R.dimen.sesl_floating_pane_menu_container_elevation)
                animationStyle = R.style.sesl_floating_popup_menu_window_animation
                setOnDismissListener {
                    popupWindow = null
                    handler.removeCallbacks(hideRunnable)
                }
            }

        val location = IntArray(2)
        dragHandlerView.getLocationInWindow(location)

        val x = location[0] - ((contentView.measuredWidth - dragHandlerView.width) / 2)
        val y = dragHandlerView.paddingTop + location[1]
        popupWindow.showAtLocation(dragHandlerView, Gravity.START or Gravity.TOP, x, y)

        this.popupWindow = popupWindow
        handler.postDelayed(hideRunnable, HANDLER_MENU_POPUP_DISMISS_DELAY_TIME)
    }

    @LayoutRes
    private fun getMenuLayoutResId(): Int {
        return this.behavior.getMenuLayoutResId()
    }

    fun show(animate: Boolean) {
        if (isShowing()) {
            return
        }
        visibility = VISIBLE
        initViewState()
        behavior.initBehavior(parentView)
        updateMinimize()
        updateViewBounds(getTargetModeBounds(behavior, true))
        val showSpringAnimation = behavior.getShowSpringAnimation(context, this)

        if (showSpringAnimation != null) {
            showSpringAnimation.addUpdateListener(
                object : OnAnimationUpdateListener {
                    var isStart = false
                    override fun onAnimationUpdate(
                        animation: DynamicAnimation<*>?,
                        value: Float,
                        velocity: Float
                    ) {
                        if (!isStart) {
                            onShowAnimationStart()
                            isStart = true
                        }
                        onShowAnimationUpdate()
                    }

                }
            )
            showSpringAnimation.addEndListener { _, _, _, _ ->
                onShowAnimationEnd()
            }
            showSpringAnimation.start()

            if (!animate) {
                showSpringAnimation.skipToEnd()
            }
        }

        if (showSpringAnimation == null) {
            val showAnimation = behavior.getShowAnimation(context, this)
            if (showAnimation != null) {
                showAnimation.addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onShowAnimationEnd()
                    }
                    override fun onAnimationStart(animation: Animator) {
                        onShowAnimationStart()
                    }
                })
                if (!animate) {
                    showAnimation.setDuration(0L)
                }
                showAnimation.start()
            }
        }
    }

    fun hide(animate: Boolean) {
        if (isShowing()) {
            this.popupWindow?.dismiss()
            this.behavior.getHideSpringAnimation(context, this)?.apply {
                addUpdateListener { _, _, _ ->
                    onHideAnimationUpdate()
                }
                addEndListener { _, _, _, _ ->
                    onHideAnimationEnd()
                }
                onHideAnimationStart()
                start()
                if (!animate) skipToEnd()
            } ?: run {
                behavior.getHideAnimation(context!!, this)?.apply {
                    addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            onHideAnimationEnd()
                        }
                        override fun onAnimationStart(animation: Animator) {
                            onHideAnimationStart()
                        }
                    })
                    if (!animate) setDuration(0L)
                    start()
                }
            }
        }
    }

    private fun getTargetModeBounds(behavior: CommonBehavior, moveValidArea: Boolean): Rect {
        return behavior.getTargetModeBounds(parentView, moveValidArea)
    }

    private fun initViewState() {
        setAlpha(1.0f)
        scaleX = 1.0f
        scaleY = 1.0f
        translationY = 0.0f
        translationX = 0.0f
    }

    fun onShowAnimationEnd() {
        val intersectRect: Rect = getIntersectRect()
        callbackNotifier.onInsert(intersectRect)
        val showAnimationListener = behavior.showAnimationListener
        showAnimationListener?.onAnimationEnd(intersectRect)
        behavior.updateLayoutParams(this)
    }

    fun onShowAnimationStart() {
        val intersectRect: Rect = getIntersectRect()
        callbackNotifier.onPreInsert(intersectRect)
        behavior.showAnimationListener?.onAnimationStart(intersectRect)
    }

    private fun onShowAnimationUpdate() {
        behavior.showAnimationListener?.onAnimationUpdate(getIntersectRect())
    }

    private fun getIntersectRect(): Rect {
        val childBounds = Rect()
        val parentBounds = Rect()
        getCurrentLayoutBounds(childBounds)
        parentView.getCurrentLayoutBounds(parentBounds)
        return if (childBounds.intersect(parentBounds)) childBounds else Rect()
    }

    private fun updateMinimize() {
        if (behavior.updateMinimize(this)) {
            setMinimizeStateAndAlphaAnimation(behavior.isMinimized)
        }
    }

    private fun setMinimizeStateAndAlphaAnimation(minimize: Boolean) {
        minimizeToolbarView?.seslSetEatingTouch(!minimize)
        behavior.setMinimize(minimize, this)
        if (haveAnotherMinimizeView) {
            if (minimize) {
                startMinimizeAlphaAnimation()
            } else {
                startUnMinimizeAlphaAnimation()
            }
        }
    }

    private fun getDefaultLayoutMode(configuration: Configuration): FloatingPaneMode {
        return if (configuration.orientation == ORIENTATION_LANDSCAPE) {
            MODE_SIDE
        } else {
            MODE_BOTTOM
        }
    }

    internal fun getBehavior(mode: FloatingPaneMode): CommonBehavior {
        return behaviors[mode] ?: BottomBehavior(
            context,
            MODE_BOTTOM,
            callbackNotifier,
            viewModel,
            resizeTouchSize
        )
    }

    private fun getToolbar(parent: ViewGroup): Toolbar? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is Toolbar) return child
            if (child is ViewGroup) {
                val toolbar = getToolbar(child)
                if (toolbar != null) return toolbar
            }
        }
        return null
    }

    fun isShowing(): Boolean = visibility == VISIBLE

    private fun isAllowedMode(mode: FloatingPaneMode): Boolean {
        if (allowedMode.contains(mode)) {
            if (getBehavior(mode).isSupported(context)) {
                return true
            }
        }
        return false
    }

    fun changePaneLayoutMode(
        requestMode: FloatingPaneMode,
        invalidate: Boolean,
        isLongPress: Boolean,
        skipAnimate: Boolean
    ) {

        Log.i(TAG, "Change Pane Mode to $requestMode")
        val initialMode = mode
        var requestMode = requestMode

        if (!isAllowedMode(requestMode)) {
            Log.e(
                TAG,
                "Change mode canceled, Because of the mode $requestMode is not allowed"
            )
            val configuration = resources.configuration
            requestMode = getDefaultLayoutMode(configuration)
        }

        if (invalidate || mode != requestMode) {
            initEffect()

            if (initialMode != requestMode) {
                behavior.initBehavior(parentView)

                if (behavior.isMinimized) {
                    Log.d(
                        TAG,
                        "Change Mode ClearMinimizeState invalidate=($invalidate $mode -> $requestMode)"
                    )
                    setMinimizeStateAndAlphaAnimation(false)
                }
            }

            mode = requestMode

            behavior = getBehavior(requestMode)

            if (isShowing() && initialMode != requestMode && !isLongPress) {
                val commonBehavior: CommonBehavior? = this.behavior
                val floatingBehavior = commonBehavior as? FloatingBehavior

                if (floatingBehavior != null) {
                    floatingBehavior.lastPosX = left + (right - left - floatingBehavior.requestedWidth) / 2
                    floatingBehavior.lastPosY = top
                    val bottom = bottom - floatingBehavior.requestedHeight - bottomToFloatingBottomMargin
                    if (initialMode == MODE_BOTTOM && top > bottom) {
                        floatingBehavior.lastPosY = bottom
                    }
                }
            }

            updateMinimize()
            updateView(behavior)

            if (isShowing()) {
                getCurrentLayoutBounds(Rect())
                if (initialMode != mode) {
                    callbackNotifier.onModeChanged(mode)
                }
            }

            val animationDurationMs =
                if (skipAnimate) 0L else if (isLongPress) LONG_PRESS_ANIM_DURATION else ANIM_DURATION
            val targetModeBounds = behavior.getTargetModeBounds(this, !isLongPress)
            behavior.updateLayoutParams(this)
            startBoundAnimation(targetModeBounds, animationDurationMs, false)
        }
    }

    private fun startBoundAnimation(to: Rect, duration: Long, moveValidArea: Boolean) {
        if (moveValidArea) {
            getCurrentLayoutBounds(originBounds)
        } else {
            originBounds.set(getIntersectRect())
        }
        val rect = Rect(originBounds)
        if (rect == to) return

        startAnimation(rect, to, duration)
    }

    private fun startAnimation(from: Rect, to: Rect, animatorDuration: Long) {
        animator?.end()
        animator = null
        endBounds.set(to)

        val animator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                val newBounds = RECT_EVALUATOR.evaluate(value, from, endBounds)
                updateViewBounds(newBounds)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null

                    if (isShowing()) {
                        callbackNotifier.onInsert(to)
                        behavior.updateLayoutParams(this@FloatingPaneView)
                    }
                }

                override fun onAnimationStart(animation: Animator) {
                    if (isShowing()) {
                        callbackNotifier.onPreInsert(from)
                    }
                }
            })
            interpolator = INTERPOLATOR
            setDuration(animatorDuration)
            start()
        }
        callbackNotifier.onResizeAnimate(from, to, animator.duration)
        this.animator = animator
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action

        if (action == ACTION_DOWN) {
            downRawX = event.rawX
            downRawY = event.rawY
            originRect = getCurrentRect()
        }

        if (dragHandlerController.onTouchEvent(event)) {
            Log.d(TAG, "return by dragHandlerController onTouchEvent=" + event.getAction())
            viewModel.state = STATE_IDLE
            return true
        }

        if (action == ACTION_DOWN) {
            lastTouchRawX = downRawX
            lastTouchRawY = downRawY
            lastTouchX = event.x
            lastTouchY = event.y

            if (shouldInterceptTouch(event)) {
                updateState(event)
                if (viewModel.state == STATE_RESIZE) {
                    getResizePinDirection(event)
                }
                Log.d(TAG, "onTouchEvent onInterceptTouchEvent " + event.getAction())
            }
        }

        if (behavior.isMinimized && minimizeGestureDetector.onTouchEvent(event)) {
            updateState(event)
            return true
        }

        if (viewModel.state == STATE_IDLE) {
            Log.d(TAG, "onTouchEvent is consumed in ResultView(STATE_IDLE)")
            return true
        }

        if (action == ACTION_MOVE && !isDragging) {
            isDragging =
                abs(event.rawX - lastTouchRawX) > touchSlop || abs((event.rawY - lastTouchRawY)) > touchSlop
            Log.d(TAG, "onTouchEvent Drag Start")
        }

        if (isDragging) {
            val currentRect = getCurrentRect()
            if (viewModel.state == STATE_MOVE) {
                onPreMove(event)
                onMove(event, currentRect)
            } else if (viewModel.state == STATE_RESIZE) {
                onResize(event, currentRect)
            }
        }

        updateState(event)

        if (action == ACTION_UP || action == ACTION_CANCEL) {
            isDragging = false
            startReleaseAnimation()
        }

        return true
    }

    private fun onResize(event: MotionEvent, nextResultViewRect: Rect) {
        var nextResultViewRect = nextResultViewRect

        if (mode != MODE_FLOATING) {
            nextResultViewRect = Rect(originRect)
        }
        val rect = Rect()
        val action = event.action

        if ((resizePinPoint and RESIZE_PIN_LEFT) == 0) {
            val rawX = (event.rawX - downRawX).toInt()
            val width = nextResultViewRect.width() - rawX
            if (mode == MODE_FLOATING || mode == MODE_SIDE) {
                if (width > behavior.getMaxWidth()) {
                    nextResultViewRect.left = nextResultViewRect.right - behavior.getMaxWidth()
                } else if (width <= behavior.getMinWidth()) {
                    rect.set(nextResultViewRect)
                    rect.left = nextResultViewRect.right - behavior.getMinWidth()
                    nextResultViewRect.left =
                        (nextResultViewRect.right - behavior.getMinWidth()) + (((rawX - (nextResultViewRect.width() - behavior.getMinWidth())) * 0.1f).toInt())
                } else {
                    nextResultViewRect.left += rawX
                }
            }
        }

        if ((resizePinPoint and RESIZE_PIN_TOP) == 0) {
            val rawY = (event.rawY - downRawY).toInt()
            val height = nextResultViewRect.height() - rawY
            if (mode == MODE_FLOATING || mode == MODE_BOTTOM) {
                if (height > behavior.getMaxHeight()) {
                    nextResultViewRect.top =
                        nextResultViewRect.bottom - behavior.getMaxHeight()
                } else if (height < behavior.getMinHeight()) {
                    rect.set(nextResultViewRect)
                    rect.top = nextResultViewRect.bottom - behavior.getMinHeight()
                    nextResultViewRect.top =
                        nextResultViewRect.bottom - behavior.getMinHeight()
                } else {
                    nextResultViewRect.top += rawY
                }
            }
        }

        if ((resizePinPoint and RESIZE_PIN_RIGHT) == 0) {
            val rawX2 = (event.rawX - downRawX).toInt()
            val width2 = nextResultViewRect.width() + rawX2
            if (mode == MODE_FLOATING || mode == MODE_SIDE) {
                if (width2 > behavior.getMaxWidth()) {
                    nextResultViewRect.right = behavior.getMaxWidth() + nextResultViewRect.left
                } else if (width2 <= behavior.getMinWidth()) {
                    rect.set(nextResultViewRect)
                    rect.right = nextResultViewRect.left - behavior.getMinWidth()
                    nextResultViewRect.right =
                        behavior.getMinWidth() + nextResultViewRect.left + (((rawX2 - (behavior.getMinWidth() - nextResultViewRect.width())) * 0.1f).toInt())
                } else {
                    nextResultViewRect.right += rawX2
                }
            }
        }

        if ((resizePinPoint and RESIZE_PIN_BOTTOM) == 0) {
            val rawY2 = (event.rawY - downRawY).toInt()
            val height2 = nextResultViewRect.height() + rawY2
            val i8 = nextResultViewRect.bottom + rawY2
            if (mode == MODE_FLOATING) {
                if (i8 > parentView.height) {
                    nextResultViewRect.bottom = parentView.height
                } else if (height2 <= behavior.getMinHeight()) {
                    rect.set(nextResultViewRect)
                    rect.bottom = nextResultViewRect.top - behavior.getMinHeight()
                    nextResultViewRect.bottom =
                        behavior.getMinHeight() + nextResultViewRect.top + (((rawY2 - (behavior.getMinHeight() - nextResultViewRect.height())) * 0.1f).toInt())
                } else {
                    nextResultViewRect.bottom += rawY2
                }
            }
        }

        if (mode == MODE_FLOATING) {
            (behavior as FloatingBehavior).apply {
                lastPosX = nextResultViewRect.left
                lastPosY = nextResultViewRect.top
            }

            parentView.showMinimizedIcon(behavior.isMinimizableRect(nextResultViewRect), false)

            if (!dragHandlerController.isInArea(dragHandlerView, event)) {
                parentView.seslRequestDrawResizeRect(nextResultViewRect)
            }
        } else {
            val bottomBehavior = behavior as? BottomBehavior
            if (bottomBehavior != null && bottomBehavior.isMinimized) {
                val upper: Int = bottomBehavior.minVIThreshold.getUpper()
                if (upper > nextResultViewRect.top) {
                    setMinimizeStateAndAlphaAnimation(false)
                }
            }
            updateViewBounds(nextResultViewRect)
        }

        if (action == ACTION_UP || action == ACTION_CANCEL) {
            parentView.seslRequestDrawResizeRect(null)

            if (behavior.isSupportMinimize()) {
                if (behavior is BottomBehavior) {
                    val behavior = behavior as BottomBehavior
                    if (behavior.closeVIThreshold.contains(nextResultViewRect.top)) {
                        hide(true)
                    } else if (behavior.minVIThreshold.contains(nextResultViewRect.top)) {
                        enterMinimizeView(true, nextResultViewRect)
                        return
                    } else if (behavior.maxVIThreshold.contains(nextResultViewRect.top)) {
                        nextResultViewRect.top = parentView.height - behavior.getMaxHeight()
                        nextResultViewRect.bottom = parentView.height
                        startBoundAnimation(nextResultViewRect, ANIM_DURATION, false)

                    }
                } else if (behavior.isMinimizableRect(nextResultViewRect)) {
                    parentView.showMinimizedIcon(false, true)
                    enterMinimizeView(true)
                    return
                }
            }

            if (mode == MODE_FLOATING) {
                startBoundAnimation(rect, ANIM_DURATION, false)
                return
            }

            if (isShowing()) {
                if ((behavior is SideBehavior) && rect.contains(nextResultViewRect)) {
                    startBoundAnimation(rect, ANIM_DURATION, false)

                } else {
                    callbackNotifier.onInsert(nextResultViewRect)
                    behavior.updateLayoutParams(this)
                }
            }
        }
    }

    private fun shouldInterceptTouch(event: MotionEvent) =
        dragHandlerController.isInArea(
            dragHandlerView,
            event
        ) || behavior.shouldInterceptTouch(this, event)

    private fun updateState(event: MotionEvent) = behavior.updateState(this, event)

    private fun getResizePinDirection(event: MotionEvent): Boolean {
        val xCoordinate = event.x.toInt()
        val yCoordinate = event.y.toInt()
        resizePinPoint = RESIZE_PIN_ALL
        if (xCoordinate < resizeTouchSize) {
            resizePinPoint = resizePinPoint xor RESIZE_PIN_LEFT
        }
        if (xCoordinate > width - resizeTouchSize) {
            this.resizePinPoint = resizePinPoint xor RESIZE_PIN_RIGHT
        }
        if (yCoordinate > height - resizeTouchSize) {
            this.resizePinPoint = resizePinPoint xor RESIZE_PIN_BOTTOM
        }
        if (yCoordinate < resizeTouchSize) {
            resizePinPoint = resizePinPoint xor RESIZE_PIN_TOP
        }
        val currentResizePinDirectionFlags = resizePinPoint or behavior.getResizePinDirectionFlags()
        resizePinPoint = currentResizePinDirectionFlags
        return currentResizePinDirectionFlags != RESIZE_PIN_ALL
    }

    private fun onPreMove(event: MotionEvent) {
        if ((behavior is FloatingBehavior) && !behavior.isMinimized && event.action == ACTION_MOVE) {
            startLongPressOrDragStartAnimation()
        }
    }

    private fun onMove(event: MotionEvent, nextResultViewRect: Rect) {
        val action = event.action
        val deltaX: Int = (event.rawX - lastTouchRawX).roundToInt()
        val deltaY: Int = (event.rawY - lastTouchRawY).roundToInt()
        lastTouchRawX = event.rawX
        lastTouchRawY = event.rawY
        lastTouchX = event.x
        lastTouchY = event.y

        (behavior as? FloatingBehavior)?.apply {
            lastPosX = left
            lastPosY = top
        }

        val newLayoutMode = checkLayoutModeChangeOnMove(event)

        if ((newLayoutMode != MODE_FLOATING) && (action == ACTION_UP || action == ACTION_CANCEL)) {
            changePaneLayoutMode(newLayoutMode, false, false, false)
            viewModel.state = STATE_IDLE
            parentView.seslStopDrawAllRequested()
            return
        }

        nextResultViewRect.top += deltaY
        nextResultViewRect.bottom += deltaY
        nextResultViewRect.left += deltaX
        nextResultViewRect.right += deltaX

        if (mode == MODE_FLOATING && (action == ACTION_UP || action == ACTION_CANCEL)) {
            updateViewBoundsInSideMoveableArea(nextResultViewRect)
        } else {
            updateViewBounds(nextResultViewRect)
        }
        callbackNotifier.onFloatingMoved(nextResultViewRect.left, nextResultViewRect.top)
    }

    private fun checkLayoutModeChangeOnMove(event: MotionEvent): FloatingPaneMode {
        val absoluteX = event.x + left
        val absoluteY = event.y + top
        val width = parentView.width
        val height = parentView.height
        val paneWidthThreshold = getWidth() * modeChangeSideThreshold
        val paneHeightThreshold = getHeight() * modeChangeBottomThreshold
        val isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL
        val requestMode =
            if (if (isRtl) absoluteX < paneWidthThreshold else absoluteX > width - paneWidthThreshold) {
                MODE_SIDE
            } else if (absoluteY > height - paneHeightThreshold) {
                MODE_BOTTOM
            } else {
                MODE_FLOATING
            }
        parentView.seslShowProDockingEffect(
            requestMode != MODE_FLOATING,
            getTargetModeBounds(getBehavior(MODE_FLOATING), true)
        )
        return requestMode
    }

    internal fun setContentView(view: View) {
        val layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        if (contentContainer.childCount != 0) {
            contentContainer.removeAllViews()
        }
        contentContainer.addView(view, layoutParams)
    }

    /**
     * Enters or exits the minimize view.
     *
     * @param minimize `true` to enter the minimize view, `false` to exit.
     */
    fun enterMinimizeView(minimize: Boolean) {
        if (!this.behavior.isSupportMinimize()) {
            Log.d(TAG, "that mode is not support Minimize")
        } else {
            enterMinimizeView(minimize, getCurrentRect())
        }
    }

    private fun enterMinimizeView(minimize: Boolean, current: Rect) {
        if (behavior.isSupportMinimize() && behavior.isMinimized != minimize) {
            minimizeToolbarView?.seslSetEatingTouch(!minimize)
            val minimizeRect = behavior.getMinimizeRect(minimize, current)
            behavior.setMinimize(minimize, this)
            if (minimize) {
                startMinimizeAnimation(current, minimizeRect)
            } else {
                startUnMinimizeAnimation(current, minimizeRect)
            }
        }
    }

    private fun startMinimizeAnimation(from: Rect, to: Rect) {
        endBounds.set(to)
        val springAnimation = SpringAnimation(FloatValueHolder()).apply {
            setSpring(SpringForce())
            spring.setDampingRatio(1.0f)
            spring.setStiffness(361.0f)
            spring.setFinalPosition(1_000.0f)
            setStartValue(0.0f)
            setMinValue(0.0f)
            setMaxValue(1_000.0f)
            addUpdateListener { _, value, _ ->
                val newBounds = RECT_EVALUATOR.evaluate(value * 0.001f, from, endBounds)
                updateViewBounds(newBounds)
            }
            addEndListener { _, _, value, _ ->
                val newBounds = RECT_EVALUATOR.evaluate(value * 0.001f, from, endBounds)
                updateViewBounds(newBounds)
            }
        }

        callbackNotifier.onPreInsert(from)
        callbackNotifier.onResizeAnimate(from, to, 0L)

        springAnimation.start()

        if (haveAnotherMinimizeView) {
            startMinimizeAlphaAnimation()
        }
    }

    private fun startMinimizeAlphaAnimation() {
        if (exitMinimizeAlphaAnimation.isRunning()) {
            exitMinimizeAlphaAnimation.cancel()
        }
        if (enterMinimizeAlphaAnimation.isRunning()) {
            enterMinimizeAlphaAnimation.cancel()
        }
        minimizeViewContainer.setAlpha(0.0f)
        minimizeViewContainer.visibility = VISIBLE
        enterMinimizeAlphaAnimation.start()
    }

    private fun startUnMinimizeAnimation(from: Rect, to: Rect) {
        endBounds.set(to)
        val springAnimation = SpringAnimation(FloatValueHolder()).apply {
            setSpring(SpringForce())
            spring.setDampingRatio(1.0f)
            spring.setStiffness(361.0f)
            spring.setFinalPosition(1_000.0f)
            setStartValue(0.0f)
            setMinValue(0.0f)
            setMaxValue(1_000.0f)
            addUpdateListener { _, value, _ ->
                val newBounds = RECT_EVALUATOR.evaluate(value * 0.001f, from, endBounds)
                updateViewBounds(newBounds)
            }
            addEndListener { _, _, value, _ ->
                callbackNotifier.onInsert(to)
            }
        }

        callbackNotifier.onPreInsert(from)
        callbackNotifier.onResizeAnimate(from, to, 0L)

        springAnimation.start()

        if (haveAnotherMinimizeView) {
            startUnMinimizeAlphaAnimation()
        }
    }

    private fun startUnMinimizeAlphaAnimation() {
        if (enterMinimizeAlphaAnimation.isRunning()) {
            enterMinimizeAlphaAnimation.cancel()
        }
        if (exitMinimizeAlphaAnimation.isRunning()) {
            exitMinimizeAlphaAnimation.cancel()
        }
        contentContainer.visibility = 0
        contentContainer.setAlpha(0.0f)
        exitMinimizeAlphaAnimation.start()
    }

    private fun startReleaseAnimation() {
        if (pressScaleAnimation.isRunning) {
            pressScaleAnimation.cancel()
        }
        moveScaleAnimation.animateToFinalPosition(
            LONG_PRESS_AND_DRAG_SCALE_ANIM_START_VALUE,
            LONG_PRESS_AND_DRAG_SCALE_ANIM_START_VALUE
        )
    }

    private fun updateView(behavior: CommonBehavior) {
        elevation = when (behavior) {
            is BottomBehavior -> 1.0f
            is SideBehavior -> 0.0f
            else -> floatLayoutElevation
        }
        setBackgroundResource(behavior.getBackgroundResId())
        dragHandlerView.visibility = if (context.isPhoneSize() && behavior is SideBehavior) GONE else VISIBLE
    }


    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        callbackNotifier.onVisibilityChanged(visibility)
    }

    override fun onConfigurationChanged(@NotNull newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val needUpdate =
            prevConfiguration?.let { it.screenWidthDp != newConfig.screenWidthDp || it.screenHeightDp != newConfig.screenHeightDp } != false

        if (needUpdate) {
            var requestMode = if (this.mode != MODE_FLOATING) {
                if (newConfig.orientation == ORIENTATION_LANDSCAPE) MODE_SIDE else MODE_BOTTOM
            } else this.mode

            val iterator = behaviors.values.iterator()
            while (iterator.hasNext()) {
                iterator.next().saveState(parentView)
            }

            parentView.doOnGlobalLayout {
                Log.d(
                    "SeslFloatingPaneView",
                    "onConfigurationChanged parentView(${parentView.width}, ${parentView.height} )" +
                            " (${parentView.left}, ${parentView.top}" +
                            ", ${parentView.right}, ${parentView.bottom} )"
                )

                for (behavior in behaviors.values) {
                    behavior.updateBehavior(parentView)
                    behavior.loadState(parentView)
                }
                changePaneLayoutMode(
                    requestMode = requestMode,
                    invalidate = true,
                    isLongPress = false,
                    skipAnimate = true
                )
            }
            prevConfiguration = newConfig
        }
    }

    private fun onHideAnimationEnd() {
        visibility = GONE
        val intersectRect = getIntersectRect()
        callbackNotifier.onInsert(intersectRect)
        if (behavior.isSupportMinimize() && behavior.isMinimized) {
            setMinimizeStateAndAlphaAnimation(false)
        }
        behavior.hideAnimationListener?.onAnimationEnd(intersectRect)
    }

    private fun onHideAnimationStart() {
        val intersectRect = getIntersectRect()
        behavior.hideAnimationListener?.onAnimationStart(intersectRect)
        callbackNotifier.onPreInsert(intersectRect)
    }

    private fun onHideAnimationUpdate() =
        behavior.hideAnimationListener?.onAnimationUpdate(getIntersectRect())


    /**
     * Sets the top limit size for the floating pane view.
     * This method is used when the view is in certain modes like 'Bottom' or 'Side' to restrict
     * how far up it can be moved or resized.
     *
     * @param newTopLimitSize The new top limit size in pixels.
     */
    fun setTopLimitSize(newTopLimitSize: Int) {
        this.topLimitSize = newTopLimitSize
    }

    fun getTopLimitSize(): Int {
        return this.topLimitSize
    }

    /**
     * Checks if the floating pane view is currently in a minimized state.
     *
     * @return `true` if the current behavior supports minimization and the view is minimized, `false` otherwise.
     */
    fun isMinimizeView() = behavior.isSupportMinimize() && behavior.isMinimized

    /**
     * Adds a callback to be notified of floating pane events.
     *
     * @param callback The callback to add.
     */
    fun addCallbacks(callback: IFloatingPaneCallback) {
        if (callbackNotifier.contains(callback as Any)) return
        callbackNotifier.add(callback)
    }

    /**
     * Removes all registered callbacks.
     */
    fun removeAllCallback() = callbackNotifier.clear()

    /**
     * Removes a callback that was previously added.
     *
     * @param callback The callback to remove.
     */
    fun removeCallback(callback: IFloatingPaneCallback) {
        callbackNotifier.remove(callback as Any)
    }

    /**
     * Sets whether another minimize view exists.
     * This is used to determine if alpha animations should be played when minimizing or un-minimizing.
     *
     * @param haveAnotherMinimizedView True if another minimize view exists, false otherwise.
     */
    fun setHaveAnotherMinimizeView(haveAnotherMinimizedView: Boolean) {
        haveAnotherMinimizeView = haveAnotherMinimizedView
    }

    /**
     * Sets the view to be displayed when the floating pane is minimized.
     *
     * If there is already a minimized view, it will be removed before adding the new one.
     * Sets a flag indicating that there is a custom minimized view.
     *
     * @param view The view to be displayed when minimized.
     */
    internal fun setMinimizeView(view: View) {
        if (minimizeViewContainer.childCount != 0) {
            minimizeViewContainer.removeAllViews()
        }
        haveAnotherMinimizeView = true
        minimizeViewContainer.addView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        minimizeToolbarView = getToolbar(minimizeViewContainer)
    }

    internal fun onChangedParentBounds(left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onChangedParentBounds $prevParentRect -> ($left,$top,$right,$bottom)")
        val currentRect = getCurrentRect()
        val requestedWidthMatches = behavior.requestedWidth == currentRect.width()
        val requestedHeightMatches = behavior.requestedHeight == currentRect.height()

        behaviors.values.forEach { it.updateBehavior(parentView) }

        val isAnimating = animator?.isRunning == true
        Log.d(TAG, "onChangedParentBound $mode, animating=$isAnimating")

        if (isAnimating) {
            animator?.end()
            animator = null
            changePaneLayoutMode(mode, invalidate = true, isLongPress = false, skipAnimate = false)
        } else {
            val previousParentRect = prevParentRect
            val widthChanged = previousParentRect.left != left || previousParentRect.right != right
            val heightChanged = previousParentRect.top != top || previousParentRect.bottom != bottom

            var update = false
            var logWidthChanged = widthChanged
            var logHeightChanged = heightChanged
            var logRequestedWidth = requestedWidthMatches
            var logRequestedHeight = requestedHeightMatches

            when (behavior) {
                is SideBehavior -> {
                    // No-op, update remains false
                }

                is BottomBehavior -> {
                    // No-op, update remains false
                }

                is FloatingBehavior -> {
                    if (widthChanged || heightChanged) {
                        if (!requestedWidthMatches || !requestedHeightMatches) {
                            val updatedCurrentRect = Rect(currentRect)
                            updateViewBoundsInSideMoveableArea(updatedCurrentRect)
                            (behavior as FloatingBehavior).apply {
                                lastPosX = updatedCurrentRect.left
                                lastPosY = updatedCurrentRect.top
                            }
                        }
                        update = true
                    }
                }

                else -> {
                    // Default case
                }
            }

            if (!update) {
                changePaneLayoutMode(
                    mode,
                    invalidate = true,
                    isLongPress = false,
                    skipAnimate = true
                )
            }

            Log.d(
                TAG,
                "onChangedParentBound $mode, update=$update, $currentRect -> ${getCurrentRect()}, " +
                        "w=$logWidthChanged, h=$logHeightChanged, dw=$logRequestedWidth, dh=$logRequestedHeight"
            )
        }
        prevParentRect.set(left, top, right, bottom)
    }

    private fun updateViewBoundsInSideMoveableArea(nextResultViewRect: Rect) {
        val floatingBehavior = behavior as? FloatingBehavior
        if (floatingBehavior != null) {
            if (nextResultViewRect.moveInsideAndIntersect(
                    floatingBehavior.getMoveableArea(
                        parentView
                    )
                )
            ) {
                startBoundAnimation(nextResultViewRect, ANIM_DURATION, true)

            } else {
                updateViewBounds(nextResultViewRect)
            }
        }
    }

    /**
     * Sets the height of the floating pane for a specific mode.
     *
     * @param mode The [FloatingPaneMode] for which to set the height.
     * @param height The new height in pixels, or `null` to reset to the default height for that mode.
     */
    fun setResultHeight(mode: FloatingPaneMode, height: Int?) {
        val behaviorForMode: CommonBehavior = getBehavior(mode)
        if (behaviorForMode.customHeight == height) {
            return
        }
        behaviorForMode.customHeight = height
        if (behaviorForMode == behavior) {
            startBoundAnimation(getTargetModeBounds(behavior, true), 400L, false)
        }
    }

    /**
     * Sets the background resource for the result view in a specific mode.
     *
     * @param mode The [FloatingPaneMode] for which to set the background.
     * @param backgroundResourceId The drawable resource ID for the background. Can be null to use the default background.
     */
    fun setResultViewBackgroundResource(
        mode: FloatingPaneMode,
        @DrawableRes backgroundResourceId: Int?
    ) {
        val behaviorForMode: CommonBehavior = getBehavior(mode)
        if (behaviorForMode.customBackground == backgroundResourceId) {
            return
        }

        behaviorForMode.customBackground = backgroundResourceId
        if (behaviorForMode == behavior) {
            setBackgroundResource(this.behavior.getBackgroundResId())
        }
    }

    /**
     * Sets the width for a specific mode of the floating pane.
     *
     * This method allows customization of the width for different modes (e.g., side, bottom, floating).
     * If the new width is different from the current custom width for that mode, it updates the
     * behavior and, if the current mode is the one being modified, it starts an animation to
     * adjust the bounds to the new width.
     *
     * @param mode The [FloatingPaneMode] for which to set the width.
     * @param width The new width in pixels, or null to reset to the default width for that mode.
     */
    fun setResultWidth(mode: FloatingPaneMode, width: Int?) {
        val behavior: CommonBehavior = getBehavior(mode)
        if (behavior.customWidth == width) {
            return
        }
        behavior.customWidth = width
        if (behavior == this@FloatingPaneView.behavior) {
            startBoundAnimation(getTargetModeBounds(this@FloatingPaneView.behavior, true), 400L, false)
        }
    }

}