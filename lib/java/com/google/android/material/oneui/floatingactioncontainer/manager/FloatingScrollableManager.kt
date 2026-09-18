package com.google.android.material.oneui.floatingactioncontainer.manager

import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.core.oneui.common.internal.log.debug
import androidx.core.widget.NestedScrollView
import androidx.core.widget.SeslScrollable
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.floatingactioncontainer.FloatingGroupLayout
import com.google.android.material.oneui.floatingactioncontainer.FloatingTopLayout.Companion.UNSPECIFIED_BOUND
import com.google.android.material.oneui.floatingactioncontainer.manager.adapter.FloatingNestedScrollViewAdapter
import com.google.android.material.oneui.floatingactioncontainer.manager.adapter.FloatingRecyclerviewAdapter
import com.google.android.material.oneui.floatingactioncontainer.manager.adapter.FloatingScrollableAdapter
import java.lang.ref.WeakReference
import java.util.WeakHashMap


/**
 * Coordinates scroll offsets, available scroll bounds, hover paddings, GoToTop button offsets,
 * and scrollbar positions between floating layouts and [SeslScrollable] views.
 */
class FloatingScrollableManager private constructor(
    private var floatingSupportableAdapter: FloatingScrollableAdapter
) : FloatingScrollableAdapter, MaterialLogTag {

    private var appbarOffset: Int = 0
    private var bottomBarAnimOffset: Int = 0
    private var bottomLayoutHeight: Int = 0
    private var collapsedAppbarHeight: Int = 0
    private var floatingToolbarLayoutHeight: Int = 0
	var manageFadingEdgeBottomOffset: Boolean = true
	var manageGoToTopOffset: Boolean = false

	var windowInsetBottom: Int = 0
		set(value) {
			field = value
			setFloatingBottomLayoutHeight(UNSET)
			updateGoToTopOffset()
			(getFloatingScrollable() as? View)?.requestLayout()
		}

    private var availRectList: MutableMap<String, Pair<Int, Int>> = LinkedHashMap()

    override val logTag: String = "FloatingScrollableManager"

    override fun getFloatingScrollable(): SeslScrollable? {
        return floatingSupportableAdapter.getFloatingScrollable()
    }

    override fun addSeslScrollableListener(listener: SeslScrollableListener) {
        floatingSupportableAdapter.addSeslScrollableListener(listener)
    }

    override fun removeSeslScrollableListener(listener: SeslScrollableListener) {
        floatingSupportableAdapter.removeSeslScrollableListener(listener)
    }

    override fun dispose() {
        floatingSupportableAdapter.dispose()
    }

    override fun isInScreen(availHeight: Int, appbarOffset: Int, appbarRange: Int): Boolean {
        return floatingSupportableAdapter.isInScreen(availHeight, appbarOffset, appbarRange)
    }

	@Throws(Exception::class)
    fun setFloatingScrollableView(floatingScrollableView: SeslScrollable) {
        debug("setFloatingScrollableView floatingScrollableView=${floatingScrollableView.hashCode()}")
        if (floatingSupportableAdapter.getFloatingScrollable() == floatingScrollableView) {
            return
        }
        val adapter: FloatingScrollableAdapter = when (floatingScrollableView) {
            is RecyclerView -> FloatingRecyclerviewAdapter(floatingScrollableView)
            is NestedScrollView -> FloatingNestedScrollViewAdapter(floatingScrollableView)
            else -> throw IllegalArgumentException("setFloatingScrollableView type error $floatingScrollableView")
        }
        debug("setFloatingScrollableView change Adapter=$adapter")
        floatingSupportableAdapter = adapter
    }

	fun getFloatingScrollableView(): View? = getFloatingScrollable() as? View

    fun setAppBarOffset(offset: Int) {
        appbarOffset = offset
        updateGoToTopOffset()
    }

    fun setBottomBarAnimOffset(offset: Int) {
        bottomBarAnimOffset = offset
        updateGoToTopOffset()
    }

    private fun updateGoToTopOffset() {
        if (!manageGoToTopOffset) {
            debug("updateGoToTopOffset off")
            return
        }
        val scrollable = getFloatingScrollable() ?: return
        val targetPadding = scrollable.seslGetGoToTopDefaultBottomPadding() + appbarOffset + bottomBarAnimOffset + windowInsetBottom
        if (targetPadding != scrollable.seslGetGoToTopBottomPadding()) {
            scrollable.seslSetGoToTopBottomPadding(targetPadding)
        }
    }

    fun setFloatingBottomLayoutHeight(height: Int) {
        if (height != UNSET) {
	        bottomLayoutHeight = height
        }
        val hoverBottomPadding = bottomLayoutHeight + windowInsetBottom
        getFloatingScrollable()?.seslSetHoverBottomPadding(hoverBottomPadding)
        updateScrollBarBottomOffset()
    }

    fun setFloatingToolbarLayoutHeight(height: Int) {
        val h = height.coerceAtLeast(0)
        if (floatingToolbarLayoutHeight == h) return
        floatingToolbarLayoutHeight = h
        getFloatingScrollable()?.seslSetHoverTopPadding(floatingToolbarLayoutHeight)
    }

    fun setScrollBarTopOffset(collapsedHeight: Int, offset: Int) {
        this.collapsedAppbarHeight = collapsedHeight
        getFloatingScrollable()?.seslSetScrollBarTopOffset(offset)
        updateScrollBarBottomOffset()
    }

    private fun updateScrollBarBottomOffset() {
        getFloatingScrollable()?.seslSetScrollBarBottomOffset(collapsedAppbarHeight + bottomLayoutHeight + windowInsetBottom)
    }

    fun setFadingEdgeBottomOffset(offset: Int) {
        if (!manageFadingEdgeBottomOffset) return
        getFloatingScrollable()?.seslSetBottomScrollOffset(offset)
    }

    fun applyAvailBound(topSize: Int, bottomSize: Int, tag: String, dispatchFakeScroll: Boolean = true) {
        if (availRectList.containsKey(tag) && topSize == UNSPECIFIED_BOUND && bottomSize == UNSPECIFIED_BOUND) {
            availRectList.remove(tag)
        } else {
            availRectList[tag] = Pair(topSize, bottomSize)
        }
        var sumTop = 0
        var sumBottom = 0
        for ((_, pair) in availRectList) {
            if (pair.first != UNSPECIFIED_BOUND) sumTop += pair.first
            if (pair.second != UNSPECIFIED_BOUND) sumBottom += pair.second
        }
        val bottomPadding = sumBottom + windowInsetBottom
        saveLastAvailRectInfo(sumTop, bottomPadding)
        val scrollable = getFloatingScrollable() ?: return
        val view = scrollable as? View ?: return
        val rect = Rect(0, sumTop, view.measuredWidth, view.measuredHeight - bottomPadding)
        scrollable.seslSetAvailableBounds(rect, dispatchFakeScroll)
    }

    private fun saveLastAvailRectInfo(top: Int, bottom: Int) {
        lastAvailBounds = Rect(0, top, 0, bottom)
    }

    fun forceTopFadingEdgeClamped(height: Int) {
        getFloatingScrollable()?.seslForceTopFadingEdgeClamped(height)
    }

    fun showGoToTop() {
        getFloatingScrollable()?.seslShowGoToTop()
    }

    fun hideGoToTop() {
        getFloatingScrollable()?.seslHideGoToTop()
    }

    fun setGoToTopSuppressed(suppressed: Boolean) {
        getFloatingScrollable()?.seslSetGoToTopSuppressed(suppressed)
    }

    fun invalidateScrollableView() {
        getFloatingScrollableView()?.invalidate()
    }

    companion object {
		const val UNSET = -1
        private var lastAvailBounds: Rect? = null
        private val lock = Any()
        private val instanceCollection = WeakHashMap<SeslScrollable, FloatingScrollableManager>()
        private val clientLayout = WeakHashMap<FloatingGroupLayout, WeakReference<SeslScrollable>>()

        val instance = FloatingScrollableManager(object : FloatingScrollableAdapter {
            override fun addSeslScrollableListener(listener: SeslScrollableListener) {}
            override fun removeSeslScrollableListener(listener: SeslScrollableListener) {}
            override fun dispose() {}
        })

        fun getInstance(
            floatingLayout: FloatingGroupLayout,
            scrollableView: SeslScrollable?,
            scrollableAdapter: FloatingScrollableAdapter? = null
        ): FloatingScrollableManager {
            if (scrollableView == null) return instance
            synchronized(lock) {
                clientLayout[floatingLayout] = WeakReference(scrollableView)
                var manager = instanceCollection[scrollableView]
                if (manager == null) {
                    val adapter = scrollableAdapter ?: when (scrollableView) {
                        is RecyclerView -> FloatingRecyclerviewAdapter(scrollableView)
                        is NestedScrollView -> FloatingNestedScrollViewAdapter(scrollableView)
                        else -> null
                    }
                    manager = if (adapter != null) FloatingScrollableManager(adapter) else instance
                    instanceCollection[scrollableView] = manager
                }
                return manager
            }
        }

        fun getInstance(
            floatingLayout: FloatingGroupLayout,
            scrollableAdapter: FloatingScrollableAdapter?
        ): FloatingScrollableManager {
            val scrollable = scrollableAdapter?.getFloatingScrollable()
            if (scrollable == null) {
                Log.w("FloatingScrollManager", "getInstance fail. using default (adapter scrollable is null), scrollableAdapter=$scrollableAdapter")
                return instance
            }
            return getInstance(floatingLayout, scrollable, scrollableAdapter)
        }

        fun clearInstance(floatingLayout: FloatingGroupLayout, scrollableView: SeslScrollable?) {
            synchronized(lock) {
                clientLayout.remove(floatingLayout)
                val entries = clientLayout.entries.iterator()
                while (entries.hasNext()) {
                    if (entries.next().value.get() == null) {
                        entries.remove()
                    }
                }
                if (scrollableView != null) {
                    var inUse = false
                    for (ref in clientLayout.values) {
                        if (ref.get() == scrollableView) {
                            inUse = true
                            break
                        }
                    }
                    if (!inUse) {
                        val manager = instanceCollection.remove(scrollableView)
                        manager?.dispose()
                    }
                }
            }
        }

        fun clearAllInstances() {
            synchronized(lock) {
                for (manager in instanceCollection.values) {
                    manager.dispose()
                }
                instanceCollection.clear()
                clientLayout.clear()
            }
        }
    }
}
