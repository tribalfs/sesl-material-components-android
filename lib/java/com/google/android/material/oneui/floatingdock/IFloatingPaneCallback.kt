package com.google.android.material.oneui.floatingdock

import android.graphics.Rect

/**
 * Callback interface for receiving lifecycle, state, and bounds transformation events
 * from Samsung One UI FloatingPane components.
 */
interface IFloatingPaneCallback {
    interface AnimationListener {
        fun onAnimationStart(bounds: Rect)
        fun onAnimationUpdate(bounds: Rect)
        fun onAnimationEnd(bounds: Rect)
    }

    fun onModeChanged(newMode: Int) {}
    fun onStateChanged(state: Int) {}
    fun onVisibilityChanged(visibility: Int) {}
    fun onPreInsert(rect: Rect) {}
    fun onInsert(rect: Rect) {}
    fun onResizeAnimate(from: Rect, to: Rect, duration: Long) {}
    fun onLayoutChanged(left: Int, top: Int, right: Int, bottom: Int) {}
    fun onFloatingMoved(left: Int, top: Int) {}
    fun onMinimizedChanged(mode: Int, isMinimized: Boolean) {}
}
