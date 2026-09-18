package com.google.android.material.oneui.responsivepane.behavior

import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.oneui.responsivepane.ResponsiveDrawerLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

typealias ResponsivePaneSlideOffsetCallback = (ResponsiveDrawerLayout, Float) -> Unit

interface ResponsivePaneBehaviorStrategy {
    fun animateOverWidthToOpened(
        layout: ResponsivePaneLayout,
        callback: ResponsivePaneSlideOffsetCallback? = null,
        onAnimationEnd: () -> Unit
    )

    fun animateStateTransition(
        layout: ResponsivePaneLayout,
        toState: PaneState,
        skipAnimation: Boolean,
        callback: ResponsivePaneSlideOffsetCallback? = null,
        onAnimationEnd: () -> Unit
    )

    fun canOutSideTouch(): Boolean
    fun cleanup()
    fun getDragRange(layout: ConstraintLayout): Int
    fun getDrawerLayout(layout: ViewGroup): ViewGroup?
    fun getDrawerPaneWidth(layout: ConstraintLayout, state: PaneState): Int
    fun getPaneConfig(paneState: PaneState): ResponsiveConfig
    fun getPaneState(): PaneState
    fun initPaneConfig(context: Context)
    fun onPaneDragged(parent: ResponsivePaneLayout, offset: Float, callback: ResponsivePaneSlideOffsetCallback? = null)
    fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig)
    fun shouldAllowDrag(): Boolean
    fun updateConstraintConnect(parent: ConstraintLayout)
}
