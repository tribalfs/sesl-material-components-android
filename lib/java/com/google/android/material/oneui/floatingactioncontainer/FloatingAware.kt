package com.google.android.material.oneui.floatingactioncontainer

import android.content.res.Configuration
import android.graphics.Rect
import android.view.View

/**
 * Interface implemented by components that participate in a floating action container,
 * exposing reference views and bounding insets to position projection backgrounds and
 * reacting to floating background transition events.
 *
 * Implementations can be supplied to a floating container layout (such as [FloatingGroupLayout]
 * or [FloatingToolbarLayout]) via `floatingAware` to control how projection backgrounds
 * align with reference views, as well as to receive callbacks during transition animations
 * and configuration changes.
 *
 * Default implementations like `FloatingGroupAware` and `FloatingToolbarAware` are provided
 * for standard container and toolbar integration.
 */
interface FloatingAware {
    companion object {
        /**
         * Default option indicating no special behavior flags are set for the floating aware instance.
         */
        const val FLOATING_AWARE_OPTION_NONE = 0

        /**
         * Option flag indicating that visibility checks ([View.VISIBLE]) on reference views
         * should be skipped when resolving target views for projection matching.
         */
        const val FLOATING_AWARE_OPTION_SKIP_REFER_VISIBLE_CHECK = 1

        /**
         * Option flag indicating that spring scaling animations on floating containers should
         * apply only to reference views/projection backgrounds, omitting the main content view.
         */
        const val FLOATING_AWARE_OPTION_SCALE_WITHOUT_CONTENT_VIEW = 2
    }

    /**
     * Identifies the relative position slot of a reference view within a floating container layout.
     */
    enum class PositionType {
        /** Primary reference view located at the start/leading position (e.g., toolbar navigation view). */
        START_FIRST,

        /** Secondary reference view located at the start/leading position (e.g., custom action view). */
        START_SECOND,

        /** Primary reference view located at the end/trailing position (e.g., action menu view). */
        END_FIRST
    }

    /**
     * Returns a bitmask combining option flags (such as [FLOATING_AWARE_OPTION_SKIP_REFER_VISIBLE_CHECK]
     * or [FLOATING_AWARE_OPTION_SCALE_WITHOUT_CONTENT_VIEW]) that configure floating layout behavior.
     *
     * @return Bitmask of floating aware option flags. Defaults to [FLOATING_AWARE_OPTION_NONE].
     */
    fun getFloatingAwareOptions(): Int = FLOATING_AWARE_OPTION_NONE

    /**
     * Returns the primary reference [View] associated with the specified [PositionType] slot,
     * used by the floating container to position matching projection backgrounds.
     *
     * @param type Target [PositionType] slot.
     * @return The reference [View] for the slot, or `null` if none exists.
     */
    fun getReferenceView(type: PositionType): View? = null

    /**
     * Returns a [Rect] specifying the padding inset adjustments (left, top, right, bottom)
     * applied to the projection background bounds matching the reference view at the given [PositionType].
     *
     * @param type Target [PositionType] slot.
     * @return [Rect] containing inset values in pixels. Defaults to an empty `Rect()`.
     */
    fun getReferenceViewInset(type: PositionType): Rect = Rect()

    /**
     * Returns a list of reference [View]s associated with the specified [PositionType] slot.
     * Useful when a position slot corresponds to multiple views (e.g. menu items within an action menu).
     *
     * @param type Target [PositionType] slot.
     * @return List of reference [View]s for the slot, or `null` if not applicable.
     */
    fun getReferenceViews(type: PositionType): List<View>? = null

    /**
     * Callback invoked when the configuration of the floating view changes (e.g., orientation or theme change).
     *
     * @param show `true` if the floating container/background is currently visible; `false` otherwise.
     * @param newConfig The new [Configuration] applied to the device/container.
     */
    fun onFloatingViewConfigurationChanged(show: Boolean, newConfig: Configuration) {}

    /**
     * Callback invoked when the floating background starts its hide transition animation.
     */
    fun onStartHideFloatingBackground() {}

    /**
     * Callback invoked when the floating background starts its show transition animation.
     */
    fun onStartShowFloatingBackground() {}
}

