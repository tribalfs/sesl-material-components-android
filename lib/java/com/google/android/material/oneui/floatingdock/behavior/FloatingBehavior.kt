package com.google.android.material.oneui.floatingdock.behavior

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState.Companion.STATE_IDLE
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState.Companion.STATE_MOVE
import com.google.android.material.oneui.floatingdock.FloatingPaneViewModel
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import com.google.android.material.oneui.floatingdock.util.dp
import com.google.android.material.oneui.floatingdock.util.getCurrentLayoutBounds
import com.google.android.material.oneui.floatingdock.util.getFloat
import com.google.android.material.oneui.floatingdock.util.moveInsideAndIntersect
import com.google.android.material.oneui.floatingdock.util.screenHeight
import com.google.android.material.oneui.floatingdock.util.screenWidth

/**
 * Behavior class for the FloatingPane in floating mode.
 *
 * This class handles the behavior of the FloatingPane when it's in floating mode, including:
 * - Sizing and positioning
 * - Animations for showing and hiding
 * - Touch handling for moving and resizing
 * - Minimizing and restoring
 * - Saving and restoring state
 *
 * @param context The context.
 * @param mode The FloatingPaneMode.
 * @param callBackNotifier The FloatingPaneCallbackNotifier.
 * @param viewModel The FloatingPaneViewModel.
 * @param resizeTouchSize The size of the touch area for resizing.
 */
