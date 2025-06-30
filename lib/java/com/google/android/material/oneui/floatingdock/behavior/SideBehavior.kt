package com.google.android.material.oneui.floatingdock.behavior

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
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
import com.google.android.material.oneui.floatingdock.util.screenWidth

/**
 * Behavior class for the "Side" mode of the FloatingPane.
 * This mode displays the pane on the side of the screen.
 *
 * @property context The [Context] for accessing resources.
 * @property mode The [FloatingPaneMode] of the pane.
 * @property callBackNotifier The [FloatingPaneCallbackNotifier] to send callbacks.
 * @property viewModel The [FloatingPaneViewModel] for managing the pane's state.
 * @property resizeTouchSize The size of the touch area for resizing the pane.
 */
class SideBehavior(
    val context: Context,
    mode: FloatingPaneMode,
    callBackNotifier: FloatingPaneCallbackNotifier,
    val viewModel: FloatingPaneViewModel,
    private val resizeTouchSize: Int
) : CommonBehavior(mode, callBackNotifier) {

    companion object {
        const val TAG = "SideBehavior"
    }

    override var customHeight: Int? = null
        set(value) {
            Log.d(TAG, "custom height can't change in this Mode")
            customHeight = null
        }

    override var customMaxHeight: Int? = null
        set(value) {
            Log.d(BottomBehavior.TAG, "custom max height can't change in this Mode")
            customMaxHeight = null
        }

    override var customMinHeight: Int? = null
        set(value) {
            Log.d(TAG, "custom height can't change in this Mode")
            customHeight = null
        }

    override var customMinimizeWidth: Int? = null
        set(value) {
            Log.d(TAG, "custom min height can't change in this Mode")
            customMinHeight = null
        }

    private var isRightSide: Boolean = true
    private val mostMinWidth: Int =
        context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_side_mode_most_min_width)
    override var requestedHeight: Int = -1
        set(value) {
            Log.d(TAG, "height can't change in this Mode")
            requestedHeight = -1
        }

    init {
        updateDefaultSize()
    }

    private fun resizeCheck(view: View, event: MotionEvent): Boolean {
        return if (isRightSide) {
            event.x < resizeTouchSize
        } else {
            event.x > view.width - resizeTouchSize
        }
    }

    private fun updateDefaultSize() {
        requestedWidth = (context.screenWidth * context.getFloat(
            R.dimen.sesl_floating_pane_side_mode_default_width,
            0.45f
        )).toInt()
    }

    @DrawableRes
    override fun getBackgroundResId(): Int {
        val customBackground = customBackground
        return customBackground ?: if (SeslMisc.isLightTheme(context)) {
            if (isRightSide) R.drawable.sesl_floating_pane_background_side_right
            else R.drawable.sesl_floating_pane_background_side_left
        } else {
            if (isRightSide) R.drawable.sesl_floating_pane_background_side_right_dark
            else R.drawable.sesl_floating_pane_background_side_left_dark
        }
    }

    override fun getHideSpringAnimation(context: Context, target: View): SpringAnimation {
        val springAnimation = SpringAnimation(target, DynamicAnimation.TRANSLATION_X)
        val springForce = SpringForce().apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
            finalPosition = if (isRightSide) target.width.toFloat() else -target.width.toFloat()
        }
        springAnimation.spring = springForce
        springAnimation.setStartValue(0f)
        return springAnimation
    }

    @LayoutRes
    override fun getMenuLayoutResId(): Int {
        return if (SeslMisc.isLightTheme(context)) {
            R.layout.sesl_floating_pane_menu_side
        } else {
            R.layout.sesl_floating_pane_menu_side_dark
        }
    }

    override fun getResizePinDirectionFlags(): Int {
        return (if (isRightSide) 4 else 1) or 10
    }

    override fun getShowSpringAnimation(context: Context, target: View): SpringAnimation {
        val springAnimation = SpringAnimation(target, DynamicAnimation.TRANSLATION_X)
        val springForce = SpringForce().apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
            finalPosition = 0f
        }
        springAnimation.spring = springForce
        springAnimation.setStartValue(if (isRightSide) requestedWidth.toFloat() else -requestedWidth.toFloat())
        return springAnimation
    }

    override fun getTargetModeBounds(view: View, moveValidArea: Boolean): Rect {
        val rect = Rect()
        rect.right = view.width
        rect.bottom = view.height
        if (isRightSide) {
            rect.left = view.width - requestedWidth
        } else {
            rect.right = requestedWidth
        }
        return rect
    }


    override fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean {
        return event.y < resizeTouchSize.toFloat() || resizeCheck(view, event)
    }

    override fun updateBehavior(parent: View) {
        val typedValue = TypedValue()
        val width = parent.width
        val screenWidth = context.screenWidth
        val minWidthFraction = parent.getFloat(R.dimen.sesl_floating_pane_side_mode_min_width, 0.3f)
        updateDefaultSize()
        setMinWidth((screenWidth * minWidthFraction).toInt())
        if (getMinWidth() < mostMinWidth) {
            setMinWidth(mostMinWidth)
        }
        val maxWidthRes = R.dimen.sesl_floating_pane_side_mode_max_width
        setMaxWidth(
            if (parent.getFloat(maxWidthRes, typedValue)) {
                (typedValue.float * width).toInt()
            } else {
                parent.resources.getDimension(maxWidthRes).toInt()
            }
        )
        isRightSide = parent.layoutDirection != View.LAYOUT_DIRECTION_RTL
    }

    override fun updateLayoutParams(view: View) {
        val layoutParams = view.layoutParams
        layoutParams.height = requestedHeight
        if (layoutParams is FrameLayout.LayoutParams) {
            layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        }
        view.layoutParams = layoutParams
    }

    override fun updateState(view: View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewModel.state = (
                        if (resizeCheck(view, event))
                            FloatingPane.FloatingPaneState.STATE_RESIZE
                        else
                            FloatingPane.FloatingPaneState.STATE_IDLE
                        )
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.state = FloatingPane.FloatingPaneState.STATE_IDLE
            }
        }
    }
}