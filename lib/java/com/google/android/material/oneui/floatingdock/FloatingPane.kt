package com.google.android.material.oneui.floatingdock

/**
 * Interface definition containing value classes and mode constants for One UI FloatingPane components.
 *
 * <p>Defines display modes (NONE, BOTTOM, SIDE, FLOATING) and interaction states (IDLE, MOVE, RESIZE)
 * used by floating dock windows, panes, and behaviors in Samsung One UI.
 */
interface FloatingPane {
    @JvmInline
    value class FloatingPaneMode(val type: Int) {
        companion object {
            val MODE_NONE = FloatingPaneMode(0)
            val MODE_BOTTOM = FloatingPaneMode(1)
            val MODE_SIDE = FloatingPaneMode(2)
            val MODE_FLOATING = FloatingPaneMode(4)
            val MODE_ALL = FloatingPaneMode(7)
        }

        fun contains(mode: FloatingPaneMode): Boolean {
            return (type and mode.type) != MODE_NONE.type
        }
    }

    @JvmInline
    value class FloatingPaneState(val state: Int) {
        companion object {
            val STATE_IDLE = FloatingPaneState(1)
            val STATE_MOVE = FloatingPaneState(2)
            val STATE_RESIZE = FloatingPaneState(3)
        }
    }
}
