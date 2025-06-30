package com.google.android.material.oneui.floatingdock.controller

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.util.Consumer
import androidx.core.util.Predicate
import com.google.android.material.oneui.floatingdock.util.HapticFeedbackHelper
import com.google.android.material.oneui.floatingdock.util.getCurrentLayoutBounds

/**
 * Controller class for handling drag events on a view.
 *
 * This class uses a [GestureDetector] to detect various touch events like down, long press, and
 * single tap up. It then invokes the provided listeners for these events.
 *
 * It also provides methods to check if a touch event is within the bounds of the view and to
 * handle intercepted touch events.
 *
 * @param view The view to handle drag events for.
 * @param onTouchListener The listener for touch events.
 * @param onClickListener The listener for click events.
 * @param onLongPress The listener for long press events.
 */
class DragHandlerController(
    val view: View,
    private val onTouchListener: View.OnTouchListener,
    private val onClickListener: View.OnClickListener,
    private val onLongPress: Consumer<Unit>
) {
    companion object {
        private const val TAG = "DragHandlerController"
    }

    private val context: Context = view.context
    private var actionDownStart: Boolean = false

    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                onTouchListener.onTouch(view, e)
                HapticFeedbackHelper.onTap(view)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                onLongPress.accept(Unit)
                HapticFeedbackHelper.onLongPress(view)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onClickListener.onClick(view)
                return true
            }
        })
    }

    private fun onEventInternal(event: MotionEvent, trigger: Predicate<MotionEvent>): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            actionDownStart = isInArea(view, event)
        }
        val result = actionDownStart && trigger.test(event)
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            actionDownStart = false
        }
        return result
    }

    private fun onInterceptTouchTrigger(event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        Log.d(TAG, "onInterceptTouchEventInternal result=$result")
        return result
    }

    private fun onTouchEventTrigger(event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        Log.d(TAG, "onTouchEventInternal value= result=$result ${event.action != MotionEvent.ACTION_DOWN && result}")
        return event.action != MotionEvent.ACTION_DOWN && result
    }

    fun isInArea(view: View, ev: MotionEvent): Boolean {
        val rect = Rect()
        view.getCurrentLayoutBounds(rect)
        return rect.contains(ev.x.toInt(), ev.y.toInt())
    }

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return onEventInternal(event, Predicate { onInterceptTouchTrigger(it) })
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return onEventInternal(event, Predicate { onTouchEventTrigger(it) })
    }
}