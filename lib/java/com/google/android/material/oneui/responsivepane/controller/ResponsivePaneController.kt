package com.google.android.material.oneui.responsivepane.controller

import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ViewGroup
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

interface ResponsivePaneController {
    var onStateChangedListener: ((ViewGroup, PaneState) -> Unit)?
    var onSlideOffsetChangedListener: ((ViewGroup, Float) -> Unit)?

    fun initialize(responsivePaneLayout: ResponsivePaneLayout)
    fun updateResponsiveLayout(view: ResponsivePaneLayout)
    fun onInterceptTouchEvent(viewGroup: ViewGroup, ev: MotionEvent): Boolean?
    fun onTouchEvent(viewGroup: ViewGroup, event: MotionEvent): Boolean?
    fun onLayout(viewGroup: ViewGroup, changed: Boolean, left: Int, top: Int, right: Int, bottom: Int)
    fun onConfigurationChanged(responsivePaneLayout: ResponsivePaneLayout, newConfig: Configuration)
    fun computeScroll(viewGroup: ViewGroup)
    val isOutsideTouchEnabled: Boolean
    fun setOutsideTouchEnabled(enabled: Boolean)
    fun setPaneState(viewGroup: ViewGroup, state: PaneState, animate: Boolean)
    fun getPaneConfig(paneState: PaneState): ResponsiveConfig
    fun setPaneConfig(viewGroup: ResponsivePaneLayout, paneState: PaneState, responsiveConfig: ResponsiveConfig)
    fun getCurrentPaneState(): PaneState
    fun cleanup()
}
