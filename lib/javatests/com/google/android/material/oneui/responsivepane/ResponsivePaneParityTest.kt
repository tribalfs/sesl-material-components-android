package com.google.android.material.oneui.responsivepane

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.semblurinfo.ColorCurvePreset
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.SemBlurCompat
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationDefaultAttributeMap
import com.google.android.material.oneui.common.helper.concept.elevation.SeslElevationTier
import com.google.android.material.oneui.responsivepane.behavior.ResponsiveDrawerResizeStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneBehaviorStrategy
import com.google.android.material.oneui.responsivepane.behavior.ResponsivePaneSlideOffsetCallback
import com.google.android.material.oneui.responsivepane.controller.ResponsivePaneControllerImpl
import com.google.android.material.oneui.responsivepane.helper.ResponsivePanePresetConfigs
import com.google.android.material.oneui.responsivepane.helper.concept.ElevationConcept
import com.google.android.material.oneui.responsivepane.helper.concept.ElevationConceptRange
import com.google.android.material.oneui.responsivepane.helper.concept.ResponsivePaneElevationConcept
import com.google.android.material.oneui.responsivepane.model.PaneState
import com.google.android.material.oneui.responsivepane.model.ResponsiveConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ResponsivePaneParityTest {
    private lateinit var context: Context
    private lateinit var layout: ResponsivePaneLayout
    private lateinit var drawer: ResponsiveDrawerLayout
    private lateinit var controller: ResponsivePaneControllerImpl
    private lateinit var strategy: RecordingStrategy

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.applicationInfo.flags = context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        layout = ResponsivePaneLayout(context).apply { id = View.generateViewId() }
        drawer = ResponsiveDrawerLayout(context).apply { id = R.id.responsive_pane_drawer }
        layout.addView(drawer, ConstraintLayout.LayoutParams(200, 400))
        layout.layout(0, 0, 1000, 400)
        drawer.layout(0, 0, 200, 400)
        controller = layout.controller as ResponsivePaneControllerImpl
        strategy = RecordingStrategy(drawer)
        controller.currentBehaviorStrategy = strategy
    }

    @Test
    fun outsideDownClosesModalDrawerAndIntercepts() {
        assertEquals(true, intercept(MotionEvent.ACTION_DOWN, 500f, 100f))
        assertEquals(PaneState.CLOSED, strategy.state)
        assertTrue(strategy.animated)
    }

    @Test
    fun outsideDownDoesNotCloseWhenOutsideTouchEnabled() {
        layout.setOutsideTouchEnabled(true)
        assertEquals(false, intercept(MotionEvent.ACTION_DOWN, 500f, 100f))
        assertEquals(PaneState.OPENED, strategy.state)
    }

    @Test
    fun horizontalDrawerSwipeInterceptsButVerticalSwipeDoesNot() {
        assertEquals(false, intercept(MotionEvent.ACTION_DOWN, 100f, 100f))
        assertTrue(controller.isDrawerTouchDown)
        assertEquals(false, intercept(MotionEvent.ACTION_MOVE, 110f, 180f))
        assertEquals(true, intercept(MotionEvent.ACTION_MOVE, 180f, 110f))
        assertEquals(false, intercept(MotionEvent.ACTION_CANCEL, 180f, 110f))
    }

    @Test
    fun missingOrHiddenDrawerDoesNotIntercept() {
        drawer.visibility = View.GONE
        assertEquals(false, intercept(MotionEvent.ACTION_DOWN, 500f, 100f))
        strategy.drawer = null
        assertEquals(false, intercept(MotionEvent.ACTION_DOWN, 500f, 100f))
        assertEquals(PaneState.OPENED, strategy.state)
    }

    @Test
    fun disabledDraggingFallsBackToParent() {
        strategy.allowDrag = false
        assertNull(intercept(MotionEvent.ACTION_DOWN, 100f, 100f))
    }

    @Test
    fun draggingUsesSuccessiveMovementDeltasInBothDirections() {
        touch(MotionEvent.ACTION_DOWN, 100f)
        touch(MotionEvent.ACTION_MOVE, 115f)
        touch(MotionEvent.ACTION_MOVE, 110f)
        assertEquals(listOf(15f, -5f), strategy.offsets)
        strategy.offsets.clear()
        layout.layoutDirection = View.LAYOUT_DIRECTION_RTL
        touch(MotionEvent.ACTION_DOWN, 100f)
        touch(MotionEvent.ACTION_MOVE, 115f)
        touch(MotionEvent.ACTION_MOVE, 110f)
        assertEquals(listOf(-15f, 5f), strategy.offsets)
    }

    @Test
    fun currentStateTracksBehaviorAndMissingDrawerCallbackIsNullable() {
        strategy.state = PaneState.CLOSED
        assertEquals(PaneState.CLOSED, controller.currentState)
        strategy.drawer = null
        var notified = false
        controller.onStateChangedListener = { view, state ->
            assertNull(view)
            assertEquals(PaneState.OPENED, state)
            notified = true
        }
        controller.setPaneState(layout, PaneState.OPENED, false)
        assertTrue(notified)
        assertEquals(PaneState.OPENED, controller.currentState)
    }

    @Test
    @Config(qualifiers = "xhdpi")
    fun presetWidthsConvertDpToPixels() {
        assertEquals(2f, Resources.getSystem().displayMetrics.density, 0f)
        assertEquals(600, ResponsivePanePresetConfigs.defaultWidthPx(PaneState.OPENED))
        assertEquals(144, ResponsivePanePresetConfigs.defaultWidthPx(PaneState.CLOSED))
    }

    @Test
    fun blurKeepsBackgroundOpaqueAndZeroRadiusRemovesElevation() {
        val view = TestBlurView(context)
        val background = GradientDrawable().apply { setColor(0xff112233.toInt()); alpha = 80 }
        view.background = background
        elevationConcept(10).applyConcept(view, 1f)
        assertEquals(255, view.background.alpha)
        assertEquals(8f, view.elevation, 0.001f)
        assertEquals(10, view.curve?.blurRadius)
        elevationConcept(0).applyConcept(view, 1f)
        assertEquals(255, view.background.alpha)
        assertEquals(0f, view.elevation, 0f)
    }

    @Test
    fun nonBlurBackgroundInterpolatesAlpha() {
        val view = View(context).apply { background = GradientDrawable() }
        elevationConcept(10).applyConcept(view, 0.5f)
        assertEquals(128, view.background.alpha)
        assertTrue(view.elevation > 0f)
    }

    @Test
    fun elevationTiersResolveTheirNonBlurAlphaResources() {
        val expected = listOf(0f, 0.88f, 0.92f, 0.96f, 0.98f)
        SeslElevationTier.entries.forEachIndexed { index, tier ->
            assertEquals(expected[index], SeslElevationDefaultAttributeMap.getNonBlurBackgroundAlpha(context, tier), 0.001f)
        }
    }

    @Test
    fun drawerInitializationAndPresetsUseTheSameTierAlpha() {
        val resize = ResponsiveDrawerResizeStrategy()
        resize.initPaneConfig(context)
        assertEquals(0.96f, resize.getPaneConfig(PaneState.OPENED).nonBlurBGAlpha, 0.001f)
        assertEquals(0f, resize.getPaneConfig(PaneState.CLOSED).nonBlurBGAlpha, 0.001f)
        assertEquals(0.96f, ResponsivePanePresetConfigs.paneConfigForTier(
            context, SeslElevationTier.ELEVATION_LG, 300
        ).nonBlurBGAlpha, 0.001f)
    }

    @Test
    fun elevationConversionIsAvailableUnderSeslJavaClassName() {
        val facade = Class.forName(
            "com.google.android.material.oneui.responsivepane.helper.concept.ResponsivePaneElevationConceptKt"
        )
        val config = ResponsiveConfig()
        val concept = facade.getMethod("toElevationTierConcept", ResponsiveConfig::class.java)
            .invoke(null, config) as ElevationConcept
        assertEquals(config.elevation, concept.elevationDimen, 0f)
        assertEquals(config.blur, concept.blurPreset)
        assertEquals(config.nonBlurBGAlpha, concept.nonBlurBackgroundAlpha, 0f)
    }

    private fun elevationConcept(radius: Int): ResponsivePaneElevationConcept {
        val blur = ColorCurvePreset(SemBlurCompat.CurveParameter(radius, 0f, 0f, 0f, 0f, 0f, 0f))
        return ResponsivePaneElevationConcept(ElevationConceptRange(
            ElevationConcept(0f, blur, 0.8f), ElevationConcept(8f, blur, 0.2f)
        ))
    }

    private fun intercept(action: Int, x: Float, y: Float): Boolean? {
        val event = MotionEvent.obtain(0, 16, action, x, y, 0)
        return try { controller.onInterceptTouchEvent(layout, event) } finally { event.recycle() }
    }

    private fun touch(action: Int, x: Float) {
        val event = MotionEvent.obtain(0, 16, action, x, 100f, 0)
        try { controller.onTouchEvent(layout, event) } finally { event.recycle() }
    }

    private class RecordingStrategy(var drawer: ViewGroup?) :
        ResponsivePaneBehaviorStrategy by ResponsiveDrawerResizeStrategy() {
        var state = PaneState.OPENED
        var animated = false
        var allowDrag = true
        val offsets = mutableListOf<Float>()
        override fun getDrawerLayout(layout: ViewGroup) = drawer
        override fun getPaneState() = state
        override fun shouldAllowDrag() = allowDrag
        override fun onPaneDragged(parent: ResponsivePaneLayout, offset: Float, callback: ResponsivePaneSlideOffsetCallback?) {
            offsets.add(offset)
        }
        override fun animateStateTransition(layout: ResponsivePaneLayout, toState: PaneState,
            skipAnimation: Boolean, callback: ResponsivePaneSlideOffsetCallback?, onAnimationEnd: () -> Unit) {
            state = toState
            animated = !skipAnimation
            onAnimationEnd()
        }
    }

    private class TestBlurView(context: Context) : View(context), BlurSupportable {
        var curve: SemBlurCompat.CurveParameter? = null
        override fun isBlurApplied() = true
        override fun applyBlurInfo(context: Context) = true
        override fun applyBlurInfo(curveParameter: SemBlurCompat.CurveParameter): Boolean {
            curve = curveParameter
            return true
        }
        override fun clearBlurInfo(context: Context) = Unit
        override fun setBlurMode(semBlurInfoMode: Int) = Unit
    }
}
