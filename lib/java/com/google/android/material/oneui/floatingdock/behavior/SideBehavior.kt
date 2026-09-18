package com.google.android.material.oneui.floatingdock.behavior

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.util.getFloat
import com.google.android.material.oneui.common.internal.util.getScreenWidth
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.FloatingPaneViewModel
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class SideBehavior(
    val context: Context,
    mode: Int,
    callBackNotifier: FloatingPaneCallbackNotifier,
    private val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    override val logTag: String = "SideBehavior"
    private val mostMinWidth: Int = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_side_mode_most_min_width)
    private var isRightSide: Boolean = true

    init {
        updateDefaultSize()
        requestedHeight = -1
    }

    private fun updateDefaultSize() {
        requestedWidth = (context.getScreenWidth() * context.getFloat(R.dimen.sesl_floating_pane_side_mode_default_width, 0.45f)).toInt()
    }

    private fun resizeCheck(view: View, event: MotionEvent): Boolean {
        return if (isRightSide) {
            event.x < resizeTouchSize
        } else {
            event.x > view.width - resizeTouchSize
        }
    }

    override var customMinHeight: Int?
        get() = super.customMinHeight
        set(value) {
            debug("custom Min Height can't change in this Mode")
            super.customMinHeight = null
        }

    override var customMaxHeight: Int?
        get() = super.customMaxHeight
        set(value) {
            debug("custom Max Height can't change in this Mode")
            super.customMaxHeight = null
        }

    override var customHeight: Int?
        get() = super.customHeight
        set(value) {
            debug("custom Height can't change in this Mode")
            super.customHeight = null
        }

    override var customMinimizeWidth: Int?
        get() = super.customMinimizeWidth
        set(value) {
            debug("custom Minimize Width can't change in this Mode")
            super.customMinimizeWidth = null
        }

    override fun getRequestedHeightValue(): Int {
        return requestedHeight
    }

    override fun getBackgroundResId(): Int {
        customBackground?.let { return it }
        return if (SeslMisc.isLightTheme(context)) {
            if (isRightSide) R.drawable.sesl_floating_pane_background_side_right else R.drawable.sesl_floating_pane_background_side_left
        } else {
            if (isRightSide) R.drawable.sesl_floating_pane_background_side_right_dark else R.drawable.sesl_floating_pane_background_side_left_dark
        }
    }

    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) R.layout.sesl_floating_pane_menu_side else R.layout.sesl_floating_pane_menu_side_dark
    }

    override fun getResizePinDirectionFlags(): Int {
        return (if (isRightSide) 4 else 1) or 10
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect(0, 0, view.width, view.height)
        if (isRightSide) {
            rect.left = view.width - getRequestedWidthValue()
        } else {
            rect.right = getRequestedWidthValue()
        }
        return rect
    }

    override fun updateBehavior(parent: View) {
        val typedValue = TypedValue()
        val width = parent.width
        val screenWidth = context.getScreenWidth()
        val f6 = context.getFloat(R.dimen.sesl_floating_pane_side_mode_min_width, 0.3f)
        updateDefaultSize()
        minWidth = (screenWidth * f6).toInt()
        if (minWidth < mostMinWidth) {
            minWidth = mostMinWidth
        }
        val dimension = if (context.getFloat(R.dimen.sesl_floating_pane_side_mode_max_width, typedValue)) {
            typedValue.float * width
        } else {
            parent.resources.getDimension(R.dimen.sesl_floating_pane_side_mode_max_width)
        }
        maxWidth = dimension.toInt()
        isRightSide = parent.layoutDirection != View.LAYOUT_DIRECTION_RTL
    }

    override fun updateDivider(divider: View, dividerSize: Int) {
        divider.visibility = View.VISIBLE
        val layoutParams = divider.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = dividerSize
        layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
        layoutParams.gravity = 8388611
        divider.layoutParams = layoutParams
    }

    override fun updateLayoutParams(view: View) {
        val layoutParams = view.layoutParams
        layoutParams.height = getRequestedHeightValue()
        (layoutParams as? FrameLayout.LayoutParams)?.gravity = 51
        view.layoutParams = layoutParams
    }

    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        return event.y < resizeTouchSize || resizeCheck(view, event)
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val newState = if (resizeCheck(view, event)) FloatingPane.FloatingPaneState.STATE_RESIZE.state else FloatingPane.FloatingPaneState.STATE_IDLE.state
                viewModel.state = newState
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = FloatingPane.FloatingPaneState.STATE_IDLE.state
            }
        }
    }

    override fun getShowSpringAnimation(context: Context, target: View): SpringAnimation {
        val anim = SpringAnimation(target, DynamicAnimation.TRANSLATION_X)
        anim.spring = SpringForce(0.0f).apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
        }
        val startVal = getRequestedWidthValue().toFloat()
        anim.setStartValue(if (isRightSide) startVal else -startVal)
        return anim
    }

    override fun getHideSpringAnimation(context: Context, target: View): SpringAnimation {
        val anim = SpringAnimation(target, DynamicAnimation.TRANSLATION_X)
        val finalVal = target.width.toFloat()
        anim.spring = SpringForce(if (isRightSide) finalVal else -finalVal).apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
        }
        anim.setStartValue(0.0f)
        return anim
    }
}
