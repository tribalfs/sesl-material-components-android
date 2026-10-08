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
import com.google.android.material.oneui.responsivepane.ResponsivePanePolicy.OVER_MAX_WIDTH_DRAG_SCALE
import com.google.android.material.oneui.responsivepane.ResponsivePanePolicy.SNAP_OVER_MAX_WIDTH_SPRING_DAMPING_RATIO
import com.google.android.material.oneui.responsivepane.ResponsivePanePolicy.SNAP_OVER_MAX_WIDTH_SPRING_STIFFNESS
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import com.google.android.material.oneui.responsivepane.model.ResponsiveInitConfig
import java.util.LinkedHashMap
import kotlin.math.roundToInt

/**
 * Resizes a drawer identified by `responsive_pane_drawer` using spring animations.
 *
 * Uses resource-backed opened and closed configurations and permits drag resizing. Child lookups
 * are cached until [cleanup]. Call [initPaneConfig] before using preset widths.
 */
open class ResponsiveDrawerResizeStrategy : ResponsivePaneBehaviorStrategy, ResponsivePaneTag {
	private var cachedContentLayout: ViewGroup? = null
	private var cachedDrawerLayout: ViewGroup? = null
	private val initCloseConfig: ResponsiveInitConfig
	private val initOpenConfig: ResponsiveInitConfig
	/**
	 * Tag used for resize strategy log messages.
	 */
	override val logTag: String = "ResponsiveDrawerResizeStrategy"
	private var overMaxWidthSnapAnimation: SpringAnimation? = null
	private var overScaledSize: Float = 0.0f
	private var overSize: Int = 0
	private var widthAnimation: SpringAnimation? = null
	/**
	 * State maintained by width transitions, initially [PaneState.OPENED].
	 *
	 * Updated at animation end, including cancellation; direct dragging does not settle the state.
	 */
	private var behaviorPaneState: PaneState = PaneState.OPENED

	private val configMap: MutableMap<PaneState, ResponsiveConfig> = LinkedHashMap()

	init {
		val tierOpen = SeslElevationTier.ELEVATION_LG
		val iOpen = R.dimen.sesl_responsive_pane_navigation_drawer_width
		val attrMap = SeslElevationDefaultAttributeMap
		initOpenConfig = ResponsiveInitConfig(
			iOpen,
			tierOpen.elevationDimenRes,
			attrMap.getBlurPreset(tierOpen),
			attrMap.getNonBlurBackgroundAlphaRes(tierOpen)
		)
		val tierClose = SeslElevationTier.ELEVATION_ZERO
		initCloseConfig = ResponsiveInitConfig(
			R.dimen.sesl_responsive_pane_navigation_drawer_width_closed,
			tierClose.elevationDimenRes,
			attrMap.getBlurPreset(tierClose),
			attrMap.getNonBlurBackgroundAlphaRes(tierClose)
		)
	}

	private fun computeDraggedDrawerWidth(
		width: Int,
		delta: Int,
		closedSize: Int,
		openedSize: Int
	): Int {
		val baseWidth = width - overScaledSize.toInt()
		val currentOverSize = overSize
		if (baseWidth + currentOverSize + delta < openedSize) {
			overSize = 0
			overScaledSize = 0.0f
			return (width + delta).coerceAtLeast(closedSize)
		}
		val newOverSize = (delta - if (currentOverSize == 0) openedSize - width else 0) + currentOverSize
		overSize = newOverSize
		val scaledOverSize = newOverSize * OVER_MAX_WIDTH_DRAG_SCALE
		overScaledSize = scaledOverSize
		return openedSize + scaledOverSize.toInt()
	}

	private fun getDefaultDrawerPaneWidth(state: PaneState): Int {
		val responsiveConfig = configMap[state]
		if (responsiveConfig != null) {
			return responsiveConfig.width
		}
		warn("getDefaultDrawerPaneWidth[$state] : The Config is not complete yet")
		return 0
	}

	/**
	 * Snaps an oversize drawer in [layout] back to its opened width.
	 *
	 * @param callback optional listener for width progress.
	 * @param onAnimationEnd completion callback; implementations may also invoke it on cancellation.
	 */
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
			override fun getValue(newValue: ResponsiveDrawerLayout): Float =
				newValue.layoutParams.width.toFloat()

