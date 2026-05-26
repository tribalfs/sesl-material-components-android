package com.google.android.material.appbar

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Property
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.FrameLayout
import com.google.android.material.R
import java.util.Stack

/*
 * Original code by Samsung, all rights reserved to the original author.
 */
//Added in sesl7
class StackViewGroup(val rootView: FrameLayout) {

    class SceneStack<T : View> : Stack<T>() {
        override fun pop(): T {
            return super.pop().also {
                if (isNotEmpty()) peek().visibility = View.VISIBLE
            }
        }

        override fun push(item: T): T {
            if (isNotEmpty()) peek().visibility = View.GONE
            return super.push(item)
        }
    }

    private val sceneStack = SceneStack<ViewGroup>()
    private val showDuration = 200L
    private val hideDuration = 100L
    private val showHideInterpolator: Interpolator = AnimationUtils.loadInterpolator(rootView.context, R.interpolator.sesl_interpolator_0_0_1_1)
    private val showAnimator: ObjectAnimator
    private val hideAnimator: ObjectAnimator
    private val showHideAnimator: AnimatorSet

    init {
        val property: Property<View, Float> = View.ALPHA
        showAnimator = ObjectAnimator.ofFloat(null as View?, property, 0.0f, 1.0f).apply {
            interpolator = showHideInterpolator
            duration = showDuration
            startDelay = hideDuration
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    (target as? View)?.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        hideAnimator = ObjectAnimator.ofFloat(null as View?, property, 1.0f, 0.0f).apply {
            interpolator = showHideInterpolator
            duration = hideDuration
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    (target as? View)?.let { rootView.removeView(it) }
                    target = null // prevent leaked strong ref to detached SuggestAppBarView
                }

                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        showHideAnimator = AnimatorSet().apply {
            playTogether(hideAnimator, showAnimator)
        }
    }

    fun pop(): ViewGroup? {
        return if (sceneStack.isNotEmpty()) {
            sceneStack.pop().also { rootView.removeView(it) }
        } else null
    }

    fun push(child: ViewGroup?) {
        child?.let {
            sceneStack.push(it)
            rootView.addView(it)
        }
    }

    fun remove(view: ViewGroup?) {
        sceneStack.remove(view)
        view?.let {
            if (showHideAnimator.isRunning) {
                showHideAnimator.cancel()
            }
            hideAnimator.target = it
            showAnimator.target = sceneStack.peekOrNull()
            showHideAnimator.start()
        }
    }

    private fun <T> Stack<T>.peekOrNull(): T? = if (isNotEmpty()) peek() else null
}