package com.google.android.material.oneui.responsivepane

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.customview.widget.Openable
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.info
import com.google.android.material.oneui.responsivepane.controller.ResponsivePaneController
import com.google.android.material.oneui.responsivepane.controller.ResponsivePaneControllerImpl
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import com.google.android.material.oneui.responsivepane.util.ResponsivePaneCallbackNotifier

/**
 * An adaptive Samsung One UI container layout extending [ConstraintLayout] that manages side navigation drawers
 * and content panes, supporting responsive pane states (OPENED, CLOSED) across tablet, foldable, and desktop screens.
 *
 * <p>Integrates with One UI elevation concepts, predictive back handling via [OnBackInvokedDispatcher], and drag gestures.
 */
class ResponsivePaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Openable, ResponsivePaneTag {

    interface ResponsivePaneListener {
        fun onDrawerClosed(drawer: View)
        fun onDrawerOpened(drawer: View)
        fun onDrawerSlide(drawer: View, slideRatio: Float)
    }

    private var actionModeBackInvoked: OnBackInvokedCallback? = null
    val controller: ResponsivePaneController = ResponsivePaneControllerImpl()
    private var isFirstLayout: Boolean = true
    override val logTag: String = "ResponsivePaneLayout"
    private val responsivePaneCallbackNotifier = ResponsivePaneCallbackNotifier(mutableListOf())

    init {
        setupControllerCallbacks()
        controller.initialize(this)
    }

