package com.google.android.material.oneui.floatingdock.util

import android.util.Log
import android.view.View
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector

/**
 * Helper class for performing haptic feedback on a view.
 *
 * This class provides methods for performing various types of haptic feedback, such as when an edit
 * guide is moved, when a long press occurs, or when a tap occurs.
 */
object HapticFeedbackHelper {
    private fun semPerformHapticFeedback(view: View, index: Int) {
        Log.d("HapticFeedbackHelper", "performHaptic index=$index")
        view.performHapticFeedback(SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(index))
    }

    fun onEditGuide(view: View) {
        semPerformHapticFeedback(view, 41)
    }

    fun onEditGuideWithSnapping(view: View) {
        semPerformHapticFeedback(view, 49)
    }

    fun onLongPress(view: View) {
        semPerformHapticFeedback(view, 108)
    }

    fun onTap(view: View) {
        semPerformHapticFeedback(view, 1)
    }
}