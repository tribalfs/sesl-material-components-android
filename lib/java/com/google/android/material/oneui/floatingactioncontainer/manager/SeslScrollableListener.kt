package com.google.android.material.oneui.floatingactioncontainer.manager

import android.view.View

/**
 * Listener for scroll and fast-scroll events on views managed by [FloatingScrollableManager].
 */
interface SeslScrollableListener {
    fun onScrolled(view: View?, dx: Int, dy: Int)

    fun onFastScrollStart(view: View?) {}

    fun onFastScrollEnd(view: View?) {}
}