    private fun findResponsiveDrawerLayout(): ResponsiveDrawerLayout? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ResponsiveDrawerLayout) {
                return child
            }
        }
        return null
    }

    private fun getPaneState(): PaneState {
        return controller.getCurrentPaneState()
    }

    private fun isModalOpenCase(drawer: ResponsiveDrawerLayout?): Boolean {
        return drawer != null && drawer.visibility == View.VISIBLE && isOpen && !isOutsideTouchEnabled()
    }

    private fun onSlideOffsetChanged(viewGroup: ViewGroup?, slideRatio: Float) {
        if (viewGroup != null) {
            responsivePaneCallbackNotifier.onDrawerSlide(viewGroup, slideRatio)
        }
    }

    private fun onStateChanged(viewGroup: ViewGroup?, state: PaneState) {
        if (viewGroup != null) {
            if (state == PaneState.OPENED) {
                responsivePaneCallbackNotifier.onDrawerOpened(viewGroup)
            } else {
                responsivePaneCallbackNotifier.onDrawerClosed(viewGroup)
            }
        }
        updateChildrenImportantForAccessibility()
    }

    private fun registerOnBackInvokedCallback(currentDispatcher: OnBackInvokedDispatcher) {
        if (Build.VERSION.SDK_INT >= 33) {
            debug("registerOnBackInvokedCallback currentDispatcher=$currentDispatcher")
            if (actionModeBackInvoked == null) {
                actionModeBackInvoked = OnBackInvokedCallback {
                    debug("actionModeBackInvoked isOpen=$isOpen")
                    if (isOpen) {
                        close()
                    }
                    unregisterOnBackInvokedCallback(currentDispatcher)
                }
            }
            actionModeBackInvoked?.let {
                currentDispatcher.registerOnBackInvokedCallback(1_000_000, it)
            }
        }
    }

    fun setPaneState(state: PaneState, animate: Boolean = true) {
        controller.setPaneState(this, state, animate)
    }

    private fun setupControllerCallbacks() {
        controller.onStateChangedListener = { viewGroup, state ->
            updateBackInvokedCallbackState()
            onStateChanged(viewGroup, state)
        }
        controller.onSlideOffsetChangedListener = { viewGroup, ratio ->
            onSlideOffsetChanged(viewGroup, ratio)
        }
    }

    private fun unregisterOnBackInvokedCallback(currentDispatcher: OnBackInvokedDispatcher) {
        if (Build.VERSION.SDK_INT >= 33) {
            val callback = actionModeBackInvoked ?: return
            debug("unregisterOnBackInvokedCallback currentDispatcher=$currentDispatcher")
            currentDispatcher.unregisterOnBackInvokedCallback(callback)
            actionModeBackInvoked = null
        }
    }

    private fun updateBackInvokedCallbackState() {
        if (Build.VERSION.SDK_INT >= 33) {
            val dispatcher = findOnBackInvokedDispatcher()
            if (dispatcher == null) {
                info("updateBackInvokedCallbackState backInvoked=null, parent=$parent")
            } else if (isModalOpenCase(findResponsiveDrawerLayout())) {
                registerOnBackInvokedCallback(dispatcher)
            } else {
                unregisterOnBackInvokedCallback(dispatcher)
            }
        }
    }

    private fun updateChildrenImportantForAccessibility() {
        val drawer = findResponsiveDrawerLayout()
        if (!isModalOpenCase(drawer)) {
            for (i in 0 until childCount) {
                getChildAt(i).importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO
            }
            return
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child == drawer) {
                child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        }
    }

    override fun addFocusables(views: ArrayList<View>, direction: Int, focusableMode: Int) {
        val drawer = findResponsiveDrawerLayout()
        if (drawer == null || !isModalOpenCase(drawer)) {
            super.addFocusables(views, direction, focusableMode)
        } else {
            if (descendantFocusability == FOCUS_BLOCK_DESCENDANTS) {
                return
            }
            drawer.addFocusables(views, direction, focusableMode)
        }
    }

    fun addResponsivePaneListener(listener: ResponsivePaneListener) {
        debug("addResponsivePaneListener listener=$listener, listeners=$responsivePaneCallbackNotifier")
        if (!responsivePaneCallbackNotifier.contains(listener)) {
            responsivePaneCallbackNotifier.add(listener)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is ResponsiveDrawerLayout) {
            controller.cleanup()
            val impl = controller as? ResponsivePaneControllerImpl
            impl?.getElevationConcept()?.applyConcept(child, if (isOpen) 1.0f else 0.0f)
        }
        super.addView(child, index, params)
    }

    override fun close() {
        close(true)
    }

    override fun computeScroll() {
        controller.computeScroll(this)
    }

    fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
        return controller.getPaneConfig(paneState)
    }

    override fun isOpen(): Boolean = getPaneState() == PaneState.OPENED

    fun isOutsideTouchEnabled(): Boolean = controller.isOutsideTouchEnabled

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateChildrenImportantForAccessibility()
        updateBackInvokedCallbackState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller.onConfigurationChanged(this, newConfig)
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= 33) {
            findOnBackInvokedDispatcher()?.let { unregisterOnBackInvokedCallback(it) }
        }
        controller.cleanup()
        super.onDetachedFromWindow()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val result = controller.onInterceptTouchEvent(this, event)
        return result ?: super.onInterceptTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        controller.onLayout(this, changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (isFirstLayout) {
            controller.updateResponsiveLayout(this)
            isFirstLayout = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = controller.onTouchEvent(this, event)
        return result ?: super.onTouchEvent(event)
    }

    override fun open() {
        open(true)
    }

    fun removeResponsivePaneListener(listener: ResponsivePaneListener) {
        debug("removeResponsivePaneListener listener=$listener, listeners=$responsivePaneCallbackNotifier")
        responsivePaneCallbackNotifier.remove(listener)
    }

    fun setOutsideTouchEnabled(enabled: Boolean) {
        debug("setOutsideTouchEnabled enabled=$enabled")
        controller.setOutsideTouchEnabled(enabled)
        updateBackInvokedCallbackState()
        updateChildrenImportantForAccessibility()
    }

    fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig) {
        controller.setPaneConfig(this, paneState, responsiveConfig)
    }

    fun updateResponsiveLayout() {
        debug("updateResponsiveLayout")
        controller.updateResponsiveLayout(this)
    }

    fun close(animate: Boolean) {
        debug("close animate=$animate")
        setPaneState(PaneState.CLOSED, animate)
    }

    fun open(animate: Boolean) {
        debug("open animate=$animate")
        setPaneState(PaneState.OPENED, animate)
    }
}
