package com.google.android.material.oneui.floatingdock.util

import android.view.View
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.floatingdock.FloatingDockLogTag

object HapticFeedbackHelper : FloatingDockLogTag {
    override val logTag: String = "HapticFeedbackHelper"

    private fun semPerformHapticFeedback(view: View, index: Int) {
        debug("performHaptic index=$index")
        view.performHapticFeedback(SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(index))
    }

    fun onTap(view: View) {
        semPerformHapticFeedback(view, 1)
    }

    fun onLongPress(view: View) {
        semPerformHapticFeedback(view, 108)
    }

    fun onEditGuide(view: View) {
        semPerformHapticFeedback(view, 41)
    }

    fun onEditGuideWithSnapping(view: View) {
        semPerformHapticFeedback(view, 49)
    }

    fun onTouchMinimizeThreshold(view: View) {
        semPerformHapticFeedback(view, 23)
    }
}
