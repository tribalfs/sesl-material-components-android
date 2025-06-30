package com.google.android.material.oneui.floatingdock

interface FloatingPane {
    /**
     * Represents the different modes a floating pane can be in.
     *
     * Each mode is represented by an integer value. The modes can be combined using bitwise operations.
     *
     * Available modes:
     * - `MODE_NONE`: The floating pane is not active.
     * - `MODE_BOTTOM`: The floating pane is docked to the bottom of the screen.
     * - `MODE_SIDE`: The floating pane is docked to the side of the screen.
     * - `MODE_FLOATING`: The floating pane is floating freely on the screen.
     * - `MODE_ALL`: A combination of all possible modes.
     */
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

    /**
     * Represents the state of the floating pane.
     *
     * The state can be one of the following:
     * - `STATE_IDLE`: The pane is idle and not being interacted with.
     * - `STATE_MOVE`: The pane is being moved by the user.
     * - `STATE_RESIZE`: The pane is being resized by the user.
     */
    @JvmInline
    value class FloatingPaneState(val state: Int) {
        companion object {
            val STATE_IDLE = FloatingPaneState(1)
            val STATE_MOVE = FloatingPaneState(2)
            val STATE_RESIZE = FloatingPaneState(3)
        }

    }
}