package com.google.android.material.oneui.floatingdock.behavior

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.SpringAnimation
import com.google.android.material.oneui.floatingdock.FloatingDockLogTag
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier

abstract class CommonBehavior(
    val mode: FloatingPaneMode,
    protected val callbackNotifier: FloatingPaneCallbackNotifier
) : FloatingDockLogTag {

    var minWidth: Int = -1
    var minHeight: Int = -1
    var maxWidth: Int = -1
    var maxHeight: Int = -1
    open var requestedWidth: Int = -1
    open var requestedHeight: Int = -1
    var posXRatio: Float = -1.0f
    var posYRatio: Float = -1.0f
    var minimizeWidth: Int = -1
    var minimizeHeight: Int = -1
    var minimizeMaxWidth: Int = -1
    var minimizeMaxHeight: Int = Int.MAX_VALUE
    var minimizeMinWidth: Int = -1
    var minimizeMinHeight: Int = -1
    var isMinimized: Boolean = false
        private set

    open var showAnimationListener: IFloatingPaneCallback.AnimationListener? = null
    open var hideAnimationListener: IFloatingPaneCallback.AnimationListener? = null
    var customBackground: Int? = null

    open var customMinWidth: Int? = null
    open var customMinHeight: Int? = null
    open var customMaxWidth: Int? = null
    open var customMaxHeight: Int? = null
    open var customWidth: Int? = null
    open var customHeight: Int? = null
    open var customMinimizeWidth: Int? = null

    open fun getMinWidthValue(): Int {
        return customMinWidth ?: minWidth
    }

    open fun getMinHeightValue(): Int {
        return customMinHeight ?: minHeight
    }

    open fun getMaxWidthValue(): Int {
        return customMaxWidth ?: maxWidth
    }

    open fun getMaxHeightValue(): Int {
        return customMaxHeight ?: maxHeight
    }

    open fun getRequestedWidthValue(): Int {
        val custom = customWidth ?: requestedWidth
        val min = getMinWidthValue()
        var result = if (min >= 0 && custom < min) min else custom
        val max = getMaxWidthValue()
        if (max in 0..<result) {
            result = max
        }
        return result
    }

    open fun getRequestedHeightValue(): Int {
        val custom = customHeight ?: requestedHeight
        val min = getMinHeightValue()
        var result = if (min >= 0 && custom < min) min else custom
        val max = getMaxHeightValue()
        if (max in 0..<result) {
            result = max
        }
        return result
    }

    open fun getMinimizeWidthValue(): Int {
        val custom = customMinimizeWidth ?: minimizeWidth
        var result = if (minimizeMinWidth >= 0 && custom < minimizeMinWidth) minimizeMinWidth else custom
        if (minimizeMaxWidth in 0..<result) {
            result = minimizeMaxWidth
        }
        return result
    }

    open fun getMinimizeHeightValue(): Int {
        var result = if (minimizeMinHeight >= 0 && minimizeHeight < minimizeMinHeight) minimizeMinHeight else minimizeHeight
        if (minimizeMaxHeight in 0..<result) {
            result = minimizeMaxHeight
        }
        return result
    }

    open fun getBackgroundResId(): Int = -1
    open fun getMenuLayoutResId(): Int = -1

    abstract fun getResizePinDirectionFlags(): Int
    abstract fun updateBehavior(parent: View)

    open fun initBehavior(parent: View) {
        updateBehavior(parent)
    }

    open fun getTargetModeBounds(view: View, moveValidArea: Boolean = true): Rect {
        return Rect()
    }

    open fun getMinimizeRect(minimize: Boolean, from: Rect): Rect {
        return Rect()
    }

    open fun isSupportMinimize(): Boolean = false
    open fun isMinimizableRect(newRect: Rect): Boolean = false

    open fun isSupported(context: Context): Boolean = true

    open fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean = false
    open fun updateState(view: View, event: MotionEvent) {}

    open fun updateDivider(divider: View, dividerSize: Int) {
        divider.visibility = View.GONE
    }

    open fun updateLayoutParams(view: View) {}
    open fun updateMinimize(view: View): Boolean = false

    open fun setMinimize(minimize: Boolean, view: View) {
        if (isMinimized != minimize) {
            isMinimized = minimize
            callbackNotifier.onMinimizedChanged(mode, minimize)
        }
    }

    open fun saveState(parent: View) {}
    open fun loadState(parent: View) {}

    open fun getShowAnimation(context: Context, target: View): AnimatorSet? = null
    open fun getHideAnimation(context: Context, target: View): AnimatorSet? = null
    open fun getShowSpringAnimation(context: Context, target: View): SpringAnimation? = null
    open fun getHideSpringAnimation(context: Context, target: View): SpringAnimation? = null
}
