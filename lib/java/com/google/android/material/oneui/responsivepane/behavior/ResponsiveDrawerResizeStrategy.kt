package com.google.android.material.oneui.responsivepane.behavior

import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationDefaultAttributeMap
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.warn
import com.google.android.material.oneui.responsivepane.ResponsiveDrawerLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import com.google.android.material.oneui.responsivepane.model.ResponsiveInitConfig
import java.util.LinkedHashMap
import kotlin.math.roundToInt

class ResponsiveDrawerResizeStrategy : ResponsivePaneBehaviorStrategy, ResponsivePaneTag {
    private var cachedContentLayout: ViewGroup? = null
    private var cachedDrawerLayout: ViewGroup? = null
    private val initCloseConfig: ResponsiveInitConfig
    private val initOpenConfig: ResponsiveInitConfig
    override val logTag: String = "ResponsiveDrawerResizeStrategy"
    private var overMaxWidthSnapAnimation: SpringAnimation? = null
    private var overScaledSize: Float = 0.0f
    private var overSize: Int = 0
    private var widthAnimation: SpringAnimation? = null
    var behaviorPaneState: PaneState = PaneState.OPENED
        private set
    private val configMap: MutableMap<PaneState, ResponsiveConfig> = LinkedHashMap()

    init {
        val tierOpen = SeslElevationTier.ELEVATION_LG
        val iOpen = R.dimen.sesl_responsive_pane_navigation_drawer_width
        val attrMap = SeslElevationDefaultAttributeMap
        initOpenConfig = ResponsiveInitConfig(iOpen, tierOpen.elevationDimenRes, attrMap.getBlurPreset(tierOpen), attrMap.getNonBlurBackgroundAlphaRes(tierOpen))
        val tierClose = SeslElevationTier.ELEVATION_ZERO
        initCloseConfig = ResponsiveInitConfig(R.dimen.sesl_responsive_pane_navigation_drawer_width_closed, tierClose.elevationDimenRes, attrMap.getBlurPreset(tierClose), attrMap.getNonBlurBackgroundAlphaRes(tierClose))
    }

    private fun computeDraggedDrawerWidth(width: Int, delta: Int, closedSize: Int, openedSize: Int): Int {
        val i = width - overScaledSize.toInt()
        val i5 = overSize
        if (i + i5 + delta < openedSize) {
            overSize = 0
            overScaledSize = 0.0f
            return (width + delta).coerceAtLeast(closedSize)
        }
        val i6 = (delta - if (i5 == 0) openedSize - width else 0) + i5
        overSize = i6
        val f6 = i6 * 0.02f
        overScaledSize = f6
        return openedSize + f6.toInt()
    }

    private fun getDefaultDrawerPaneWidth(state: PaneState): Int {
        val responsiveConfig = configMap[state]
        if (responsiveConfig != null) {
            return responsiveConfig.width
        }
        warn("getDefaultDrawerPaneWidth[$state] : The Config is not complete yet")
        return 0
    }

    override fun animateOverWidthToOpened(
        layout: ResponsivePaneLayout,
        callback: ResponsivePaneSlideOffsetCallback?,
        onAnimationEnd: () -> Unit
    ) {
        debug("animateOverWidthToOpened layout=$layout")
        overSize = 0
        overScaledSize = 0.0f
        val drawer = getDrawerLayout(layout) as? ResponsiveDrawerLayout ?: run {
            onAnimationEnd()
            return
        }
        val openWidth = getDrawerPaneWidth(layout, PaneState.OPENED)
        val closedWidth = getDrawerPaneWidth(layout, PaneState.CLOSED)
        if (drawer.width <= openWidth) {
            onAnimationEnd()
            return
        }
        drawer.maxWidth = openWidth
        widthAnimation?.cancel()
        overMaxWidthSnapAnimation?.cancel()

        val prop = object : FloatPropertyCompat<ResponsiveDrawerLayout>("width") {
            override fun getValue(newValue: ResponsiveDrawerLayout): Float = newValue.layoutParams.width.toFloat()
            override fun setValue(newValue: ResponsiveDrawerLayout, value: Float) {
                val roundW = value.roundToInt()
                val lp = newValue.layoutParams
                lp.width = roundW
                newValue.layoutParams = lp
                val range = (openWidth - closedWidth).toFloat()
                val offset = if (range > 0.0f) ((roundW - closedWidth) / range).coerceIn(0.0f, 1.0f) else 1.0f
                callback?.invoke(newValue, offset)
            }
        }
        val anim = SpringAnimation(drawer, prop)
        anim.spring = SpringForce(drawer.layoutParams.width.toFloat()).apply {
            dampingRatio = 0.8f
            stiffness = 255.0f
        }
        anim.addEndListener { _, _, _, _ ->
            debug("animateOverWidthToOpened end")
            behaviorPaneState = PaneState.OPENED
            overMaxWidthSnapAnimation = null
            onAnimationEnd()
        }
        overMaxWidthSnapAnimation = anim
        anim.animateToFinalPosition(openWidth.toFloat())
    }

