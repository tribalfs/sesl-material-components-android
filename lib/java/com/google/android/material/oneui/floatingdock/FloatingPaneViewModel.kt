package com.google.android.material.oneui.floatingdock

import android.os.Build
import android.util.Log
import com.google.android.material.oneui.floatingdock.FloatingPane.FloatingPaneState
import com.google.android.material.oneui.floatingdock.util.FloatingPaneCallbackNotifier
import java.util.Locale

/**
 * ViewModel for the floating pane.
 *
 * This class manages the state of the floating pane and notifies listeners of state changes.
 *
 * @property state The current state of the floating pane. See [FloatingPaneState] for possible values.
 * @property callbackNotifier A callback notifier to notify listeners of state changes.
 */
class FloatingPaneViewModel(
    state: FloatingPaneState,
    private val callbackNotifier: FloatingPaneCallbackNotifier
) {
    companion object {
        private const val TAG = "FloatingPaneViewModel"
        private val ENG: Boolean
        private val DEBUG: Boolean

        init {
            val type = Build.TYPE
            val lowerCase = type.lowercase(Locale.ROOT)
            val isEng = lowerCase == "eng" || lowerCase == "userdebug"
            ENG = isEng
            DEBUG = isEng
        }
    }

    /**
     * The current state of the floating pane.
     *
     * Setting this property will notify registered callbacks of the state change.
     * @see FloatingPaneState
     */
    var state: FloatingPaneState = state
        set(value) {
            if (field == value) {
                return
            }
            if (DEBUG) {
                Log.d(TAG, "callback onStateChanged=${value}")
            }
            callbackNotifier.onStateChanged(value)
            field = value
        }
}