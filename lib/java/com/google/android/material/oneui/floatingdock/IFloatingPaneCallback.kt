package com.google.android.material.oneui.floatingdock

import android.graphics.Rect
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Interface definition for a callback to be invoked when a FloatingPane event occurs.
 */
interface IFloatingPaneCallback {

    interface AnimationListener {
        fun onAnimationStart(value: Rect)
        fun onAnimationEnd(value: Rect)
        fun onAnimationUpdate(value: Rect)
    }

    fun onModeChanged(newMode: FloatingPaneMode) {}
    fun onPreInsert(rect: Rect?) {}
    fun onInsert(rect: Rect?) {}
    fun onFloatingMoved(left: Int, top: Int) {}
    fun onVisibilityChanged(visibility: Int) {}
    fun onResizeAnimate(from: Rect, to: Rect, duration: Long) {}
    fun onMinimizedChanged(mode: FloatingPaneMode, isMinimized: Boolean) {}
    fun onStateChanged(state: FloatingPaneState) {}
}