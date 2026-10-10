package com.google.android.material.oneui.floatingactioncontainer

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.oneui.floatingactioncontainer.FloatingAware.PositionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class FloatingActionContainerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>().apply {
            setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar)
        }
        ShadowLog.clear()
    }

    @Test
    fun addingContentKeepsProjectionViewAtIndexZero() {
        val layout = FloatingGroupLayout(context)
        val firstContent = View(context)
        val secondContent = View(context)

        layout.addView(firstContent)
        layout.addView(secondContent)

        assertEquals(3, layout.childCount)
        assertSame(layout.projectionView, layout.getChildAt(0))
        assertSame(firstContent, layout.getChildAt(1))
        assertSame(secondContent, layout.getChildAt(2))
    }

    @Test
    fun defaultFloatingAwareReferencesPrimaryContent() {
        val layout = createLayout()

        val firstAware = layout.floatingAware
        val secondAware = layout.floatingAware

        assertNotNull(firstAware)
        assertNotSame(firstAware, secondAware)
        assertSame(layout.getChildAt(1), firstAware?.getReferenceView(PositionType.START_FIRST))
        assertNull(firstAware?.getReferenceView(PositionType.START_SECOND))
        assertNull(firstAware?.getReferenceView(PositionType.END_FIRST))
    }

    @Test
    fun settingNullFloatingAwareStoresInertAware() {
        val layout = createLayout()
        assertNotNull(layout.floatingAware?.getReferenceView(PositionType.START_FIRST))

        layout.floatingAware = null

        val inertAware = layout.floatingAware
        assertNotNull(inertAware)
        assertSame(inertAware, layout.floatingAware)
        PositionType.entries.forEach { type ->
            assertNull(inertAware?.getReferenceView(type))
            assertNull(inertAware?.getReferenceViews(type))
        }
    }

    @Test
    fun settingCustomFloatingAwareSynchronizesVisibleAppBarState() {
        val layout = AppBarBackedFloatingGroupLayout(context)
        layout.addView(View(context))
        val aware = RecordingAware()

        layout.floatingAware = aware

        assertEquals(0, aware.showCallbacks)
        assertEquals(1, aware.hideCallbacks)
    }

    @Test
    fun settingCustomFloatingAwareSynchronizesHiddenAppBarState() {
        val layout = AppBarBackedFloatingGroupLayout(
            context,
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK,
        )
        layout.addView(View(context))
        val aware = RecordingAware()

        layout.floatingAware = aware

        assertEquals(1, aware.showCallbacks)
        assertEquals(0, aware.hideCallbacks)
    }

    @Test
    fun visibleReferenceViewsFiltersGoneGroupMembers() {
        val layout = createLayout()
        val visible = View(context)
        val gone = View(context).apply { visibility = View.GONE }

        val filtered = layout.projectionView.getVisibleReferenceViews(
            listOf(visible, gone),
            null,
            skipVisibleCheck = false,
        )
        val unfiltered = layout.projectionView.getVisibleReferenceViews(
            listOf(visible, gone),
            null,
            skipVisibleCheck = true,
        )

        assertEquals(listOf(visible), filtered)
        assertEquals(listOf(visible, gone), unfiltered)
    }

    @Test
    fun visibleReferenceViewsFallsBackToSingleVisibleReference() {
        val layout = createLayout()
        val reference = View(context)

        assertEquals(
            listOf(reference),
            layout.projectionView.getVisibleReferenceViews(null, reference, skipVisibleCheck = false),
        )

        reference.visibility = View.INVISIBLE

        assertNull(
            layout.projectionView.getVisibleReferenceViews(null, reference, skipVisibleCheck = false),
        )
        assertEquals(
            listOf(reference),
            layout.projectionView.getVisibleReferenceViews(null, reference, skipVisibleCheck = true),
        )
    }

    @Test
    fun visibleReferenceViewsPrefersGroupOverSingleReference() {
        val layout = createLayout()
        val groupReference = View(context)
        val singleReference = View(context)

        val result = layout.projectionView.getVisibleReferenceViews(
            listOf(groupReference),
            singleReference,
            skipVisibleCheck = false,
        )

        assertEquals(listOf(groupReference), result)
    }

    @Test
    fun immediateProjectionShowKeepsBackgroundsWithoutReferencesHidden() {
        val layout = createLayout()

        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(0f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)

        assertEquals(1f, layout.projectionView.alpha, 0f)
        assertEquals(1f, layout.projectionView.prjBgStartFirstView.alpha, 0f)
        assertEquals(0f, layout.projectionView.prjBgStartSecondView.alpha, 0f)
        assertEquals(0f, layout.projectionView.prjBgEndFirstView.alpha, 0f)
    }

    @Test
    fun projectionCallbacksAreDispatchedOncePerAlphaDirection() {
        val layout = createLayout()
        val aware = RecordingAware()
        layout.floatingAware = aware

        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(0f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(0f, immediately = true)

        assertEquals(1, aware.showCallbacks)
        assertEquals(1, aware.hideCallbacks)
    }

    @Test
    fun immediateShowWithoutReferencesNeverExposesStaleBackgrounds() {
        val layout = createLayout()
        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)
        layout.projectionView.startProjectionViewAlphaAnimation(0f, immediately = true)
        layout.floatingAware = null

        layout.showFloatingItemBackground(show = true, animate = false)

        layout.projectionView.prjBgViewList.forEach { background ->
            assertEquals(0f, background.alpha, 0f)
        }

        layout.viewTreeObserver.dispatchOnPreDraw()

        assertEquals(1f, layout.projectionView.alpha, 0f)
        layout.projectionView.prjBgViewList.forEach { background ->
            assertEquals(0f, background.alpha, 0f)
        }
    }

    @Test
    fun projectionRelayoutRequestsAnimatedItemMatchingWhenShown() {
        val layout = createLayout()
        val projection = layout.projectionView
        projection.startProjectionViewItemAnimation(animate = false)
        projection.removeProjectionItemAnimationPreDrawListener()
        projection.alpha = 1f

        projection.layout(0, 0, 300, 80)

        assertTrue(ReflectionHelpers.getField(projection, "pendingProjectionItemAnimationAnimate"))
        assertTrue(ReflectionHelpers.getField(projection, "projectionItemAnimationPreDrawRegistered"))
    }

    @Test
    fun configurationChangeMakesNextItemMatchingImmediate() {
        val layout = createLayout()
        val projection = layout.projectionView
        layout.floatingAware = null
        projection.alpha = 1f
        projection.prjBgViewList.forEach { background -> background.alpha = 1f }
        projection.dispatchConfigurationChanged(Configuration(context.resources.configuration))

        projection.startProjectionViewItemAnimation(animate = true)
        layout.viewTreeObserver.dispatchOnPreDraw()

        projection.prjBgViewList.forEach { background ->
            assertEquals(0f, background.alpha, 0f)
            val animator: ObjectAnimator = ReflectionHelpers.getField(
                background,
                "prjBgViewAlphaAnimator",
            )
            assertFalse(animator.isRunning)
        }
    }

    @Test
    fun skipAnimationMakesItemMatchingImmediate() {
        val layout = createLayout().apply {
            floatingAware = null
            skipAnimation = true
        }
        val projection = layout.projectionView
        projection.alpha = 1f
        projection.prjBgViewList.forEach { background -> background.alpha = 1f }

        projection.startProjectionViewItemAnimation(animate = true)
        layout.viewTreeObserver.dispatchOnPreDraw()

        projection.prjBgViewList.forEach { background ->
            assertEquals(0f, background.alpha, 0f)
            val animator: ObjectAnimator = ReflectionHelpers.getField(
                background,
                "prjBgViewAlphaAnimator",
            )
            assertFalse(animator.isRunning)
        }
    }

    @Test
    fun hiddenProjectionRelayoutDoesNotRequestItemMatching() {
        val layout = createLayout()
        val projection = layout.projectionView
        projection.startProjectionViewItemAnimation(animate = false)
        projection.removeProjectionItemAnimationPreDrawListener()
        projection.alpha = 0f

        projection.layout(0, 0, 300, 80)

        assertFalse(ReflectionHelpers.getField(projection, "projectionItemAnimationPreDrawRegistered"))
    }

    @Test
    fun visibleStateReflectsSettledAlpha() {
        val layout = FloatingGroupLayout(context)

        layout.alpha = 1f
        assertEquals(FloatingLayoutState.STATE_SHOW, layout.getVisibleState())

        layout.alpha = 0f
        assertEquals(FloatingLayoutState.STATE_HIDE, layout.getVisibleState())
    }

    @Test
    fun visibleStateReflectsRunningAnimatorTarget() {
        val layout = FloatingGroupLayout(context)
        val animator = runningLayoutAnimator(layout)

        layout.viewTargetAlpha = 0f
        assertEquals(FloatingLayoutState.STATE_ANIMATING_TO_HIDE, layout.getVisibleState())

        layout.viewTargetAlpha = 1f
        assertEquals(FloatingLayoutState.STATE_ANIMATING_TO_SHOW, layout.getVisibleState())

        animator.cancel()
    }

    @Test
    fun invalidSettledAlphaLogsAndFallsBackToShownState() {
        val layout = FloatingGroupLayout(context).apply { alpha = 0.5f }

        assertEquals(FloatingLayoutState.STATE_SHOW, layout.getVisibleState())

        assertInvalidStateWasLogged()
    }

    @Test
    fun invalidRunningAnimatorTargetLogsAndFallsBackToShownState() {
        val layout = FloatingGroupLayout(context)
        val animator = runningLayoutAnimator(layout)
        layout.viewTargetAlpha = 0.5f

        assertEquals(FloatingLayoutState.STATE_SHOW, layout.getVisibleState())

        assertInvalidStateWasLogged()
        animator.cancel()
    }

    @Test
    fun appBarHideStateStartsProjectionAlphaAnimation() {
        val layout = createTopLayout()
        val behavior = FloatingTopLayout.FloatingTopBehavior<FloatingTopLayout>(context)

        behavior.onAppBarStateChanged(
            FloatingTopLayout.APP_BAR_STATE_EXPANDED_MASK,
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK,
            layout,
        )

        val animator: ObjectAnimator = ReflectionHelpers.getField(
            layout.projectionView,
            "prjViewAlphaAnimator",
        )
        assertTrue(animator.isRunning)
        assertEquals(1f, ReflectionHelpers.getField(layout.projectionView, "prjViewAnimateAlphaEndValue"))
        animator.cancel()
    }

    @Test
    fun unchangedAppBarHideMaskDoesNotStartProjectionAlphaAnimation() {
        val layout = createTopLayout()
        val behavior = FloatingTopLayout.FloatingTopBehavior<FloatingTopLayout>(context)

        behavior.onAppBarStateChanged(
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK,
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK or FloatingTopLayout.APP_BAR_STATE_COLLAPSED_MASK,
            layout,
        )

        val animator: ObjectAnimator = ReflectionHelpers.getField(
            layout.projectionView,
            "prjViewAlphaAnimator",
        )
        assertFalse(animator.isRunning)
        assertEquals(0f, layout.projectionView.alpha, 0f)
    }

    @Test
    fun appBarLeavingHideStateStartsProjectionHideAnimation() {
        val layout = createTopLayout()
        layout.projectionView.startProjectionViewAlphaAnimation(1f, immediately = true)
        val behavior = FloatingTopLayout.FloatingTopBehavior<FloatingTopLayout>(context)

        behavior.onAppBarStateChanged(
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK,
            FloatingTopLayout.APP_BAR_STATE_EXPANDED_MASK,
            layout,
        )

        val animator: ObjectAnimator = ReflectionHelpers.getField(
            layout.projectionView,
            "prjViewAlphaAnimator",
        )
        assertTrue(animator.isRunning)
        assertEquals(0f, ReflectionHelpers.getField(layout.projectionView, "prjViewAnimateAlphaEndValue"))
        animator.cancel()
    }

    @Test
    fun disabledProjectionTransitionIgnoresAppBarStateChange() {
        val layout = createTopLayout().apply { enablePrjAlphaTransition = false }
        val behavior = FloatingTopLayout.FloatingTopBehavior<FloatingTopLayout>(context)

        behavior.onAppBarStateChanged(
            FloatingTopLayout.APP_BAR_STATE_EXPANDED_MASK,
            FloatingTopLayout.APP_BAR_STATE_HIDE_MASK,
            layout,
        )

        val animator: ObjectAnimator = ReflectionHelpers.getField(
            layout.projectionView,
            "prjViewAlphaAnimator",
        )
        assertFalse(animator.isRunning)
        assertEquals(0f, layout.projectionView.alpha, 0f)
    }

    private fun createLayout(): FloatingGroupLayout = FloatingGroupLayout(context).apply {
        addView(
            View(context),
            FrameLayout.LayoutParams(100, 40),
        )
    }

    private fun createTopLayout(): FloatingTopLayout = FloatingTopLayout(context).apply {
        addView(
            View(context),
            FrameLayout.LayoutParams(100, 40),
        )
    }

    private fun runningLayoutAnimator(layout: FloatingGroupLayout): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat(layout, View.ALPHA, layout.alpha, 0f).apply {
            duration = 1_000L
            start()
        }
        assertTrue(animator.isRunning)
        ReflectionHelpers.setField(layout, "layoutAlphaAnimator", animator)
        return animator
    }

    private fun assertInvalidStateWasLogged() {
        assertTrue(
            ShadowLog.getLogsForTag("FloatingGroupLayout").any { item ->
                item.type == Log.ERROR && item.msg.contains("Invalid State on getVisibleState")
            },
        )
    }

    private class RecordingAware : FloatingAware {
        var showCallbacks = 0
        var hideCallbacks = 0

        override fun onStartShowFloatingBackground() {
            showCallbacks++
        }

        override fun onStartHideFloatingBackground() {
            hideCallbacks++
        }
    }

    private class AppBarBackedFloatingGroupLayout(
        context: Context,
        private val appBarState: Int = AppBarLayout.SESL_STATE_IDLE,
    ) : FloatingGroupLayout(context) {
        private val appBar = object : AppBarLayout(context) {
            override fun seslGetCurrentAppBarState(): Int = appBarState
        }

        override fun getAppBarLayout(): AppBarLayout = appBar
    }
}
