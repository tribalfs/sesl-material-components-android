package com.google.android.material.oneui.floatingdock

import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier

class FloatingPaneViewModel(
    state: Int = FloatingPane.FloatingPaneState.STATE_IDLE.state,
    private val callbackNotifier: FloatingPaneCallbackNotifier
) {
    var state: Int = state
        set(value) {
            if (field != value) {
                field = value
                callbackNotifier.onStateChanged(value)
            }
        }
}
