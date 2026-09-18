package com.google.android.material.oneui.floatingactioncontainer

import android.view.View

/**
 * Listener notified when a floating layout's [FloatingLayoutState] changes.
 */
interface FloatingLayoutStateChangeListener {
    fun onStateChange(view: View, state: FloatingLayoutState)
}
