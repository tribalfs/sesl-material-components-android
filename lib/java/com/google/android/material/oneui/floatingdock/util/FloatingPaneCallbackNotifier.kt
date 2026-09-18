package com.google.android.material.oneui.floatingdock.util

import android.graphics.Rect
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.floatingdock.FloatingDockLogTag
import com.google.android.material.oneui.floatingdock.FloatingPane
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback

class FloatingPaneCallbackNotifier(
    val callbacks: MutableList<IFloatingPaneCallback>
) : MutableList<IFloatingPaneCallback> by callbacks, IFloatingPaneCallback, FloatingDockLogTag {

    override val logTag: String = "FloatingPaneCallbackNotifier"

    override fun onModeChanged(newMode: Int) {
        debug("callback OnModeChanged=${FloatingPane.FloatingPaneMode(newMode)}")
        callbacks.forEach { it.onModeChanged(newMode) }
    }

    override fun onStateChanged(state: Int) {
        debug("callback onStateChanged state=${FloatingPane.FloatingPaneState(state)}")
        callbacks.forEach { it.onStateChanged(state) }
    }

    override fun onVisibilityChanged(visibility: Int) {
        debug("callback OnVisibilityChanged=$visibility")
        callbacks.forEach { it.onVisibilityChanged(visibility) }
    }

    override fun onPreInsert(rect: Rect) {
        debug("callback OnPreInsert=$rect")
        callbacks.forEach { it.onPreInsert(rect) }
    }

    override fun onInsert(rect: Rect) {
        debug("callback OnInsert=$rect")
        callbacks.forEach { it.onInsert(rect) }
    }

    override fun onResizeAnimate(from: Rect, to: Rect, duration: Long) {
        debug("callback onResizeAnimate from=$from -> to=$to, duration=$duration")
        callbacks.forEach { it.onResizeAnimate(from, to, duration) }
    }

    override fun onLayoutChanged(left: Int, top: Int, right: Int, bottom: Int) {
        debug("callback onLayoutChanged left=$left, top=$top, right=$right, bottom=$bottom")
        callbacks.forEach { it.onLayoutChanged(left, top, right, bottom) }
    }

    override fun onFloatingMoved(left: Int, top: Int) {
        debug("callback onFloatingMoved $left,$top")
        callbacks.forEach { it.onFloatingMoved(left, top) }
    }

    override fun onMinimizedChanged(mode: Int, isMinimized: Boolean) {
        debug("callback onMinimizedChanged mode=${FloatingPane.FloatingPaneMode(mode)}, isMinimized=$isMinimized")
        callbacks.forEach { it.onMinimizedChanged(mode, isMinimized) }
    }
}
