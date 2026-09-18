package com.google.android.material.oneui.floatingactioncontainer.manager.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.SeslScrollable
import com.google.android.material.oneui.floatingactioncontainer.manager.SeslScrollableListener

/**
 * Adapter interface wrapping scrollable views (such as [androidx.recyclerview.widget.RecyclerView]
 * or [androidx.core.widget.NestedScrollView]) to abstract scroll events and layout inspection.
 */
interface FloatingScrollableAdapter {
    /**
     * Registers a listener for scroll events.
     */
    fun addSeslScrollableListener(listener: SeslScrollableListener)

    /**
     * Unregisters a scroll event listener.
     */
    fun removeSeslScrollableListener(listener: SeslScrollableListener)

    /**
     * Releases scroll listeners and resources attached to the underlying view.
     */
    fun dispose()

    /**
     * Returns the underlying [SeslScrollable] instance if available.
     */
    fun getFloatingScrollable(): SeslScrollable? = null

    /**
     * Checks if content views inside the scrollable container are currently visible within screen bounds.
     */
    fun isInScreen(availHeight: Int, appbarOffset: Int, appbarRange: Int = 0): Boolean = true

    /**
     * Checks whether the target view meets condition constraints.
     */
    fun isValidCondition(view: View): Boolean = true

    /**
     * Returns true if the specified child view is considered empty (e.g. no visible children or empty text).
     */
    fun isEmptyView(view: View): Boolean {
        if (view is ViewGroup) {
            for (child in view.children) {
                if (child.isVisible) {
                    return false
                }
            }
            return true
        } else if (view is TextView) {
            val text = view.text
            return text.isNullOrEmpty()
        }
        return false
    }
}