class FloatingBehavior(
    val context: Context,
    mode: FloatingPaneMode,
    callBackNotifier: FloatingPaneCallbackNotifier,
    val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    companion object {
        const val TAG = "FloatingBehavior"
    }

    var customUntouchableAreaInset: Rect? = null
    var lastPosY: Int = 0
    var lastPosX: Int = 0

    private var _showAnimationListener: IFloatingPaneCallback.AnimationListener? = null
    private var _hideAnimationListener: IFloatingPaneCallback.AnimationListener? = null

    private val untouchableAreaPadding: Int =
        context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_untouchable_area_padding)

    init {
        setMinWidth(context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_min_width))
        setMinHeight(context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_min_height))
        minimizeWidth = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_minimize_default_width)
        minimizeMinWidth = minimizeWidth
        minimizeHeight = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_minimize_default_height)
        minimizeMinHeight = minimizeHeight
        minimizeMaxHeight = minimizeHeight
        updateDefaultSize()
        lastPosX = (context.screenWidth - requestedWidth) / 2
        lastPosY = (context.screenHeight - requestedHeight) / 2
    }

    private fun isNotMeasured(view: View) = view.width <= 0 || view.height <= 0

    private fun updateDefaultSize() {
        val typedValue = TypedValue()
        val screenWidth = context.screenWidth
        val screenHeight = context.screenHeight
        val defaultHeightRatio = context.getFloat(R.dimen.sesl_floating_pane_floating_mode_default_height, 0.48f)
        val widthDimen = R.dimen.sesl_floating_pane_floating_mode_default_width
        val width = if (context.getFloat(widthDimen, typedValue)) {
            (typedValue.float * screenWidth).toInt()
        } else {
            context.resources.getDimensionPixelSize(widthDimen)
        }
        requestedWidth = width
        requestedHeight = (screenHeight * defaultHeightRatio).toInt()
    }

    @DrawableRes
    override fun getBackgroundResId(): Int {
        return customBackground ?: if (SeslMisc.isLightTheme(context)) {
            R.drawable.sesl_floating_pane_background_floating
        } else {
            R.drawable.sesl_floating_pane_background_floating_dark
        }
    }

    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) {
            R.layout.sesl_floating_pane_menu_floating
        } else {
            R.layout.sesl_floating_pane_menu_floating_dark
        }
    }

    override fun getShowAnimation(context: Context, target: View) =
        AnimatorSet().apply { playTogether(AnimatorInflater.loadAnimator(context, R.animator.floating_show_anim)
            .also { it.setTarget(target) }) }

    override fun getHideAnimation(context: Context, target: View) =
         AnimatorSet().apply { playTogether(AnimatorInflater.loadAnimator(context, R.animator.floating_hide_anim)
             .also { it.setTarget(target) }) }

    override var showAnimationListener: IFloatingPaneCallback.AnimationListener?
        get() = _showAnimationListener
        set(value) {
            Log.d(TAG, "showAnimationListener can't set in this Mode yet")
            _showAnimationListener = null
        }

    override var hideAnimationListener: IFloatingPaneCallback.AnimationListener?
        get() = _hideAnimationListener
        set(value) {
            Log.d(TAG, "hideAnimationListener can't set in this Mode yet")
            _hideAnimationListener = null
        }

    fun getMoveableArea(parent: View): Rect {
        val rect = Rect()
        parent.getCurrentLayoutBounds(rect)
        rect.inset(untouchableAreaPadding, untouchableAreaPadding)
        customUntouchableAreaInset?.let {
            rect.left += it.left
            rect.top += it.top
            rect.right -= it.right
            rect.bottom -= it.bottom
        }
        return rect
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect()
        rect.right = view.width
        rect.bottom = view.height
        rect.left = lastPosX
        rect.top = lastPosY
        rect.right = lastPosX + if (isMinimized) minimizeWidth else requestedWidth
        rect.bottom = lastPosY + if (isMinimized) minimizeHeight else requestedHeight
        if (moveValidArea) {
            rect.moveInsideAndIntersect(getMoveableArea(view))
        }
        return rect
    }

    override fun getMinimizeRect(minimize: Boolean, from: Rect): Rect {
        val rect = Rect(from)
        if (minimize) {
            rect.right = minimizeWidth + from.left
            rect.bottom = minimizeHeight + from.top
            requestedWidth = from.width()
            requestedHeight = from.height()
        } else {
            rect.right = getMinWidth() + from.left
            rect.bottom = getMinHeight() + from.top
            requestedWidth = getMinWidth()
            requestedHeight = getMinHeight()
        }
        return rect
    }

    override fun isSupportMinimize(): Boolean = true

    override fun isMinimizableRect(newRect: Rect) =
        newRect.width() <= getMinWidth() && newRect.height() <= getMinHeight()

    override fun isSupported(context: Context) =
        context.screenWidth.dp >= 600 && context.screenHeight.dp >= 600

    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        if (isMinimized) return false
        return event.y < resizeTouchSize ||
                event.y > (view.height - resizeTouchSize) ||
                event.x < resizeTouchSize ||
                event.x > (view.width - resizeTouchSize)
    }

    override fun updateBehavior(parent: View) {
        val typedValue = TypedValue()
        val width = parent.width
        val height = parent.height
        updateDefaultSize()
        val maxWidthDimen = R.dimen.sesl_floating_pane_floating_mode_max_width
        val maxWidth = if (parent.getFloat(maxWidthDimen, typedValue)) {
            (typedValue.float * width).toInt()
        } else {
            parent.resources.getDimension(maxWidthDimen).toInt()
        }
        setMaxWidth(maxWidth)
        setMaxHeight(height)
    }

    override fun updateLayoutParams(view: View) {
        val lp = view.layoutParams
        if (lp is FrameLayout.LayoutParams) {
            lp.gravity =  Gravity.TOP or Gravity.LEFT
        }
        view.layoutParams = lp
    }

    override fun setMinimize(minimize: Boolean, view: View) {
        super.setMinimize(minimize, view)
        val background = view.background
        val gradientDrawable = background as? GradientDrawable
        gradientDrawable?.cornerRadius = if (isMinimized) {
            context.resources.getDimension(R.dimen.sesl_floating_pane_minimized_background_corner_radius)
        } else {
            context.resources.getDimension(R.dimen.sesl_floating_pane_background_corner_radius)
        }
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isMinimized) {
                    viewModel.state = STATE_MOVE
                } else {
                    val state = when {
                        event.y < resizeTouchSize -> STATE_MOVE
                        event.y > view.height - resizeTouchSize ||
                                event.x > view.width - resizeTouchSize ||
                                event.x < resizeTouchSize -> FloatingPane.FloatingPaneState.STATE_RESIZE
                        else -> null
                    }
                    state?.let { viewModel.state = it }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = STATE_IDLE
            }
        }
    }

    override fun initBehavior(parent: View) {
        super.initBehavior(parent)
        lastPosX = (parent.width - requestedWidth) / 2
        lastPosY = (parent.height - requestedHeight) / 2
    }

    override fun saveState(parent: View) {
        if (!isNotMeasured(parent)) {
            val width = if (isMinimized) minimizeWidth else requestedWidth
            val height = if (isMinimized) minimizeHeight else requestedHeight
            posXRatio = lastPosX.toFloat() / (parent.width - width)
            posYRatio = lastPosY.toFloat() / (parent.height - height)
        } else {
            posXRatio = -1f
            posYRatio = -1f
            Log.e(TAG, "saveState fail: parent size is wrong (${parent.width},${parent.height})")
        }
    }

    override fun loadState(parent: View) {
        if (posXRatio != -1f && posYRatio != -1f && !isNotMeasured(parent)) {
            val width = if (isMinimized) minimizeWidth else requestedWidth
            val height = if (isMinimized) minimizeHeight else requestedHeight
            lastPosX = (posXRatio * (parent.width - width)).toInt()
            lastPosY = (posYRatio * (parent.height - height)).toInt()
        } else {
            posXRatio = -1f
            posYRatio = -1f
            Log.e(TAG, "loadState fail: posRatio=($posXRatio,$posYRatio) parent=(${parent.width},${parent.height})")
        }
    }

    override fun getResizePinDirectionFlags(): Int = 0
}