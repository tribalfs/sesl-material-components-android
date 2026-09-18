package com.google.android.material.oneui.common.internal.animation

import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.util.Collections

class RectFSpringAnimation(
    rectF: RectF,
    val ratio: Float = 1.0f
) : BaseAnimation {

    private val childrenAnim: List<SpringAnimation>
    private val endListeners: MutableList<() -> Unit>
    private val handler: Handler
    private var isUpdatePosted: Boolean = false
    private val pendingRectF: RectF = RectF()
    private val rectBottomAnim: SpringAnimation
    private val rectLeftAnim: SpringAnimation
    private val rectRightAnim: SpringAnimation
    private val rectTopAnim: SpringAnimation
    private val startListeners: MutableList<() -> Unit>
    private val updateListeners: MutableList<(RectF) -> Unit>

    companion object {
        const val TAG = "RectFAnimation"
    }

    init {
        val leftProp = object : FloatPropertyCompat<RectF>("rectLeft") {
            override fun getValue(newValue: RectF): Float = ratio * newValue.left
            override fun setValue(newValue: RectF, value: Float) {
                newValue.left = value / ratio
                onUpdate(newValue)
            }
        }
        val animLeft = SpringAnimation(rectF, leftProp)
        val springLeft = SpringForce(ratio * rectF.left).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animLeft.spring = springLeft
        animLeft.addEndListener { _, _, _, _ -> maybeEnd() }
        rectLeftAnim = animLeft

        val topProp = object : FloatPropertyCompat<RectF>("rectTop") {
            override fun getValue(newValue: RectF): Float = ratio * newValue.top
            override fun setValue(newValue: RectF, value: Float) {
                newValue.top = value / ratio
                onUpdate(newValue)
            }
        }
        val animTop = SpringAnimation(rectF, topProp)
        val springTop = SpringForce(ratio * rectF.top).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animTop.spring = springTop
        animTop.addEndListener { _, _, _, _ -> maybeEnd() }
        rectTopAnim = animTop

        val rightProp = object : FloatPropertyCompat<RectF>("rectRight") {
            override fun getValue(newValue: RectF): Float = ratio * newValue.right
            override fun setValue(newValue: RectF, value: Float) {
                newValue.right = value / ratio
                onUpdate(newValue)
            }
        }
        val animRight = SpringAnimation(rectF, rightProp)
        val springRight = SpringForce(ratio * rectF.right).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animRight.spring = springRight
        animRight.addEndListener { _, _, _, _ -> maybeEnd() }
        rectRightAnim = animRight

        val bottomProp = object : FloatPropertyCompat<RectF>("rectBottom") {
            override fun getValue(newValue: RectF): Float = ratio * newValue.bottom
            override fun setValue(newValue: RectF, value: Float) {
                newValue.bottom = value / ratio
                onUpdate(newValue)
            }
        }
        val animBottom = SpringAnimation(rectF, bottomProp)
        val springBottom = SpringForce(ratio * rectF.bottom).apply {
            dampingRatio = 1.0f
            stiffness = 1500.0f
        }
        animBottom.spring = springBottom
        animBottom.addEndListener { _, _, _, _ -> maybeEnd() }
        rectBottomAnim = animBottom

        childrenAnim = listOf(rectLeftAnim, rectTopAnim, rectRightAnim, rectBottomAnim)
        updateListeners = mutableListOf()
        handler = Handler(Looper.getMainLooper())
        startListeners = Collections.synchronizedList(mutableListOf())
        endListeners = Collections.synchronizedList(mutableListOf())
    }

    private fun isEnded(): Boolean {
        return childrenAnim.none { it.isRunning }
    }

    private fun maybeEnd() {
        if (isEnded()) {
            endListeners.forEach { it.invoke() }
        }
    }

    private fun onStart() {
        startListeners.forEach { it.invoke() }
    }

    private fun onUpdate(newRectF: RectF) {
        pendingRectF.set(newRectF)
        if (isUpdatePosted) {
            return
        }
        isUpdatePosted = true
        handler.post {
            updateListeners.forEach { it.invoke(pendingRectF) }
            isUpdatePosted = false
        }
    }

    fun addEndListener(onEnd: () -> Unit) {
        endListeners.add(onEnd)
    }

    fun addStartListener(onStart: () -> Unit) {
        startListeners.add(onStart)
    }

    fun addUpdateListener(function: (RectF) -> Unit) {
        updateListeners.add(function)
    }

    fun animateToFinalPosition(finalPosition: RectF) {
        Log.d(TAG, "animateToFinalPosition $finalPosition")
        onStart()
        rectLeftAnim.animateToFinalPosition(finalPosition.left * ratio)
        rectTopAnim.animateToFinalPosition(finalPosition.top * ratio)
        rectRightAnim.animateToFinalPosition(finalPosition.right * ratio)
        rectBottomAnim.animateToFinalPosition(finalPosition.bottom * ratio)
    }

    override fun cancel() {
        childrenAnim.forEach { it.cancel() }
    }

    override fun isRunning(): Boolean {
        return childrenAnim.any { it.isRunning }
    }

    fun setSpringForce(dampingRatio: Float?, stiffness: Float?) {
        childrenAnim.forEach { anim ->
            Log.d(TAG, "setSpringForce ${anim.isRunning} $dampingRatio $stiffness")
            val spring = anim.spring
            if (stiffness != null) {
                spring.stiffness = stiffness
            }
            if (dampingRatio != null) {
                spring.dampingRatio = dampingRatio
            }
        }
    }

    override fun skipToEnd() {
        Log.d(TAG, "skipToEnd")
        childrenAnim.forEach { it.skipToEnd() }
    }
}
