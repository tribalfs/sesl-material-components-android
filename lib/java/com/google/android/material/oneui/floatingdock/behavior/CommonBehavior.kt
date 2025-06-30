package com.google.android.material.oneui.floatingdock.behavior

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.dynamicanimation.animation.SpringAnimation
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier


/**
 * Base class for defining the behavior of a floating pane in different modes.
 * This class provides common functionalities and properties that can be extended
 * by specific behavior implementations.
 *
 * @property mode The [FloatingPaneMode] this behavior is associated with.
 * @property callbackNotifier A [FloatingPaneCallbackNotifier] to dispatch callback events.
 */
abstract class CommonBehavior(
    val mode: FloatingPaneMode,
    val callbackNotifier: FloatingPaneCallbackNotifier
) {

    companion object {
        const val TAG = "CommonBehavior"
    }

    @DrawableRes
    var customBackground: Int? = null

    open var customWidth: Int? = null
    open var customHeight: Int? = null
    open var customMinWidth: Int? = null
    open var customMinHeight: Int? = null
    open var customMaxWidth: Int? = null
    open var customMaxHeight: Int? = null
    open var customMinimizeWidth: Int? = null

    var posXRatio: Float = -1f
    var posYRatio: Float = -1f

    var isMinimized: Boolean = false
        private set

    open var showAnimationListener: IFloatingPaneCallback.AnimationListener? = null
    open var hideAnimationListener: IFloatingPaneCallback.AnimationListener? = null


    private var _minWidth: Int = -1
    private var _minHeight: Int = -1
    private var _maxWidth: Int = -1
    private var _maxHeight: Int = -1

    fun getMinWidth(): Int = customMinWidth ?: _minWidth
    fun setMinWidth(value: Int) {
        _minWidth = value
    }

    fun getMinHeight(): Int = customMinHeight ?: _minHeight
    fun setMinHeight(value: Int) {
        _minHeight = value
    }

    fun getMaxWidth(): Int = customMaxWidth ?: _maxWidth
    fun setMaxWidth(value: Int) {
        _maxWidth = value
    }

    fun getMaxHeight(): Int = customMaxHeight ?: _maxHeight
    fun setMaxHeight(value: Int) {
        _minHeight = value
    }

    open var requestedWidth: Int = -1
        get() {
            val custom = customWidth ?: field
            val min = getMinWidth().takeIf { it >= 0 } ?: customMinWidth ?: -1
            val max = getMaxWidth().takeIf { it >= 0 } ?: customMaxWidth ?: -1
            var value = custom
            if (min >= 0 && value < min) value = min
            if (max >= 0 && max < value) value = max
            return value
        }

    open var requestedHeight: Int = -1
        get() {
            val custom = customHeight ?: field
            val min = getMinHeight().takeIf { it >= 0 } ?: customMinHeight ?: -1
            val max = getMaxHeight().takeIf { it >= 0 } ?: customMaxHeight ?: -1
            var value = custom
            if (min >= 0 && value < min) value = min
            if (max >= 0 && max < value) value = max
            return value
        }


    var minimizeWidth: Int = -1
        get() {
            val custom = customMinimizeWidth ?: field
            val min = minimizeMinWidth
            val max = minimizeMaxWidth
            var value = custom
            if (min >= 0 && value < min) value = min
            if (max >= 0 && max < value) value = max
            return value
        }

    var minimizeHeight: Int = -1
    var minimizeMaxWidth: Int = -1
    var minimizeMaxHeight: Int = Int.MAX_VALUE
    var minimizeMinWidth: Int = -1
    var minimizeMinHeight: Int = -1

    open fun setMinimize(minimize: Boolean, view: View) {
        isMinimized = minimize
        callbackNotifier.onMinimizedChanged(mode, minimize)
    }

    open fun getBackgroundResId(): Int = -1

    @LayoutRes
    open fun getMenuLayoutResId(): Int = -1

    open fun getShowAnimation(context: Context, target: View): AnimatorSet? = null
    open fun getHideAnimation(context: Context, target: View): AnimatorSet? = null
    open fun getShowSpringAnimation(context: Context, target: View): SpringAnimation? = null
    open fun getHideSpringAnimation(context: Context, target: View): SpringAnimation? = null

    open fun getTargetModeBounds(view: View, moveValidArea: Boolean = true): Rect = Rect()

    open fun getMinimizeRect(minimize: Boolean, from: Rect): Rect = Rect()

    open fun isSupportMinimize(): Boolean = false

    open fun isSupported(context: Context): Boolean = true

    open fun isMinimizableRect(newRect: Rect): Boolean = false

    open fun shouldInterceptTouch(view: View, event: MotionEvent): Boolean = false

    open fun updateMinimize(view: View): Boolean = false

    open fun updateLayoutParams(view: View) {}

    open fun updateState(view: View, event: MotionEvent) {}

    open fun saveState(parent: View) {}

    open fun loadState(parent: View) {}

    open fun initBehavior(parent: View) {
        updateBehavior(parent)
    }

    abstract fun updateBehavior(parent: View)

    abstract fun getResizePinDirectionFlags(): Int

}