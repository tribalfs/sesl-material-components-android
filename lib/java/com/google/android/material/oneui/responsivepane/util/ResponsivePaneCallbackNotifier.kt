package com.google.android.material.oneui.responsivepane.util

import android.view.View
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag

class ResponsivePaneCallbackNotifier(
    val callbacks: MutableList<ResponsivePaneLayout.ResponsivePaneListener>
) : MutableList<ResponsivePaneLayout.ResponsivePaneListener> by callbacks, ResponsivePaneLayout.ResponsivePaneListener, ResponsivePaneTag {

    override val logTag: String = "ResponsivePaneCallbackNotifier"

    override fun onDrawerClosed(drawer: View) {
        debug("onDrawerClosed drawer=$drawer")
        callbacks.forEach { it.onDrawerClosed(drawer) }
    }

    override fun onDrawerOpened(drawer: View) {
        debug("onDrawerOpened drawer=$drawer")
        callbacks.forEach { it.onDrawerOpened(drawer) }
    }

    override fun onDrawerSlide(drawer: View, slideRatio: Float) {
        debug("onDrawerSlide drawer=$drawer, slideRatio=$slideRatio")
        callbacks.forEach { it.onDrawerSlide(drawer, slideRatio) }
    }

    override fun toString(): String {
        return callbacks.toString()
    }
}