    override fun animateStateTransition(
        layout: ResponsivePaneLayout,
        toState: PaneState,
        skipAnimation: Boolean,
        callback: ResponsivePaneSlideOffsetCallback?,
        onAnimationEnd: () -> Unit
    ) {
        debug("animateStateTransition layout=$layout, to=$toState, skipAnimation=$skipAnimation")
        val drawer = getDrawerLayout(layout) as? ResponsiveDrawerLayout ?: run {
            debug("animateStateTransition drawer is not find")
            behaviorPaneState = toState
            return
        }
        val openWidth = getDrawerPaneWidth(layout, PaneState.OPENED)
        val closedWidth = getDrawerPaneWidth(layout, PaneState.CLOSED)
        val targetWidth = getDrawerPaneWidth(layout, toState)
        drawer.maxWidth = openWidth

        widthAnimation?.cancel()
        overMaxWidthSnapAnimation?.cancel()

        val prop = object : FloatPropertyCompat<ResponsiveDrawerLayout>("width") {
            override fun getValue(newValue: ResponsiveDrawerLayout): Float = newValue.layoutParams.width.toFloat()
            override fun setValue(newValue: ResponsiveDrawerLayout, value: Float) {
                val roundW = value.roundToInt()
                val lp = newValue.layoutParams
                lp.width = roundW
                newValue.layoutParams = lp
                val offset = ((roundW - closedWidth).toFloat() / (openWidth - closedWidth)).coerceIn(0.0f, 1.0f)
                callback?.invoke(newValue, offset)
            }
        }
        val anim = SpringAnimation(drawer, prop)
        anim.spring = SpringForce(drawer.layoutParams.width.toFloat()).apply {
            dampingRatio = 1.0f
            stiffness = 361.0f
        }
        anim.addEndListener { _, _, _, _ ->
            debug("animate end state=$toState")
            behaviorPaneState = toState
            onAnimationEnd()
        }
        widthAnimation = anim
        anim.animateToFinalPosition(targetWidth.toFloat())
        if (skipAnimation) {
            anim.skipToEnd()
        }
    }

    override fun canOutSideTouch(): Boolean = false

    override fun cleanup() {
        debug("cleanup")
        widthAnimation?.cancel()
        overMaxWidthSnapAnimation?.cancel()
        widthAnimation = null
        overMaxWidthSnapAnimation = null
        cachedDrawerLayout = null
        cachedContentLayout = null
    }

    fun getContentLayout(layout: ViewGroup): ViewGroup? {
        cachedContentLayout?.let { return it }
        val view = layout.findViewById<ViewGroup>(R.id.responsive_pane_content) ?: return null
        cachedContentLayout = view
        return view
    }

    override fun getDragRange(layout: ConstraintLayout): Int {
        return getDrawerPaneWidth(layout, PaneState.CLOSED) - getDrawerPaneWidth(layout, PaneState.OPENED)
    }

