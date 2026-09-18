package com.google.android.material.oneui.floatingactioncontainer.manager.adapter

import android.graphics.Rect
import android.view.View
import androidx.core.oneui.common.internal.log.debug
import androidx.core.widget.SeslScrollable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.floatingactioncontainer.manager.SeslScrollableListener
import java.lang.ref.WeakReference

/**
 * [FloatingScrollableAdapter] implementation wrapping a [RecyclerView] to bridge
 * scroll and fast-scroll events to [SeslScrollableListener] instances.
 *
 * @param view Target [RecyclerView] instance.
 */
class FloatingRecyclerviewAdapter(view: RecyclerView) : FloatingScrollableAdapter, MaterialLogTag {
    private val viewRef: WeakReference<RecyclerView> = WeakReference(view)
    val scrollableListener: MutableList<SeslScrollableListener> = ArrayList()
    private var isFastScrolling: Boolean = false

    private val onRecyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (isFastScrolling) return
            for (listener in scrollableListener) {
                listener.onScrolled(recyclerView, dx, dy)
            }
        }
    }

    private val onFastScrollListener = object : RecyclerView.SeslOnFastScrollListener {
        override fun onFastScrollStart() {
            isFastScrolling = true
            getFloatingScrollable()?.let { scrollable ->
                for (listener in scrollableListener) {
                    listener.onFastScrollStart(scrollable as View)
                }
            }
        }

        override fun onFastScrollEnd() {
            isFastScrolling = false
            getFloatingScrollable()?.let { scrollable ->
                for (listener in scrollableListener) {
                    listener.onFastScrollEnd(scrollable as View)
                }
            }
        }
    }

    init {
        debug("init $this, view=$view")
        view.addOnScrollListener(onRecyclerViewScrollListener)
        view.seslSetOnFastScrollListener(onFastScrollListener)
    }

    override val logTag: String = "FloatingRecyclerviewAdapter"

    override fun getFloatingScrollable(): SeslScrollable? {
        val recyclerView = viewRef.get()
        return recyclerView as? SeslScrollable
    }

    override fun addSeslScrollableListener(listener: SeslScrollableListener) {
        scrollableListener.add(listener)
    }

    override fun removeSeslScrollableListener(listener: SeslScrollableListener) {
        scrollableListener.remove(listener)
    }

    override fun dispose() {
        debug("dispose $this")
        val recyclerView = viewRef.get()
        recyclerView?.removeOnScrollListener(onRecyclerViewScrollListener)
        recyclerView?.seslSetOnFastScrollListener(null)
        scrollableListener.clear()
        isFastScrolling = false
    }

    override fun isInScreen(availHeight: Int, appbarOffset: Int, appbarRange: Int): Boolean {
        val recyclerView = (getFloatingScrollable() as? RecyclerView) ?: return false
        val childCount = recyclerView.childCount
        if (childCount <= 0) return true
        val layoutManager = recyclerView.layoutManager ?: return false
        if (recyclerView.height < availHeight) return false
        val lastChild = layoutManager.findViewByPosition(childCount - 1) ?: return false

        val isPartiallyVisible = layoutManager.isViewPartiallyVisible(lastChild, true, true)
        val isZeroHeight = layoutManager.height == 0
        val availableBounds: Rect? = recyclerView.seslGetAvailableBounds()
        val availBottom = availableBounds?.bottom ?: 0

        if (layoutManager is StaggeredGridLayoutManager) {
            if (!isStaggeredChildrenInScreen(recyclerView, layoutManager, isZeroHeight, availBottom, appbarRange)) {
                return false
            }
        }

	    if (isEmptyView(lastChild)) {
		    if ((isPartiallyVisible || isZeroHeight) && lastChild.top <= availBottom + appbarRange) {
			    return true;
		    }
	    } else if ((isPartiallyVisible || isZeroHeight) && lastChild.bottom <= availBottom + appbarRange) {
		    return true;
	    }

        return false
    }

    private fun isStaggeredChildrenInScreen(
        recyclerView: RecyclerView,
        layoutManager: StaggeredGridLayoutManager,
        shouldCheckYPosition: Boolean,
        availBottom: Int,
        appbarRange: Int
    ): Boolean {
        val maxBound = availBottom + appbarRange
        val childCount = recyclerView.childCount
        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            if (!shouldCheckYPosition && !layoutManager.isViewPartiallyVisible(child, true, true)) {
                return false
            }
            if (isEmptyView(child)) {
                if (child.top > maxBound) return false
            } else if (child.bottom > maxBound) {
                return false
            }
        }
        return true
    }
}
