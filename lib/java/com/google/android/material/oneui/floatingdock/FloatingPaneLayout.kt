package com.google.android.material.oneui.floatingdock

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.util.theme.SeslThemeResourceHelper.getColorInt
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor.ThemeResourceColor
import androidx.core.view.SemBlurCompat
import androidx.core.view.SemBlurCompat.BLUR_MODE_CANVAS
import androidx.core.view.SemBlurCompat.BLUR_UI_LOW_ULTRA_THICK_DARK
import androidx.core.view.SemBlurCompat.BLUR_UI_LOW_ULTRA_THICK_LIGHT
import androidx.core.view.SemBlurCompat.SeslBlurMode
import androidx.core.view.SemBlurCompat.isBlurEffectPresetSupport
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R
import com.google.android.material.internal.ThemeEnforcement
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_ALL
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_BOTTOM
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneMode.Companion.MODE_FLOATING
import com.google.android.material.oneui.floatingdock.behavior.FloatingBehavior
import com.google.android.material.oneui.floatingdock.util.HapticFeedbackHelper
import com.google.android.material.oneui.floatingdock.util.HapticFeedbackHelper.onEditGuide
import com.google.android.material.oneui.floatingdock.util.updateViewBounds
import org.jetbrains.annotations.NotNull


/**
 * A layout that allows a child view to be displayed as a floating pane that can be moved, resized,
 * docked, or minimized.
 *
 * It provides functionality for:
 * - Displaying a child view as a floating pane.
 * - Moving and resizing the floating pane.
 * - Docking the floating pane to the sides or bottom of the screen.
 * - Minimizing the floating pane to an icon.
 * - Showing a pre-docking effect when the pane is dragged near a docking area.
 * - Applying a blur effect to the background behind the pane (if supported).
 *
 * To use FloatingPaneLayout, add it to your layout XML and then add your content view
 * with the ID `R.id.result_layout_content`. You can optionally add a minimized view
 * with the ID `R.id.result_layout_minimize`.
 *
 * Example usage:
 * ```xml
 * <com.google.android.material.oneui.floatingdock.FloatingPaneLayout
 *     android:id="@+id/floating_pane_layout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <FrameLayout
 *         android:id="@id/result_layout_content"
 *         android:layout_width="300dp"
 *         android:layout_height="400dp"
 *         android:background="@android:color/white">
 *         <!-- Your content view here -->
 *     </FrameLayout>
 *
 *     <ImageView
 *         android:id="@id/result_layout_minimize"
 *         android:layout_width="48dp"
 *         android:layout_height="48dp"
 *         android:src="@drawable/ic_minimize_icon" />
 *
 * </com.google.android.material.oneui.floatingdock.FloatingPaneLayout>
 * ```
 *
 * You can control the behavior of the floating pane using methods like `show()`, `hide()`,
 * `enterMinimizeView()`, and `setAllowModes()`. Callbacks can be registered via `addCallback()`
 * to listen to pane state changes.
 *
 * @param context The Context the view is running in, through which it can
 */
class FloatingPaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "SeslFloatingPaneLayout"
    }

    private var blurDisableInternal = false

    @SeslBlurMode
    private var blurMode: Int = BLUR_MODE_CANVAS
    private val dockingEffectCornerRadius: Int
    private val dockingEffectPadding: Int

    private var floatingView: FloatingPaneView? = null

    private val hidePreDockingEffect: AnimatorSet
    private var isShowingPreDockingEffect = false

    private val minimizedIcon = context.getDrawable(R.drawable.sesl_floating_pane_minimized_icon)
    private val minimizedIconSize = context.resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_minimized_size)
    private val minimizedIconAlphaAnimDuration = 150L
    private val minimizedSaleAnimDuration = 250L

    private val preDockingEffect: ImageView = ImageView(context).apply {
        setBackgroundResource(R.drawable.sesl_floating_pane_pre_docking_effect)
        clipToOutline = true
        visibility = GONE
    }

    private val resizeRectDrawable = context.getDrawable(R.drawable.sesl_floating_pane_resize_background)
    private val resizeRectCenter = Point(0, 0)
    private var resizeRect: Rect? = null
    private var resizeRectAlphaValue = 0f

    private val showPreDockingEffect: AnimatorSet
    private val topLimitSize: Int

    private val resizeAlphaAnimation: ValueAnimator
    private val minimizedIconScaleShowAnimation: SpringAnimation
    private val minimizedIconScaleHideAnimation: ValueAnimator
    private val minimizedIconAlphaAnimation: ValueAnimator

    private var minimizedIconAnimAlphaValue = 0f
    private var minimizedIconAnimScaleValue = 0f
    private var showMinimizedIcon = false
    private var allowedMode = MODE_ALL

    init {
        val a = ThemeEnforcement.obtainTintedStyledAttributes(
            context, attrs, R.styleable.FloatingLayout, defStyleAttr, defStyleRes
        )
        topLimitSize = a.getResourceId(R.styleable.FloatingLayout_topLimitSize, 0)
        a.recycle()

        dockingEffectPadding = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_pre_docking_effect_padding)
        dockingEffectCornerRadius = resources.getDimensionPixelSize(R.dimen.sesl_floating_pane_background_corner_radius)

        showPreDockingEffect = AnimatorSet().apply {
            val anim = AnimatorInflater.loadAnimator(context, R.animator.pre_docking_effect_show_anim)
            anim.setTarget(preDockingEffect)
            playTogether(anim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    preDockingEffect.visibility = VISIBLE
                }
            })
        }

        hidePreDockingEffect = AnimatorSet().apply {
            val anim = AnimatorInflater.loadAnimator(context, R.animator.pre_docking_effect_hide_anim)
            anim.setTarget(preDockingEffect)
            playTogether(anim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    preDockingEffect.visibility = GONE
                }
            })
        }

        blurDisableInternal = !isBlurEffectPresetSupport()

        resizeAlphaAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = PathInterpolator(0f, 0f, 1f, 1f)
            duration = minimizedIconAlphaAnimDuration
            addUpdateListener {
                resizeRectAlphaValue = it.animatedValue as Float
                invalidate()
            }
        }

        minimizedIconScaleShowAnimation = SpringAnimation(FloatValueHolder()).apply {
            spring = SpringForce().apply {
                dampingRatio = 0.75f
                stiffness = 256f
                finalPosition = 1_000f
            }
            setMinValue(0f)
            setMaxValue(1_000f)
            addUpdateListener { _, value, _ ->
                minimizedIconAnimScaleValue = value
                invalidate()
            }
        }

        minimizedIconScaleHideAnimation = ValueAnimator.ofFloat(1_000f, 0f).apply {
            interpolator = PathInterpolator(0f, 0f, 1f, 1f)
            addUpdateListener {
                minimizedIconAnimScaleValue = it.animatedValue as Float
                invalidate()
            }
        }

        minimizedIconAlphaAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = PathInterpolator(0f, 0f, 1f, 1f)
            addUpdateListener {
                minimizedIconAnimAlphaValue = it.animatedValue as Float
                invalidate()
            }
        }
    }

    override fun addView(child: View, index: Int) {
        addView(child)
    }

    override fun addView(child: View) {
        addViewInternal(child, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun addView(child: View, width: Int, height: Int) {
        addViewInternal(child, ViewGroup.LayoutParams(width, height))
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        addViewInternal(child, params)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        addViewInternal(child, params)
    }

    private fun addViewInternal(child: View, params: ViewGroup.LayoutParams) {
        if (child.id == R.id.result_layout_content) {
            super.addView(
                preDockingEffect, 0, LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                    Gravity.LEFT or Gravity.TOP
                )
            )
            floatingView = FloatingPaneView(context, this).apply {
                setTopLimitSize(topLimitSize)
                setContentView(child)
            }
            (params as? LayoutParams)?.gravity = Gravity.LEFT or Gravity.TOP
            super.addView(floatingView, 1, params)
        } else if (child.id == R.id.result_layout_minimize) {
            floatingView?.setMinimizeView(child)
        } else {
            Log.e(TAG, "Should add R.id.result_layout_content on first")
            removeAllViews()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (resizeRectAlphaValue > 0f) {
            resizeRectDrawable?.let {
                it.alpha = (resizeRectAlphaValue * 255).toInt()
                it.draw(canvas)
            }
            if (minimizedIconAnimScaleValue > 0f) {
                val scale = minimizedIconAnimScaleValue * 0.001f
                val halfSize = (minimizedIconSize / 2f) * scale
                minimizedIcon?.let { icon ->
                    icon.setBounds(0, 0, minimizedIconSize, minimizedIconSize)
                    icon.alpha = (minimizedIconAnimAlphaValue * 255).toInt()
                    canvas.save()
                    canvas.translate(resizeRectCenter.x - halfSize, resizeRectCenter.y - halfSize)
                    canvas.scale(scale, scale)
                    icon.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }

    /**
     * Stops drawing all elements managed by this layout, such as the resize rectangle
     * and pre-docking effect.
     *
     * This is typically called when the floating pane is no longer active or visible,
     * to optimize drawing performance.
     */
    fun seslStopDrawAllRequested() {
        resizeRect = null
        preDockingEffect.setVisibility(8)
        invalidate()
        setWillNotDraw(true)
    }

    /**
     * Shows or hides the minimized icon with an animation.
     *
     * @param show Whether to show the minimized icon.
     * @param skipAnimate Whether to skip the animation. Defaults to false.
     */
    @JvmOverloads
    fun showMinimizedIcon(show: Boolean, skipAnimate: Boolean = false) {
        if (showMinimizedIcon == show) return
        showMinimizedIcon = show

        if (minimizedIconAlphaAnimation.isRunning) {
            minimizedIconAlphaAnimation.cancel()
        }

        if (show) {
            minimizedIconAlphaAnimation.setFloatValues(minimizedIconAnimAlphaValue, 1.0f)
            minimizedIconAlphaAnimation.setDuration(if (skipAnimate) 0L else ((1.0f - minimizedIconAnimAlphaValue) * minimizedIconAlphaAnimDuration).toLong())
            if (minimizedIconScaleHideAnimation.isRunning) {
                minimizedIconScaleHideAnimation.cancel()
                minimizedIconScaleShowAnimation.setStartValue(minimizedIconAnimScaleValue)
            } else {
                minimizedIconScaleShowAnimation.setStartValue(0.0f)
            }
            minimizedIconScaleShowAnimation.start()
            HapticFeedbackHelper.onEditGuideWithSnapping(this)
        } else {
            minimizedIconAlphaAnimation.setFloatValues(minimizedIconAnimAlphaValue, 0.0f)
            minimizedIconAlphaAnimation.setDuration(if (skipAnimate) 0L else (minimizedIconAlphaAnimDuration * minimizedIconAnimAlphaValue).toLong())
            if (minimizedIconScaleShowAnimation.isRunning) {
                minimizedIconScaleShowAnimation.cancel()
                minimizedIconScaleHideAnimation.setFloatValues(minimizedIconAnimScaleValue, 0.0f)
                minimizedIconScaleHideAnimation.setDuration(if (skipAnimate) 0L else (minimizedSaleAnimDuration * minimizedIconAnimScaleValue).toLong())
            } else {
                minimizedIconScaleHideAnimation.setFloatValues(1_000.0f, 0.0f)
                minimizedIconScaleHideAnimation.setDuration(if (skipAnimate) 0L else minimizedSaleAnimDuration)
            }
            minimizedIconScaleHideAnimation.start()
        }
        minimizedIconAlphaAnimation.start()
    }

    /**
     * Shows or hides the pre-docking effect.
     *
     * This effect is displayed when the floating pane is dragged near a docking area, providing
     * visual feedback to the user.
     *
     * @param show `true` to show the effect, `false` to hide it.
     * @param dockingArea The [Rect] defining the area where the docking effect should be displayed.
     *                    This area will be inset by [dockingEffectPadding] before the effect is drawn.
     */
    fun seslShowProDockingEffect(show: Boolean, dockingArea: Rect) {
        if (isShowingPreDockingEffect != show) {
            isShowingPreDockingEffect = show
            if (showPreDockingEffect.isRunning()) {
                showPreDockingEffect.cancel()
            }
            if (hidePreDockingEffect.isRunning()) {
                hidePreDockingEffect.cancel()
            }
            if (!show) {
                hidePreDockingEffect.start()
                return
            }
            val padding = dockingEffectPadding
            dockingArea.inset(padding, padding)
            preDockingEffect.updateViewBounds(dockingArea)
            val semBlurCompat = SemBlurCompat
            if (!semBlurCompat.isBlurEffectPresetSupport() || blurDisableInternal) {
                preDockingEffect.setBackgroundResource(R.drawable.sesl_floating_pane_pre_docking_effect_no_blur)
                preDockingEffect.setBackgroundTintList(
                    ColorStateList.valueOf(
                        getColorInt(
                            preDockingEffect.context,
                            ThemeResourceColor(
                                R.color.sesl_floating_pane_docking_effect_no_blur_color,
                                R.color.sesl_floating_pane_docking_effect_no_blur_color_dark
                            )
                        )
                    )
                )
                semBlurCompat.setBlurInfoClear(preDockingEffect)
            } else {
                preDockingEffect.setBackgroundResource(R.drawable.sesl_floating_pane_pre_docking_effect)
                if (Build.VERSION.SDK_INT >= 35) {
                    setBlurEffect(preDockingEffect)
                }
            }
            showPreDockingEffect.start()
            onEditGuide(this)
        }
    }

    @RequiresApi(35)
    private fun setBlurEffect(view: View) {
        SemBlurCompat.setBlurEffectPreset(
            view = view,
            blurMode = blurMode,
            colorCurvePreset = if (SeslMisc.isLightTheme(context)) BLUR_UI_LOW_ULTRA_THICK_LIGHT else BLUR_UI_LOW_ULTRA_THICK_DARK,
            color = null,
            cornerRadius = if (blurMode == BLUR_MODE_CANVAS) null else dockingEffectCornerRadius.toFloat()
        )
    }

    /**
     * Sets the allowed modes for the floating pane.
     *
     * This method controls which docking/floating behaviors are permitted for the pane.
     * By default, all modes ([FloatingPaneMode.MODE_ALL]) are allowed.
     *
     * @param mode The [FloatingPaneMode] to allow. Can be one of:
     *   - [FloatingPaneMode.MODE_ALL]: Allows all modes (floating, bottom docking, side docking).
     *   - [FloatingPaneMode.MODE_FLOATING]: Only allows the pane to be in a floating state.
     *   - [FloatingPaneMode.MODE_BOTTOM]: Only allows the pane to be docked to the bottom.
     *   - [FloatingPaneMode.MODE_SIDE]: Only allows the pane to be docked to the left or right.
     */
    fun setAllowModes(mode: FloatingPaneMode) {
        Log.d(TAG, "Custom allowed mode=$mode")
        allowedMode = mode
    }

    /**
     * Disables the blur effect on the pre-docking effect and the floating pane background.
     *
     * @param disable `true` to disable the blur effect, `false` to enable it (if supported).
     */
    fun setBlurDisable(disable: Boolean) {
        Log.d(TAG, "setBlurDisable disable=$disable")
        if (blurDisableInternal != disable) {
            setBlurDisableInternal(disable)
        }
    }

    private fun setBlurDisableInternal(disable: Boolean) {
        if (isBlurEffectPresetSupport() || disable) {
            blurDisableInternal = disable
        } else {
            Log.e(TAG, "Blur effect is not available due to the SDK version")
            blurDisableInternal = true
        }
    }

    /**
     * Adds a callback to be notified of floating pane events.
     *
     * @param callback The callback to add.
     */
    fun addCallback(@NotNull callback: IFloatingPaneCallback) {
        floatingView?.apply{
            addCallbacks(callback)
            Log.d(TAG, "addCallback $callback")
        } ?: Log.w(TAG, "Floating not added yet")
    }

    /**
     * Hides the floating pane.
     *
     * @param animate True to animate the hiding of the pane, false to hide it immediately.
     *                Defaults to true.
     */
    @JvmOverloads
    fun hide(animate: Boolean = true) {
        Log.d(TAG,"hide animate= $floatingView  floatingView= $animate")
        floatingView?.hide(animate)
    }

    /**
     * Shows the floating pane.
     *
     * @param animate True to animate the showing of the pane, false otherwise.
     *                Defaults to true.
     */
    @JvmOverloads
    fun show(animate: Boolean = true) {
        Log.d(TAG,"show animate= $floatingView  floatingView= $animate")
        floatingView?.show(animate)
    }

    /**
     * Sets an untouchable area inset for the floating pane when it's in floating mode.
     * This allows defining a region around the floating pane where touch events will be ignored.
     *
     * @param left The left inset in pixels.
     * @param top The top inset in pixels.
     * @param right The right inset in pixels.
     * @param bottom The bottom inset in pixels.
     */
    fun addUntouchableAreaInset(left: Int, top: Int, right: Int, bottom: Int) {
        val floatingViewBehavior = if (floatingView != null) floatingView?.getBehavior(MODE_FLOATING) else null
        Log.d(TAG, "setUntouchableAreaInset inset=($left, $top, $right, $bottom)")

        (floatingViewBehavior as? FloatingBehavior)?.apply {
            customUntouchableAreaInset = Rect(left, top, right, bottom)
            return
        }

        Log.d(TAG, " fail to setUntouchableAreaInset, FloatingMode is not exist($floatingViewBehavior})")
    }

    /**
     * Enters or exits the minimized view state for the floating pane.
     *
     * @param minimize `true` to enter the minimized view, `false` to exit.
     */
    fun enterMinimizeView(minimize: Boolean) {
        floatingView?.enterMinimizeView(minimize)
        Log.d(TAG, "enterMinimize $minimize")
    }

    /**
     * Retrieves the current mode of the floating pane.
     *
     * @return The current [FloatingPaneMode] of the floating pane. If the floating pane view
     * has not been added yet, it defaults to [MODE_BOTTOM] and logs a warning.
     */
    fun getPaneMode(): FloatingPaneMode {
        val floatingPaneView = floatingView ?: return MODE_BOTTOM.also { Log.w(TAG, "Floating not added yet") }
        return floatingPaneView.mode
    }

    /**
     * Retrieves the top limit size for the floating pane.
     * This value typically corresponds to the height of the status bar or a similar top UI element,
     * preventing the floating pane from overlapping with it.
     *
     * @return The top limit size in pixels.
     */
    fun getTopLimitSize(): Int = topLimitSize

    fun isMinimizeView() = (floatingView?.isMinimizeView() == true).also { Log.d(TAG, "isMinimizeView $it") }

    /**
     * Checks if the floating pane is currently showing.
     *
     * @return `true` if the floating pane is showing, `false` otherwise.
     */
    fun isShowing() = floatingView?.isShowing() == true

    public override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!changed) return
        floatingView?.onChangedParentBounds(left, top, right, bottom)
    }

    /**
     * Requests drawing of a resize rectangle with an optional animation.
     *
     * This function is responsible for displaying a visual cue (a rectangle)
     * when the floating pane is being resized. It handles the animation for
     * fading the rectangle in and out.
     *
     * If `rect` is null, the resize rectangle will fade out.
     * If `rect` is not null, the resize rectangle will fade in (if not already visible)
     * and its bounds will be updated.
     *
     * @param rect The [Rect] defining the bounds of the resize rectangle,
     *             or null to hide the rectangle.
     */
    fun seslRequestDrawResizeRect(rect: Rect?) {
        if (resizeRect == rect) return
        resizeRect = rect

        if (rect != null) {
            resizeRectDrawable?.bounds = rect
            resizeRectCenter.x = (rect.width() / 2) + rect.left
            resizeRectCenter.y = (rect.height() / 2) + rect.top
        }

        if (rect == null) {
            resizeAlphaAnimation.setFloatValues(1.0f, 0.0f)
            resizeAlphaAnimation.start()
        } else if (!resizeAlphaAnimation.isRunning && resizeRectAlphaValue == 0.0f) {
            resizeAlphaAnimation.setFloatValues(0.0f, 1.0f)
            resizeAlphaAnimation.start()
        }

        if (resizeRectAlphaValue <= 0.0f) {
            setWillNotDraw(true)
        } else {
            setWillNotDraw(false)
            invalidate()
        }
    }

    fun setPaneMode(mode: FloatingPaneMode) {
        val floatingPaneView = floatingView
        if (floatingPaneView == null) {
            Log.w(TAG, "Floating not added yet")
            return
        }
        floatingPaneView.changePaneLayoutMode(mode, false, false, false)
        Log.d(TAG, "setPaneMode $mode")
    }

    /**
     * Sets the height of the result content for a specific floating pane mode.
     *
     * This function allows you to define a custom height for the content area
     * when the floating pane is in a particular mode (e.g., docked to the bottom,
     * floating). If `height` is null, the default height for that mode will be used.
     *
     * @param mode The [FloatingPaneMode] for which to set the result height.
     * @param height The desired height in pixels, or null to use the default height.
     */
    fun setResultHeight(mode: FloatingPaneMode, height: Int?) {
        floatingView?.setResultHeight(mode, height)
    }


    /**
     * Sets the background resource for the result view of the floating pane.
     *
     * This allows customizing the appearance of the main content area of the floating pane
     * based on its current mode (e.g., floating, docked).
     *
     * @param mode The [FloatingPaneMode] for which to set the background.
     * @param resId The drawable resource ID to use as the background,
     *              or null to remove the background.
     */
    fun setResultViewBackgroundResource(mode: FloatingPaneMode, @DrawableRes resId: Int?) {
        Log.d(
            TAG,
            "setResultViewBackgroundResource mode=$mode resId= $resId")
            floatingView?.setResultViewBackgroundResource(mode, resId)
    }

    /**
     * Sets the width of the floating pane for a specific mode.
     *
     * @param mode The [FloatingPaneMode] for which to set the width.
     * @param width The desired width in pixels, or null to use the default width.
     */
    fun setResultWidth(mode: FloatingPaneMode, width: Int?) {
        floatingView?.setResultWidth(mode, width)
    }

    /**
     * Sets an animation listener for the show/hide animations of the floating pane
     * in a specific mode.
     *
     * This allows you to receive callbacks when the pane's show or hide animation
     * starts or ends for a particular mode (e.g., floating, bottom-docked).
     *
     * @param mode The [FloatingPaneMode] for which to set the listener.
     * @param callback The [IFloatingPaneCallback.AnimationListener] to be invoked during
     *                 the show/hide animations for the specified mode, or `null` to remove
     *                 an existing listener.
     */
    fun setShowAnimationListener(
        mode: FloatingPaneMode,
        callback: IFloatingPaneCallback.AnimationListener?
    ) {
        Log.d(TAG, "setShowAnimationListener mode, $mode callback= $callback")
        val behavior =  floatingView?.getBehavior(mode) ?: return
        behavior.showAnimationListener = callback
    }


    /**
     * Sets an animation listener for the hide animation of the floating pane.
     *
     * This listener will be invoked when the hide animation for the specified mode starts or ends.
     *
     * @param mode The [FloatingPaneMode] for which to set the hide animation listener.
     * @param callback The [IFloatingPaneCallback.AnimationListener] to be invoked,
     *                 or null to remove any existing listener.
     */
    fun setHideAnimationListener(
        mode: FloatingPaneMode, callback: IFloatingPaneCallback.AnimationListener?
    ) {
        Log.d(TAG, "setHideAnimationListener mode, $mode, callback=$callback")
        val behavior = floatingView?.getBehavior(mode) ?: return
        behavior.hideAnimationListener = callback
    }

    /**
     * Sets the maximum height for the floating pane in a specific mode.
     *
     * This allows you to define a custom upper limit for the height of the pane
     * when it's in the specified [FloatingPaneMode].
     *
     * @param mode The [FloatingPaneMode] for which to set the maximum height.
     *             This can be [FloatingPaneMode.MODE_FLOATING], [FloatingPaneMode.MODE_BOTTOM],
     *             or [FloatingPaneMode.MODE_SIDE].
     * @param height The maximum height in pixels, or `null` to remove any custom maximum height
     *               and revert to the default behavior.
     */
    fun setMaxHeight(mode: FloatingPaneMode, height: Int?) {
        Log.d(TAG, "setMaxHeight mode=$mode, height=$height")
        val behavior =  floatingView?.getBehavior(mode) ?: return
        behavior.customMaxHeight = height
    }

    /**
     * Sets the maximum width for the floating pane in a specific mode.
     *
     * This method allows you to define a custom maximum width for the floating pane
     * when it is in a particular [FloatingPaneMode] (e.g., floating, docked).
     *
     * @param mode The [FloatingPaneMode] for which to set the maximum width.
     * @param width The maximum width in pixels, or `null` to remove a previously set custom maximum width.
     */
    fun setMaxWidth(mode: FloatingPaneMode, width: Int?) {
        Log.d(TAG, "setMaxWidth mode=$mode, width=$width")
        val behavior =  floatingView?.getBehavior(mode) ?: return
        behavior.customMaxWidth = width
    }

    /**
     * Sets the minimum height for the floating pane in a specific mode.
     *
     * This method allows customizing the minimum height of the floating pane
     * when it's in a particular [FloatingPaneMode] (e.g., floating, bottom-docked).
     *
     * @param mode The [FloatingPaneMode] for which to set the minimum height.
     * @param height The minimum height in pixels. If null, the default minimum height
     *               for the specified mode will be used.
     */
    fun setMinHeight(mode: FloatingPaneMode, height: Int?) {
        Log.d(TAG, "setMinHeight mode=$mode, height=$height")
        val behavior =  floatingView?.getBehavior(mode) ?: return
        behavior.customMinHeight = height
    }

    /**
     * Sets the minimum width for the floating pane in a specific mode.
     *
     * This allows you to define a minimum width that the floating pane can be resized to
     * when it's in the specified [FloatingPaneMode].
     *
     * @param mode The [FloatingPaneMode] for which to set the minimum width.
     * @param width The minimum width in pixels, or `null` to remove any previously set minimum width.
     */
    fun setMinWidth(mode: FloatingPaneMode, width: Int?) {
        Log.d(TAG, "setMinWidth mode=$mode, width=$width")
        val behavior =  floatingView?.getBehavior(mode) ?: return
        behavior.customMinWidth = width
    }


    /**
     * Sets the view to be displayed when the floating pane is minimized.
     *
     * This view will be shown when [enterMinimizeView] is called with `true`.
     *
     * @param view The [View] to display when minimized.
     */
    fun setMinimizeView(view: View) {
        floatingView?.setMinimizeView(view)
        Log.d(TAG, "setMinimizeView $view")
    }

    /**
     * Sets the custom width for the minimized state of the floating pane in a specific mode.
     *
     * This allows overriding the default minimized width for a given [FloatingPaneMode].
     * If `width` is null, the default minimized width for that mode will be used.
     *
     * @param mode The [FloatingPaneMode] for which to set the custom minimized width.
     * @param width The custom minimized width in pixels, or null to use the default.
     */
    fun setMinimizeWidth(mode: FloatingPaneMode, width: Int?) {
        Log.d(TAG, "setMinimizeWidth mode=$mode, width=$width")
        val behavior = floatingView?.getBehavior(mode) ?: return
        behavior.customMinimizeWidth = width
    }
}