    override fun getDrawerLayout(layout: ViewGroup): ViewGroup? {
        cachedDrawerLayout?.let { return it }
        val view = layout.findViewById<ViewGroup>(R.id.responsive_pane_drawer) ?: return null
        cachedDrawerLayout = view
        return view
    }

    override fun getDrawerPaneWidth(layout: ConstraintLayout, state: PaneState): Int {
        val drawer = getDrawerLayout(layout)
        val lp = drawer?.layoutParams as? ConstraintLayout.LayoutParams
        val matchConstraint = if (state == PaneState.OPENED) lp?.matchConstraintMaxWidth ?: 0 else lp?.matchConstraintMinWidth ?: 0
        if (matchConstraint > 0) {
            return matchConstraint
        }
        val defaultWidth = getDefaultDrawerPaneWidth(state)
        if (lp != null) {
            lp.matchConstraintMaxWidth = getDefaultDrawerPaneWidth(PaneState.OPENED)
            lp.matchConstraintMinWidth = getDefaultDrawerPaneWidth(PaneState.CLOSED)
            drawer.layoutParams = lp
        }
        return defaultWidth
    }

    override fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
        val config = configMap[paneState]
        if (config != null) return config
        warn("getPaneConfig[$paneState] : The Config is not complete yet")
        return ResponsiveConfig()
    }

    override fun getPaneState(): PaneState = behaviorPaneState

    override fun initPaneConfig(context: Context) {
        setPaneConfig(PaneState.OPENED, initOpenConfig.generate(context))
        setPaneConfig(PaneState.CLOSED, initCloseConfig.generate(context))
    }

    override fun onPaneDragged(
        parent: ResponsivePaneLayout,
        offset: Float,
        callback: ResponsivePaneSlideOffsetCallback?
    ) {
        val drawer = getDrawerLayout(parent) as? ResponsiveDrawerLayout ?: return
        val openWidth = getDrawerPaneWidth(parent, PaneState.OPENED)
        val closedWidth = getDrawerPaneWidth(parent, PaneState.CLOSED)
        val draggedWidth = computeDraggedDrawerWidth(drawer.width, offset.roundToInt(), closedWidth, openWidth)
        val lp = drawer.layoutParams
        if (lp != null) {
            lp.width = draggedWidth
            val range = (openWidth - closedWidth).toFloat()
            val slideOffset = if (range > 0.0f && draggedWidth < openWidth) {
                ((draggedWidth - closedWidth) / range).coerceIn(0.0f, 1.0f)
            } else 1.0f
            callback?.invoke(drawer, slideOffset)
        }
        drawer.layoutParams = lp
    }

    override fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig) {
        configMap[paneState] = responsiveConfig
    }

    override fun shouldAllowDrag(): Boolean = true

    override fun updateConstraintConnect(parent: ConstraintLayout) {
        val drawerLayout = getDrawerLayout(parent)
        debug("updateConstraintConnect drawerLayout=$drawerLayout")
        val cs = ConstraintSet()
        cs.clone(parent)
        val parentId = parent.id
        if (drawerLayout != null) {
            val drawerId = drawerLayout.id
            val lp = drawerLayout.layoutParams as ConstraintLayout.LayoutParams
            val drawerWidth = getDrawerPaneWidth(parent, behaviorPaneState)
            (drawerLayout as? ResponsiveDrawerLayout)?.maxWidth = getDrawerPaneWidth(parent, PaneState.OPENED)
            cs.connect(drawerId, 6, parentId, 6)
            cs.connect(drawerId, 3, parentId, 3)
            cs.connect(drawerId, 4, parentId, 4)
            cs.clear(drawerId, 7)
            cs.constrainWidth(drawerId, drawerWidth)
            cs.setMargin(drawerId, 6, lp.marginStart)
            cs.setMargin(drawerId, 3, lp.topMargin)
            cs.setMargin(drawerId, 7, lp.marginEnd)
            cs.setMargin(drawerId, 4, lp.bottomMargin)
            cs.setVisibility(drawerId, drawerLayout.visibility)
        }
        cs.applyTo(parent)
    }
}
