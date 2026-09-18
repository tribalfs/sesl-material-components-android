package com.google.android.material.oneui.responsivepane.controller

import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.common.internal.util.withRTLDirection
import com.google.android.material.oneui.responsivepane.ResponsiveDrawerLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag
import com.google.android.material.oneui.responsivepane.behavior.ResponsiveDrawerResizeStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneBehaviorStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneSlideOffsetCallback
import com.google.android.material.oneui.responsivepane.helper.concept.ElevationConceptRange
import com.google.android.material.oneui.responsivepane.helper.concept.ResponsivePaneElevationConcept
import com.google.android.material.oneui.responsivepane.helper.concept.toElevationTierConcept
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig

class ResponsivePaneControllerImpl : ResponsivePaneController, ResponsivePaneTag {

    companion object {
        private const val HORIZONTAL_SCROLL_DY_MARGIN = 50
        private const val LARGE_SCREEN_WIDTH = 960
    }

    override var onStateChangedListener: ((ViewGroup, PaneState) -> Unit)? = null
    override var onSlideOffsetChangedListener: ((ViewGroup, Float) -> Unit)? = null

    var currentBehaviorStrategy: ResponsivePaneBehaviorStrategy = ResponsiveDrawerResizeStrategy()
    var currentState: PaneState = currentBehaviorStrategy.getPaneState()
    private lateinit var dragHelper: ViewDragHelper
    override val logTag: String = "ResponsivePaneController"
    private var isDrawerTouchDown: Boolean = false
    private var isLargeWidth: Boolean = false
    override var isOutsideTouchEnabled: Boolean = currentBehaviorStrategy.canOutSideTouch()
        private set
    private var prev: Int = -1
    private var prevMotionX: Float = -1.0f
    private var prevMotionY: Float = -1.0f
    var responsivePaneElevationConcept: ResponsivePaneElevationConcept? = null
    lateinit var responsivePaneLayout: ResponsivePaneLayout
    private var shouldChangeState: Boolean = false