			override fun setValue(newValue: ResponsiveDrawerLayout, value: Float) {
				val roundW = value.roundToInt()
				val lp = newValue.layoutParams
				lp.width = roundW
				newValue.layoutParams = lp
				val range = (openWidth - closedWidth).toFloat()
				val offset =
					if (range > 0.0f) ((roundW - closedWidth) / range).coerceIn(0.0f, 1.0f) else 1.0f
				callback?.invoke(newValue, offset)
			}
		}
		val anim = SpringAnimation(drawer, prop)
		anim.spring = SpringForce(drawer.layoutParams.width.toFloat()).apply {
			dampingRatio = SNAP_OVER_MAX_WIDTH_SPRING_DAMPING_RATIO
			stiffness = SNAP_OVER_MAX_WIDTH_SPRING_STIFFNESS
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

	/**
	 * Moves the drawer in [layout] to [toState].
	 *
	 * @param skipAnimation whether to move immediately to the final width.
	 * @param callback optional listener for width progress.
	 * @param onAnimationEnd transition-end callback; implementations may also invoke it on cancellation.
	 * The resize strategy does not invoke it if no drawer is available.
	 */
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
			override fun getValue(newValue: ResponsiveDrawerLayout): Float =
				newValue.layoutParams.width.toFloat()

			override fun setValue(newValue: ResponsiveDrawerLayout, value: Float) {
				val roundW = value.roundToInt()
				val lp = newValue.layoutParams
				lp.width = roundW
				newValue.layoutParams = lp
				val offset =
					((roundW - closedWidth).toFloat() / (openWidth - closedWidth)).coerceIn(0.0f, 1.0f)
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

	/**
	 * Returns this strategy's default policy for interaction outside the drawer.
	 */
	override fun canOutSideTouch(): Boolean = false

	/**
	 * Cancels animations and clears cached child views, retaining configurations.
	 */
	override fun cleanup() {
		debug("cleanup")
		widthAnimation?.cancel()
		overMaxWidthSnapAnimation?.cancel()
		widthAnimation = null
		overMaxWidthSnapAnimation = null
		cachedDrawerLayout = null
		cachedContentLayout = null
	}

	/**
	 * Returns and caches the container with ID `responsive_pane_content` inside [layout].
	 *
	 * @return null if the content container is absent. Call [cleanup] after replacing children.
	 */
	fun getContentLayout(layout: ViewGroup): ViewGroup? {
		cachedContentLayout?.let { return it }
		val view = layout.findViewById<ViewGroup>(R.id.responsive_pane_content) ?: return null
		cachedContentLayout = view
		return view
	}

	/**
	 * Returns the signed width range for [layout].
	 *
	 * The resize strategy returns closed width minus opened width.
	 */
	override fun getDragRange(layout: ConstraintLayout): Int {
		return getDrawerPaneWidth(layout, PaneState.CLOSED) - getDrawerPaneWidth(
			layout,
			PaneState.OPENED
		)
	}

	/**
	 * Returns the drawer managed within [layout], or null when none is found.
	 */
	override fun getDrawerLayout(layout: ViewGroup): ViewGroup? {
		cachedDrawerLayout?.let { return it }
		val view = layout.findViewById<ViewGroup>(R.id.responsive_pane_drawer) ?: return null
		cachedDrawerLayout = view
		return view
	}

	/**
	 * Returns the target drawer width in pixels for [state] within [layout].
	 *
	 * The resize strategy prefers positive match-constraint width limits over stored presets.
	 */
	override fun getDrawerPaneWidth(layout: ConstraintLayout, state: PaneState): Int {
		val drawer = getDrawerLayout(layout)
		val lp = drawer?.layoutParams as? ConstraintLayout.LayoutParams
		val matchConstraint = if (state == PaneState.OPENED) lp?.matchConstraintMaxWidth
			?: 0 else lp?.matchConstraintMinWidth ?: 0
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

	/**
	 * Returns the configuration for [paneState], or an unresolved default if not initialized.
	 */
	override fun getPaneConfig(paneState: PaneState): ResponsiveConfig {
		val config = configMap[paneState]
		if (config != null) return config
		warn("getPaneConfig[$paneState] : The Config is not complete yet")
		return ResponsiveConfig()
	}

	/**
	 * Returns the settled pane state maintained by this strategy.
	 */
	override fun getPaneState(): PaneState = behaviorPaneState

	/**
	 * Initializes state configurations from resources resolved using [context].
	 */
	override fun initPaneConfig(context: Context) {
		setPaneConfig(PaneState.OPENED, initOpenConfig.generate(context))
		setPaneConfig(PaneState.CLOSED, initCloseConfig.generate(context))
	}

	/**
	 * Resizes the drawer within [parent] by a drag delta.
	 *
	 * @param offset delta in pixels, with positive values widening the drawer; callers handle RTL.
	 * @param callback optional listener for resulting slide progress.
	 */
	override fun onPaneDragged(
		parent: ResponsivePaneLayout,
		offset: Float,
		callback: ResponsivePaneSlideOffsetCallback?
	) {
		val drawer = getDrawerLayout(parent) as? ResponsiveDrawerLayout ?: return
		val openWidth = getDrawerPaneWidth(parent, PaneState.OPENED)
		val closedWidth = getDrawerPaneWidth(parent, PaneState.CLOSED)
		val draggedWidth =
			computeDraggedDrawerWidth(drawer.width, offset.roundToInt(), closedWidth, openWidth)
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

	/**
	 * Stores [responsiveConfig] for [paneState] without directly requesting a layout.
	 */
	override fun setPaneConfig(paneState: PaneState, responsiveConfig: ResponsiveConfig) {
		configMap[paneState] = responsiveConfig
	}

	/**
	 * Returns whether gesture-driven resizing is allowed.
	 */
	override fun shouldAllowDrag(): Boolean = true

	/**
	 * Applies responsive drawer constraints to [parent] for the current state.
	 */
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
			(drawerLayout as? ResponsiveDrawerLayout)?.maxWidth =
				getDrawerPaneWidth(parent, PaneState.OPENED)
			cs.connect(drawerId, ConstraintSet.START, parentId, ConstraintSet.START)
			cs.connect(drawerId, ConstraintSet.TOP, parentId, ConstraintSet.TOP)
			cs.connect(drawerId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM)
			cs.clear(drawerId, ConstraintSet.END)
			cs.constrainWidth(drawerId, drawerWidth)
			cs.setMargin(drawerId, ConstraintSet.START, lp.marginStart)
			cs.setMargin(drawerId, ConstraintSet.TOP, lp.topMargin)
			cs.setMargin(drawerId, ConstraintSet.END, lp.marginEnd)
			cs.setMargin(drawerId, ConstraintSet.BOTTOM, lp.bottomMargin)
			cs.setVisibility(drawerId, drawerLayout.visibility)
		}
		cs.applyTo(parent)
	}
}
