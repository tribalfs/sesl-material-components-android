package com.google.android.material.appbar.model.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * Abstract base class that extends [ViewPagerAppBarView] to provide enhanced management of paged suggestions or actions within an expanded AppBar.
 *
 * This class adds the following key features:
 * - Indicator management: Handles adding, removing, and initializing the indicator that reflects the current page in the [ViewPager2].
 * - Animated item removal: Supports removing items (pages) with optional animation. If animated, the view transitions to the next available page before removing the current one.
 * - Indicator synchronization: Keeps the indicator state in sync with the [ViewPager2]'s current page, even during animated item removals.
 *
 * Subclasses must implement the [removeItem] method to define how items are actually removed from the underlying data set and view.
 *
 * @param context The context in which the view is running, providing access to resources, themes, and more.
 * @param attributeSet The set of XML attributes used to inflate the view, or null if created programmatically.
 */
@RequiresApi(23)
abstract class BasicViewPagerAppBarView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : ViewPagerAppBarView(context, attributeSet) {

    private val deleteAlphaAnimator: ValueAnimator
    private val deleteAlphaDuration = 150L
    private var deleteAnimator: AnimatorSet? = null
    private var deleteScaleAnimator: ValueAnimator? = null
    private val deleteScaleDuration = 350L
    private val deleteScaleX: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_X, 1.0f, 0.9f)
    private val deleteScaleY: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_Y , 1.0f, 0.9f)
    private var isDeleteAnimatorRunning = false
    private val pageChangeCallback: ViewPager2.OnPageChangeCallback

    init {
        deleteAlphaAnimator = ObjectAnimator.ofFloat(null, ALPHA, 0.0f).apply {
            setDuration(deleteAlphaDuration)
            interpolator = AnimationUtils.loadInterpolator(context,
	            androidx.appcompat.R.interpolator.sesl_interpolator_0_0_1_1)
        }

        pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (this@BasicViewPagerAppBarView.isDeleteAnimatorRunning) return
                this@BasicViewPagerAppBarView.indicator?.selectedPosition = position
            }
        }
    }

    private fun internalRemoveItem(index: Int) {
        removeItem(index)
        removeIndicator(index)
    }

    fun moveNextAndRemove(viewPager: ViewPager2, index: Int) {
        if (index < 0) return
        val adapter = viewPager.adapter ?: return
        if (index >= adapter.itemCount) return

        if (index != viewPager.currentItem) {
            removeItem(index)
            return
        }

        val itemCount = adapter.itemCount
        val nextItem = if (index == itemCount - 1) {
            index - 1
        } else if (index < itemCount) {
            index + 1
        } else index

        isDeleteAnimatorRunning = true
        viewPager.apply {
            setCurrentItem(nextItem, true)
            postDelayed({
                isDeleteAnimatorRunning = false
                removeItem(index)
            }, 250L)
        }
    }

    fun addIndicator() = indicator?.addIndicator()

    fun initIndicator(count: Int) {
	    if (count > 1) { for (i in 0 until count) addIndicator() }
        viewpager?.registerOnPageChangeCallback(pageChangeCallback)
    }

    fun removeIndicator(position: Int) {
        if (indicator != null) {
            indicator!!.removeIndicator(position)
            val viewPager = viewpager ?: return
            val adapter = viewPager.adapter ?: return
            if (adapter.itemCount != 1) return
            indicator!!.removeIndicator(position)
        }
    }

    abstract fun removeItem(index: Int)

    fun removeItem(index: Int, animate: Boolean) {
        if (!animate) {
            internalRemoveItem(index)
            return
        }

        if (index < 0) return
        val viewpager = viewpager ?: return
        val adapter = viewpager.adapter ?: return
        if (index >= adapter.itemCount || viewpager.childCount < 0) return

        val recyclerView = (viewpager.getChildAt(0) as? RecyclerView) ?: return
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
        if (viewHolder == null) {
            internalRemoveItem(index)
            return
        }

        val itemView = viewHolder.itemView

        if (deleteAnimator == null) {
            if (deleteScaleAnimator == null) {
                deleteScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(itemView, deleteScaleX, deleteScaleY).apply {
                    setDuration(deleteScaleDuration)
                    interpolator = AnimationUtils.loadInterpolator(context, androidx.appcompat.R.interpolator.sesl_interpolator_22_25_0_1)
                }
            }
            deleteAnimator = AnimatorSet().apply { playTogether(deleteScaleAnimator, deleteAlphaAnimator) }
        }

        deleteAlphaAnimator.apply {
            removeAllListeners()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animator: Animator) = Unit
                override fun onAnimationCancel(animator: Animator) = Unit
                override fun onAnimationStart(animator: Animator) = Unit

                override fun onAnimationEnd(animator: Animator) {
                    moveNextAndRemove(viewpager, index)
                    removeIndicator(index)
                }
            })
        }

        deleteAnimator!!.apply {
            setTarget(itemView)
            start()
        }
    }
}