    private val dragHelperCallback = object : ViewDragHelper.Callback() {
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return child.top
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return currentBehaviorStrategy.getDragRange(responsivePaneLayout)
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            val drawerLayout = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) ?: return
            if (drawerLayout.parent != responsivePaneLayout) {
                debug("onEdgeDragStarted: Cannot capture view because it is not a direct child of responsivePaneLayout")
                return
            }
            dragHelper.captureChildView(drawerLayout, pointerId)
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return true
        }
    }

    private val internalSlideOffsetChangedListener: ResponsivePaneSlideOffsetCallback = { drawer, offset ->
        updateDrawer(drawer, offset)
        onSlideOffsetChangedListener?.invoke(drawer, offset)
    }

    private fun animateOverWidthToOpened(view: ViewGroup) {
        val layout = view as ResponsivePaneLayout
        currentBehaviorStrategy.animateOverWidthToOpened(layout, internalSlideOffsetChangedListener) {
            onStateChangedListener?.invoke(currentBehaviorStrategy.getDrawerLayout(view)!!, PaneState.OPENED)
        }
    }

    fun animateStateTransition(view: ViewGroup, toState: PaneState, skipAnimation: Boolean) {
        currentBehaviorStrategy.animateStateTransition(
            view as ResponsivePaneLayout,
            toState,
            skipAnimation,
            internalSlideOffsetChangedListener
        ) {
            onStateChangedListener?.invoke(currentBehaviorStrategy.getDrawerLayout(view)!!, toState)
        }
    }

    override fun cleanup() {
        responsivePaneElevationConcept = null
        currentBehaviorStrategy.cleanup()
    }

    override fun computeScroll(viewGroup: ViewGroup) {
        if (dragHelper.continueSettling(true)) {
            responsivePaneLayout.postInvalidateOnAnimation()
        }
    }

    override fun getCurrentPaneState(): PaneState {
        return currentBehaviorStrategy.getPaneState()
    }

    fun getElevationConcept(): ResponsivePaneElevationConcept? {
        val openConfig = getPaneConfig(PaneState.OPENED)
        val closeConfig = getPaneConfig(PaneState.CLOSED)
        if (closeConfig.elevation < 0.0f || openConfig.elevation < 0.0f) {
            warn("The Config is not complete yet close[$closeConfig], open[$openConfig]")
            return null
        }
        val minTier = closeConfig.toElevationTierConcept()
        val maxTier = openConfig.toElevationTierConcept()
        val currentMin = responsivePaneElevationConcept?.conceptRange?.minConcept
        val currentMax = responsivePaneElevationConcept?.conceptRange?.maxConcept
        val changed = minTier != currentMin || maxTier != currentMax
        if (responsivePaneElevationConcept == null || changed) {
            responsivePaneElevationConcept = ResponsivePaneElevationConcept(ElevationConceptRange(minTier, maxTier))
            if (changed) {
                debug("conceptChanged min($currentMin->$minTier), max($currentMax->$maxTier)")
            }
            debug("generate New Concept=$responsivePaneElevationConcept")
        }
        return responsivePaneElevationConcept
    }

    override fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
        return currentBehaviorStrategy.getPaneConfig(paneState)
    }

    override fun initialize(responsivePaneLayout: ResponsivePaneLayout) {
        debug("updateConstraintConnect layout=$responsivePaneLayout")
        this.responsivePaneLayout = responsivePaneLayout
        val helper = ViewDragHelper.create(responsivePaneLayout, 0.5f, dragHelperCallback)
        helper.minVelocity = 400 * responsivePaneLayout.resources.displayMetrics.density
        this.dragHelper = helper
        this.isLargeWidth = responsivePaneLayout.resources.configuration.screenWidthDp >= LARGE_SCREEN_WIDTH
        this.shouldChangeState = true
        currentBehaviorStrategy.initPaneConfig(responsivePaneLayout.context)
        updateDrawerState(responsivePaneLayout)
        updateResponsiveLayout(responsivePaneLayout)
    }

    override fun onConfigurationChanged(responsivePaneLayout: ResponsivePaneLayout, newConfig: Configuration) {
        debug("onConfigurationChanged responsivePaneLayout=$responsivePaneLayout, newConfig=$newConfig")
        val large = newConfig.screenWidthDp >= LARGE_SCREEN_WIDTH
        if (isLargeWidth != large) {
            updateDrawerState(responsivePaneLayout)
        }
        isLargeWidth = large
    }

    override fun onInterceptTouchEvent(viewGroup: ViewGroup, ev: MotionEvent): Boolean? {
        return null
    }

    override fun onLayout(viewGroup: ViewGroup, changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = viewGroup.layoutDirection == View.LAYOUT_DIRECTION_RTL
        dragHelper.setEdgeTrackingEnabled(if (rtl) ViewDragHelper.EDGE_RIGHT else ViewDragHelper.EDGE_LEFT)
    }

    fun onPaneDragged(slideOffset: Float) {
	    if (currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) !is ResponsiveDrawerLayout) return
        currentBehaviorStrategy.onPaneDragged(responsivePaneLayout, slideOffset, internalSlideOffsetChangedListener)
    }

    override fun onTouchEvent(viewGroup: ViewGroup, event: MotionEvent): Boolean? {
        val x = event.x
        dragHelper.processTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevMotionX = x
                val drawer = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout)
                prev = drawer?.width ?: 0
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> snapToDrawerIfNeed()
            MotionEvent.ACTION_MOVE -> {
                onPaneDragged(x.withRTLDirection(viewGroup))
                prevMotionX = x
            }
        }
        return false
    }

    override fun setOutsideTouchEnabled(enabled: Boolean) {
        isOutsideTouchEnabled = enabled
    }

    override fun setPaneConfig(viewGroup: ResponsivePaneLayout, paneState: PaneState, responsiveConfig: ResponsiveConfig) {
        currentBehaviorStrategy.setPaneConfig(paneState, responsiveConfig)
        if (getCurrentPaneState() == paneState) {
            debug("setPaneConfig: current Config is changed. update pane")
            setPaneState(viewGroup, paneState, false)
        }
    }

    override fun setPaneState(viewGroup: ViewGroup, state: PaneState, animate: Boolean) {
        animateStateTransition(viewGroup, state, !animate)
    }

    private fun snapToDrawerIfNeed() {
        val drawer = currentBehaviorStrategy.getDrawerLayout(responsivePaneLayout) ?: return
        val openWidth = currentBehaviorStrategy.getDrawerPaneWidth(responsivePaneLayout, PaneState.OPENED)
        val closedWidth = currentBehaviorStrategy.getDrawerPaneWidth(responsivePaneLayout, PaneState.CLOSED)
        if (drawer.width > openWidth) {
            animateOverWidthToOpened(responsivePaneLayout)
            return
        }
        val threshold = ((openWidth - closedWidth) * 0.5f) + closedWidth
        if (drawer.width < threshold) {
            animateStateTransition(responsivePaneLayout, PaneState.CLOSED, drawer.width == closedWidth)
        } else {
            animateStateTransition(responsivePaneLayout, PaneState.OPENED, drawer.width == openWidth)
        }
    }

    private fun updateDrawer(drawer: ResponsiveDrawerLayout, slideRatio: Float) {
        getElevationConcept()?.applyConcept(drawer, slideRatio)
    }

    private fun updateDrawerState(view: ResponsivePaneLayout) {
        val defaultOpen = view.resources.getBoolean(R.bool.sesl_responsive_pane_navigation_drawer_default_open)
        debug("updateDrawer State:$defaultOpen")
        setPaneState(view, if (defaultOpen) PaneState.OPENED else PaneState.CLOSED, false)
    }

    override fun updateResponsiveLayout(view: ResponsivePaneLayout) {
        currentBehaviorStrategy.updateConstraintConnect(view)
    }
}
