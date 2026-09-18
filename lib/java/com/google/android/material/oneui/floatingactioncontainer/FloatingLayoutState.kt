package com.google.android.material.oneui.floatingactioncontainer

/**
 * State of a floating layout, describing whether it is shown, hidden, or animating
 * between those states.
 */
enum class FloatingLayoutState {
    STATE_SHOW,
    STATE_HIDE,
    STATE_ANIMATING_TO_SHOW,
    STATE_ANIMATING_TO_HIDE,
    STATE_NONE
}
