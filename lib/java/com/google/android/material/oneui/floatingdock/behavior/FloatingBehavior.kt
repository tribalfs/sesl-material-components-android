package com.google.android.material.oneui.floatingdock.behavior

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.oneui.common.internal.util.dp
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.util.getCurrentLayoutBounds
import com.google.android.material.oneui.common.internal.util.getFloat
import com.google.android.material.oneui.common.internal.util.getScreenHeight
import com.google.android.material.oneui.common.internal.util.getScreenWidth
import com.google.android.material.oneui.common.internal.util.moveInsideAndIntersect
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.FloatingPaneViewModel
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier

class FloatingBehavior(
    val context: Context,
    mode: Int,
    callBackNotifier: FloatingPaneCallbackNotifier,
    private val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    override val logTag: String = "FloatingBehavior"
    private val untouchableAreaPadding = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_untouchable_area_padding)
    var customUntouchableAreaInset: Rect? = null
    var lastPosX: Int = 0
    var lastPosY: Int = 0

    init {
        minWidth = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_min_width)
        minHeight = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_min_height)
        minimizeWidth = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_minimize_default_width)
        minimizeMinWidth = minimizeWidth
        minimizeHeight = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_minimize_default_height)
        minimizeMinHeight = minimizeHeight
        minimizeMaxHeight = minimizeHeight
        updateDefaultSize()
        lastPosX = (context.getScreenWidth() - getRequestedWidthValue()) / 2
        lastPosY = (context.getScreenHeight() - getRequestedHeightValue()) / 2
    }

    private fun isNotMeasured(view: View): Boolean {
        return view.width <= 0 || view.height <= 0
    }

    private fun updateDefaultSize() {
        val typedValue = TypedValue()
        val screenWidth = context.getScreenWidth()
        val screenHeight = context.getScreenHeight()
        val f6 = context.getFloat(R.dimen.sesl_floating_pane_floating_mode_default_height, 0.48f)
        val reqW = if (context.getFloat(R.dimen.sesl_floating_pane_floating_mode_default_width, typedValue)) {
            (typedValue.float * screenWidth).toInt()
        } else {
            context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_floating_mode_default_width)
        }
        requestedWidth = reqW
        requestedHeight = (screenHeight * f6).toInt()
    }

    override fun getBackgroundResId(): Int {
        customBackground?.let { return it }
        return if (SeslMisc.isLightTheme(context)) R.drawable.sesl_floating_pane_background_floating else R.drawable.sesl_floating_pane_background_floating_dark
    }

    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) R.layout.sesl_floating_pane_menu_floating else R.layout.sesl_floating_pane_menu_floating_dark
    }

    override fun getResizePinDirectionFlags(): Int = 0
    override fun isSupportMinimize(): Boolean = true

    override fun isMinimizableRect(newRect: Rect): Boolean {
        return newRect.width() <= getMinWidthValue() && newRect.height() <= getMinHeightValue()
    }

    override fun getMinimizeRect(minimize: Boolean, from: Rect): Rect {
        val rect = Rect(from)
        if (minimize) {
            rect.right = getMinimizeWidthValue() + from.left
            rect.bottom = getMinimizeHeightValue() + from.top
            requestedWidth = from.width()
            requestedHeight = from.height()
            return rect
        }
        rect.right = getMinWidthValue() + from.left
        rect.bottom = getMinHeightValue() + from.top
        requestedWidth = getMinWidthValue()
        requestedHeight = getMinHeightValue()
        return rect
    }

    fun getMoveableArea(parent: View): Rect {
        val rect = Rect()
        parent.getCurrentLayoutBounds(rect)
        rect.inset(untouchableAreaPadding, untouchableAreaPadding)
        customUntouchableAreaInset?.let { inset ->
            rect.left += inset.left
            rect.top += inset.top
            rect.right -= inset.right
            rect.bottom -= inset.bottom
        }
        return rect
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect(0, 0, view.width, view.height)
        rect.left = lastPosX
        rect.top = lastPosY
        rect.right = lastPosX + (if (isMinimized) getMinimizeWidthValue() else getRequestedWidthValue())
        rect.bottom = rect.top + (if (isMinimized) getMinimizeHeightValue() else getRequestedHeightValue())
        if (moveValidArea) {
            rect.moveInsideAndIntersect(getMoveableArea(view))
        }
        return rect
    }

    override fun initBehavior(parent: View) {
        super.initBehavior(parent)
        lastPosX = (parent.width - getRequestedWidthValue()) / 2
        lastPosY = (parent.height - getRequestedHeightValue()) / 2
    }

    override fun isSupported(context: Context): Boolean {
        return context.getScreenWidth() >= 600.dp && context.getScreenHeight() >= 600.dp
    }

    override var showAnimationListener: IFloatingPaneCallback.AnimationListener?
        get() = super.showAnimationListener
        set(value) {
            debug("showAnimationListener can't set in this Mode yet")
            super.showAnimationListener = null
        }

    override var hideAnimationListener: IFloatingPaneCallback.AnimationListener?
        get() = super.hideAnimationListener
        set(value) {
            debug("hideAnimationListener can't set in this Mode yet")
            super.hideAnimationListener = null
        }

    override fun saveState(parent: View) {
        if (!isNotMeasured(parent)) {
            val minW = if (isMinimized) getMinimizeWidthValue() else getRequestedWidthValue()
            val minH = if (isMinimized) getMinimizeHeightValue() else getRequestedHeightValue()
            posXRatio = lastPosX.toFloat() / (parent.width - minW)
            posYRatio = lastPosY.toFloat() / (parent.height - minH)
        } else {
            posXRatio = -1.0f
            posYRatio = -1.0f
        }
    }

    override fun loadState(parent: View) {
        if (posXRatio != -1.0f && posYRatio != -1.0f && !isNotMeasured(parent)) {
            val minW = if (isMinimized) getMinimizeWidthValue() else getRequestedWidthValue()
            val minH = if (isMinimized) getMinimizeHeightValue() else getRequestedHeightValue()
            lastPosX = (posXRatio * (parent.width - minW)).toInt()
            lastPosY = (posYRatio * (parent.height - minH)).toInt()
        } else {
            posXRatio = -1.0f
            posYRatio = -1.0f
        }
    }

    override fun setMinimize(minimize: Boolean, view: View) {
        super.setMinimize(minimize, view)
        val drawable = view.background as? GradientDrawable
        drawable?.cornerRadius = if (isMinimized) {
            context.resources.getDimension(R.dimen.sesl_floating_pane_minimized_background_corner_radius)
        } else {
            context.resources.getDimension(R.dimen.sesl_floating_pane_background_corner_radius)
        }
    }

    override fun updateBehavior(parent: View) {
        val typedValue = TypedValue()
        val width = parent.width
        val height = parent.height
        updateDefaultSize()
        val dimension = if (context.getFloat(R.dimen.sesl_floating_pane_floating_mode_max_width, typedValue)) {
            typedValue.float * width
        } else {
            parent.resources.getDimension(R.dimen.sesl_floating_pane_floating_mode_max_width)
        }
        maxWidth = dimension.toInt()
        maxHeight = height
    }

    override fun updateLayoutParams(view: View) {
        val layoutParams = view.layoutParams
        (layoutParams as? FrameLayout.LayoutParams)?.gravity = 51
        view.layoutParams = layoutParams
    }

    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        if (isMinimized) return false
        return event.y < resizeTouchSize || event.y > (view.height - resizeTouchSize) || event.x < resizeTouchSize || event.x > (view.width - resizeTouchSize)
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isMinimized) {
                    viewModel.state = FloatingPane.FloatingPaneState.STATE_MOVE.state
                } else {
                    val state = if (event.y < resizeTouchSize) {
                        FloatingPane.FloatingPaneState.STATE_MOVE.state
                    } else if (event.y > view.height - resizeTouchSize || event.x > view.width - resizeTouchSize || event.x < resizeTouchSize) {
                        FloatingPane.FloatingPaneState.STATE_RESIZE.state
                    } else null
                    state?.let { viewModel.state = it }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = FloatingPane.FloatingPaneState.STATE_IDLE.state
            }
        }
    }

    override fun getShowAnimation(context: Context, target: View): AnimatorSet {
        val animSet = AnimatorSet()
        val anim = AnimatorInflater.loadAnimator(context, R.animator.floating_show_anim)
        anim.setTarget(target)
        animSet.playTogether(anim)
        return animSet
    }

    override fun getHideAnimation(context: Context, target: View): AnimatorSet {
        val animSet = AnimatorSet()
        val anim = AnimatorInflater.loadAnimator(context, R.animator.floating_hide_anim)
        anim.setTarget(target)
        animSet.playTogether(anim)
        return animSet
    }
}
