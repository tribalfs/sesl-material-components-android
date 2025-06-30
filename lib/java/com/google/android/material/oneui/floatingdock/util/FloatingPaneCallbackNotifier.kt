package com.google.android.material.oneui.floatingdock.util

import android.graphics.Rect
import android.util.Log
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState
import com.google.android.material.oneui.floatingdock.IFloatingPaneCallback

/**
 * Notifies all registered [IFloatingPaneCallback]s of floating pane events.
 * Also implements [List] for convenient callback management.
 */
class FloatingPaneCallbackNotifier(
    private val callbacks: MutableList<IFloatingPaneCallback> = mutableListOf()
) : MutableList<IFloatingPaneCallback> by callbacks, IFloatingPaneCallback {

    companion object {
        private const val TAG = "FloatingPaneCallback"
    }

    var debug: Boolean = true

    override fun onModeChanged(newMode: FloatingPaneMode) {
        if (debug) {
            Log.d(TAG, "callback OnModeChanged=$newMode")
        }
        callbacks.forEach { it.onModeChanged(newMode) }
    }

    override fun onPreInsert(rect: Rect?) {
        if (debug) {
            Log.d(TAG, "callback OnPreInsert=$rect")
        }
        callbacks.forEach { it.onPreInsert(rect) }
    }

    override fun onInsert(rect: Rect?) {
        if (debug) {
            Log.d(TAG, "callback OnInsert=$rect")
        }
        callbacks.forEach { it.onInsert(rect) }
    }

    override fun onFloatingMoved(left: Int, top: Int) {
        if (debug) {
            Log.d(TAG, "callback onFloatingMoved left=$left, top=$top")
        }
        callbacks.forEach { it.onFloatingMoved(left, top) }
    }

    override fun onVisibilityChanged(visibility: Int) {
        if (debug) {
            Log.d(TAG, "callback OnVisibilityChanged=$visibility")
        }
        callbacks.forEach { it.onVisibilityChanged(visibility) }
    }

    override fun onResizeAnimate(from: Rect, to: Rect, duration: Long) {
        if (debug) {
            Log.d(TAG, "callback onResizeAnimate from=$from -> to=$to, duration=$duration")
        }
        callbacks.forEach { it.onResizeAnimate(from, to, duration) }
    }

    override fun onMinimizedChanged(mode: FloatingPaneMode, isMinimized: Boolean) {
        if (debug) {
            Log.d(TAG, "callback onMinimizedChanged mode=$mode, isMinimized=$isMinimized")
        }
        callbacks.forEach { it.onMinimizedChanged(mode, isMinimized) }
    }

    override fun onStateChanged(state: FloatingPaneState) {
        if (debug) {
            Log.d(TAG, "callback onStateChanged state=$state")
        }
        callbacks.forEach { it.onStateChanged(state) }
    }
}