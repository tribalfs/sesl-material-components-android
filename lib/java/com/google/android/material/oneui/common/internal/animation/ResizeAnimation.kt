package com.google.android.material.oneui.common.internal.animation

import android.graphics.RectF

class ResizeAnimation(val ratio: Float = 1.0f) : BaseAnimation {
    private val anim: RectFSpringAnimation
    private val rectF: RectF = RectF()
    var updater: (RectF) -> Unit = {}

    init {
        val rectFSpringAnimation = RectFSpringAnimation(rectF, ratio)
        rectFSpringAnimation.setSpringForce(1.0f, 361.0f)
        rectFSpringAnimation.addUpdateListener { rect ->
            if (!this.rectF.isEmpty) {
                this.updater.invoke(rect)
            }
        }
        this.anim = rectFSpringAnimation
    }

    fun animateToFinalPosition(finalPosition: RectF) {
        this.anim.animateToFinalPosition(finalPosition)
    }

    override fun cancel() {
        this.anim.cancel()
    }

    fun init(init: RectF) {
        this.rectF.set(init)
    }

    override fun isRunning(): Boolean {
        return this.anim.isRunning()
    }

    fun setSpringForce(dampingRatio: Float?, stiffness: Float?) {
        this.anim.setSpringForce(dampingRatio, stiffness)
    }

    override fun skipToEnd() {
        this.anim.skipToEnd()
    }
}
