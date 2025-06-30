package com.google.android.material.oneui.floatingdock.behavior

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.util.Log
import android.util.Range
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.util.SeslMisc
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPaneViewModel
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import com.google.android.material.oneui.floatingdock.util.getFloat
import com.google.android.material.oneui.floatingdock.util.screenHeight


/**
 * Behavior class for the Bottom mode of the FloatingPane.
 * This class handles the specific behaviors and interactions for the FloatingPane when it's in Bottom mode.
 *
 * @param context The context used to access resources and system services.
 * @param mode The current mode of the FloatingPane.
 * @param callBackNotifier The notifier for FloatingPane callbacks.
 * @param viewModel The ViewModel associated with the FloatingPane.
 * @param resizeTouchSize The size of the touch area for resizing.
 */
class BottomBehavior(
    val context: Context,
    mode: FloatingPaneMode,
    callBackNotifier: FloatingPaneCallbackNotifier,
    val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    companion object {
        const val TAG = "BottomBehavior"
    }

    private val resources: Resources = context.resources
    private val maxHeightTopPadding: Int =
        resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_max_height_top_padding)
    private val mostMinHeight: Int =
        resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_most_min_height)

    private var _customMaxWidth: Int? = null
    private var _customMinWidth: Int? = null
    private var _customMinimizeWidth: Int? = null
    private var _customWidth: Int? = null

    private var _requestedWidth: Int = -1

    var maxVIThreshold: Range<Int> = Range(-1, -1)
    var minVIThreshold: Range<Int> = Range(-1, -1)
    var closeVIThreshold: Range<Int> = Range(-1, Int.MAX_VALUE)

    init {
        setMinWidth(0)
        minimizeHeight = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_minimize_default_height)
        updateDefaultSize()
        _requestedWidth = -1
    }

    private fun updateDefaultSize() {
        requestedWidth = (context.screenHeight *
                context.getFloat(R.dimen.sesl_floating_pane_bottom_mode_default_height, 0.45f)).toInt()
    }

    @DrawableRes
    override fun getBackgroundResId(): Int {
        return customBackground ?: if (SeslMisc.isLightTheme(context)) {
            R.drawable.sesl_floating_pane_background_bottom
        } else {
            R.drawable.sesl_floating_pane_background_bottom_dark
        }
    }

    @LayoutRes
    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) {
            R.layout.sesl_floating_pane_menu_bottom
        } else {
            R.layout.sesl_floating_pane_menu_bottom_dark
        }
    }

    override fun getShowSpringAnimation(context: Context, target: View): SpringAnimation {
        val springAnimation = SpringAnimation(target, DynamicAnimation.TRANSLATION_Y)
        val springForce = SpringForce().apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
            finalPosition = 0.0f
        }
        springAnimation.spring = springForce
        springAnimation.setStartValue(requestedHeight.toFloat())
        return springAnimation
    }

    override fun getHideSpringAnimation(context: Context, target: View): SpringAnimation {
        val springAnimation = SpringAnimation(target, DynamicAnimation.TRANSLATION_Y)
        val springForce = SpringForce().apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
            finalPosition = target.height.toFloat()
        }
        springAnimation.spring = springForce
        springAnimation.setStartValue(0.0f)
        return springAnimation
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect()
        rect.right = view.width
        val height = view.height
        rect.bottom = height
        rect.top = height - if (isMinimized) minimizeHeight else requestedHeight
        return rect
    }

    override fun getMinimizeRect(minimize: Boolean, from: Rect): Rect {
        val rect = Rect(from)
        if (minimize) {
            rect.top = closeVIThreshold.lower
        } else {
            rect.top = rect.bottom - requestedHeight
        }
        return rect
    }

    override fun isSupportMinimize(): Boolean = true

    override fun isMinimizableRect(newRect: Rect): Boolean {
        return minVIThreshold.contains(newRect.top)
    }

    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        return event.y < resizeTouchSize
    }

    override fun updateBehavior(parent: View) {
        val height = parent.height
        updateDefaultSize()
        setMaxHeight(height - maxHeightTopPadding)
        val minimizeHeightTop = height - minimizeHeight
        closeVIThreshold = Range(minimizeHeightTop, Int.MAX_VALUE)
        var minValue = closeVIThreshold.lower - minimizeHeight
        val lower = closeVIThreshold.lower
        if (minValue > lower) minValue = lower
        minVIThreshold = Range(minValue, closeVIThreshold.lower)
        val maxValue = minimizeHeight + (height - getMaxHeight())
        maxVIThreshold = Range(0, if (maxValue >= 0) maxValue else 0)
    }

    override fun updateLayoutParams(view: View) {
        val layoutParams = view.layoutParams
        if (layoutParams is FrameLayout.LayoutParams) {
            layoutParams.width = requestedWidth
            layoutParams.gravity = 80 // Gravity.BOTTOM
            view.layoutParams = layoutParams
        } else {
            layoutParams.width = requestedWidth
            view.layoutParams = layoutParams
        }
    }

    override fun updateMinimize(view: View): Boolean {
        if (isMinimized) return false
        setMinimize(requestedHeight < mostMinHeight, view)
        Log.d(TAG, "updateMinimize requested Height($requestedHeight) is less than most min height($mostMinHeight). set minimized")
        return true
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewModel.state = if (event.y < resizeTouchSize) {
                    FloatingPane.FloatingPaneState.STATE_RESIZE
                } else {
                    FloatingPane.FloatingPaneState.STATE_IDLE
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = FloatingPane.FloatingPaneState.STATE_IDLE
            }
        }
    }

    override var customMaxWidth: Int?
        get() = _customMaxWidth
        set(_) {
            Log.d(TAG, "custom max width can't change in this Mode")
            _customMaxWidth = null
        }

    override var customMinWidth: Int?
        get() = _customMinWidth
        set(_) {
            Log.d(TAG, "custom min width can't change in this Mode")
            _customMinWidth = null
        }

    override var customMinimizeWidth: Int?
        get() = _customMinimizeWidth
        set(_) {
            Log.d(TAG, "custom MinimizeWidth can't change in this Mode")
            _customMinimizeWidth = null
        }

    override var customWidth: Int?
        get() = _customWidth
        set(_) {
            Log.d(TAG, "custom width can't change in this Mode")
            _customWidth = null
        }

    override var requestedWidth: Int
        get() = _requestedWidth
        set(_) {
            Log.d(TAG, "width can't change in this Mode")
            _requestedWidth = -1
        }

    override fun getResizePinDirectionFlags(): Int = 13
}