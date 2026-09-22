package com.google.android.material.oneui.floatingdock

import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier

class FloatingPaneViewModel(
    state: FloatingPaneState = FloatingPaneState.STATE_IDLE,
    private val callbackNotifier: FloatingPaneCallbackNotifier
) : FloatingDockLogTag {

    override val logTag: String = "FloatingPaneViewModel"

    var state: FloatingPaneState = state
        set(value) {
            if (field == value) {
                return
            }
            debug("callback onStateChanged=$value")
            callbackNotifier.onStateChanged(value)
            field = value
        }
}
