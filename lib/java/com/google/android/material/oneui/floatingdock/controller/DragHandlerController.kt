package com.google.android.material.oneui.floatingdock.controller

import android.content.Context
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.util.Consumer
import androidx.core.util.Predicate
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.util.getCurrentLayoutBounds
import com.google.android.material.oneui.floatingdock.FloatingDockLogTag
import com.google.android.material.oneui.floatingdock.util.HapticFeedbackHelper
import com.google.android.material.oneui.floatingdock.util.isPhoneSize

class DragHandlerController(
    val view: View,
    private val onTouchListener: View.OnTouchListener,
    private val onClickListener: View.OnClickListener,
    private val onLongPress: Consumer<Unit>
) : FloatingDockLogTag {

    private var actionDownStart: Boolean = false
    private val context: Context = view.context
    override val logTag: String = "DragHandlerController"

    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                onTouchListener.onTouch(view, e)
                HapticFeedbackHelper.onTap(view)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                onLongPress.accept(Unit)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onClickListener.onClick(view)
                return true
            }
        })
    }

    init {
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isClickable = !context.isPhoneSize()
                info.isLongClickable = true
            }
        })
    }

    private fun onEventInternal(event: MotionEvent, trigger: Predicate<MotionEvent>): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            actionDownStart = isInArea(view, event)
        }
        val z = actionDownStart && trigger.test(event)
        if (event.action != MotionEvent.ACTION_CANCEL && event.action != MotionEvent.ACTION_UP) {
            return z
        }
        actionDownStart = false
        return z
    }

    private fun onInterceptTouchTrigger(event: MotionEvent): Boolean {
        val z = gestureDetector.onTouchEvent(event)
        debug("onInterceptTouchEventInternal result=$z")
        return z
    }

    private fun onTouchEventTrigger(event: MotionEvent): Boolean {
        val z = gestureDetector.onTouchEvent(event)
        val result = event.action != MotionEvent.ACTION_DOWN && z
        debug("onTouchEventInternal value=$z result=$result")
        return result
    }

    fun isInArea(view: View, ev: MotionEvent): Boolean {
        val rect = Rect()
        view.getCurrentLayoutBounds(rect)
        return rect.contains(ev.x.toInt(), ev.y.toInt())
    }

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return onEventInternal(event) { ev -> onInterceptTouchTrigger(ev) }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return onEventInternal(event) { ev -> onTouchEventTrigger(ev) }
    }
}
