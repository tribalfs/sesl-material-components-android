package com.google.android.material.oneui.floatingdock.behavior

import android.content.Context
import android.graphics.Rect
import android.util.Range
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.util.getFloat
import com.google.android.material.oneui.common.internal.util.getScreenHeight
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.FloatingPaneViewModel
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce


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
    mode: Int,
    callBackNotifier: FloatingPaneCallbackNotifier,
    private val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    override val logTag: String = "BottomBehavior"
    private val resources = context.resources
    private val maxHeightTopPadding = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_max_height_top_padding)
    private val maxHeightTopPaddingSmallScreen = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_max_height_top_padding)
    private val mostMinHeight = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_most_min_height)

    var maxVIThreshold: Range<Int> = Range(-1, -1)
    var minVIThreshold: Range<Int> = Range(-1, -1)
    var closeVIThreshold: Range<Int> = Range(-1, Int.MAX_VALUE)
    var minimizeBottomInset: Int = 0

    init {
        minWidth = 0
        minimizeHeight = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_bottom_mode_minimize_default_height)
        updateDefaultSize()
        requestedWidth = -1
    }

    private fun isSmallScreen(): Boolean {
        val config = context.resources.configuration
        return config.screenWidthDp <= 400 && config.screenHeightDp <= 442
    }

    private fun updateDefaultSize() {
        requestedHeight = (context.getScreenHeight() * context.getFloat(R.dimen.sesl_floating_pane_bottom_mode_default_height, 0.45f)).toInt()
    }

    fun getEffectiveMinimizeHeight(): Int {
        return getMinimizeWidthValue() + minimizeBottomInset
    }

    override var customMinWidth: Int?
        get() = super.customMinWidth
        set(value) {
            debug("Custom Min Width can't change in this Mode")
            super.customMinWidth = null
        }

    override var customMaxWidth: Int?
        get() = super.customMaxWidth
        set(value) {
            debug("Custom Max Width can't change in this Mode")
            super.customMaxWidth = null
        }

    override var customWidth: Int?
        get() = super.customWidth
        set(value) {
            debug("Custom Width can't change in this Mode")
            super.customWidth = null
        }

    override var customMinimizeWidth: Int?
        get() = super.customMinimizeWidth
        set(value) {
            debug("Custom Minimize Width can't change in this Mode")
            super.customMinimizeWidth = null
        }

    override fun getRequestedWidthValue(): Int {
        return requestedWidth
    }

    override fun getBackgroundResId(): Int {
        customBackground?.let { return it }
        return if (SeslMisc.isLightTheme(context)) R.drawable.sesl_floating_pane_background_bottom else R.drawable.sesl_floating_pane_background_bottom_dark
    }

    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) R.layout.sesl_floating_pane_menu_bottom else R.layout.sesl_floating_pane_menu_bottom_dark
    }

    override fun getResizePinDirectionFlags(): Int = 13

    override fun isSupportMinimize(): Boolean = true

    override fun isMinimizableRect(newRect: Rect): Boolean {
        return minVIThreshold.contains(newRect.top)
    }

    override fun getMinimizeRect(minimize: Boolean, from: Rect): Rect {
        val rect = Rect(from)
        if (!minimize) {
            rect.top = rect.bottom - getRequestedHeightValue()
            return rect
        }
        rect.top = closeVIThreshold.lower
        return rect
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect(0, 0, view.width, view.height)
        val h = view.height
        rect.bottom = h
        rect.top = h - (if (isMinimized) getEffectiveMinimizeHeight() else getRequestedHeightValue())
        return rect
    }

    override fun updateBehavior(parent: View) {
        val height = parent.height
        val small = isSmallScreen()
        maxHeight = height - (if (small) maxHeightTopPaddingSmallScreen else maxHeightTopPadding)
        updateDefaultSize()
        val reqH = getRequestedHeightValue()
        if (small) {
            requestedHeight = getMaxHeightValue()
        } else {
            val maxH = getMaxHeightValue()
            if (maxH < mostMinHeight || reqH >= mostMinHeight) {
                requestedHeight = reqH
            } else {
                requestedHeight = mostMinHeight
            }
        }
        closeVIThreshold = Range((height - minimizeHeight - minimizeBottomInset).coerceAtLeast(0), Int.MAX_VALUE)
        minVIThreshold = Range((closeVIThreshold.lower - minimizeHeight).coerceAtLeast(0), closeVIThreshold.lower)
        maxVIThreshold = Range(0, (minimizeHeight + (height - getMaxHeightValue())).coerceAtLeast(0))
    }

    override fun updateDivider(divider: View, dividerSize: Int) {
        divider.visibility = View.VISIBLE
        val layoutParams = divider.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = -1
        layoutParams.height = dividerSize
        layoutParams.gravity = 48
        divider.layoutParams = layoutParams
    }

    override fun updateLayoutParams(view: View) {
        val layoutParams = view.layoutParams
        layoutParams.width = getRequestedWidthValue()
        (layoutParams as? FrameLayout.LayoutParams)?.gravity = 80
        view.layoutParams = layoutParams
    }

    override fun updateMinimize(view: View): Boolean {
        if (isMinimized) return false
        if (getMaxHeightValue() <= 0) {
            val parent = view.parent as? View ?: return false
            if (parent.height <= 0) return false
            updateBehavior(parent)
        }
        val z = getMaxHeightValue() < mostMinHeight
        setMinimize(z, view)
        debug("updateMinimize maxHeight(${getMaxHeightValue()}) < mostMinHeight($mostMinHeight) -> setMinimize($z)")
        return true
    }

    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        return event.y < resizeTouchSize
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val newState = if (event.y < resizeTouchSize) FloatingPane.FloatingPaneState.STATE_RESIZE.state else FloatingPane.FloatingPaneState.STATE_IDLE.state
                viewModel.state = newState
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = FloatingPane.FloatingPaneState.STATE_IDLE.state
            }
        }
    }

    override fun getShowSpringAnimation(context: Context, target: View): SpringAnimation {
        val anim = SpringAnimation(target, DynamicAnimation.TRANSLATION_Y)
        anim.spring = SpringForce(0.0f).apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
        }
        anim.setStartValue(getRequestedHeightValue().toFloat())
        return anim
    }

    override fun getHideSpringAnimation(context: Context, target: View): SpringAnimation {
        val anim = SpringAnimation(target, DynamicAnimation.TRANSLATION_Y)
        anim.spring = SpringForce(target.height.toFloat()).apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
        }
        anim.setStartValue(0.0f)
        return anim
    }
}
