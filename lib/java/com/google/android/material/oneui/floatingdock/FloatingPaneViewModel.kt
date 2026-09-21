package com.google.android.material.oneui.floatingdock

import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier

class FloatingPaneViewModel(
    state: Int = FloatingPane.FloatingPaneState.STATE_IDLE.state,
    private val callbackNotifier: FloatingPaneCallbackNotifier
) : FloatingDockLogTag {

    override val logTag: String = "FloatingPaneViewModel"

    var state: Int = state
        set(value) {
            if (field == value) {
                return
            }
            debug("callback onStateChanged=${FloatingPane.FloatingPaneState(value)}")
            callbackNotifier.onStateChanged(value)
            field = value
        }
}
