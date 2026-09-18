package com.google.android.material.oneui.floatingactioncontainer.manager.adapter

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.oneui.common.internal.log.debug
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.SeslScrollable
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.floatingactioncontainer.manager.SeslScrollableListener
import java.lang.ref.WeakReference

/**
 * [FloatingScrollableAdapter] implementation wrapping a [NestedScrollView] to bridge
 * scroll change events to [SeslScrollableListener] instances.
 *
 * @param view Target [NestedScrollView] instance.
 */
class FloatingNestedScrollViewAdapter(view: NestedScrollView) : FloatingScrollableAdapter, MaterialLogTag {
    private val viewRef: WeakReference<NestedScrollView> = WeakReference(view)
    val scrollableListener: MutableList<SeslScrollableListener> = ArrayList()

    private val onScrollChangeListener = NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
        val dy = scrollY - oldScrollY
        for (listener in scrollableListener) {
            listener.onScrolled(v, 0, dy)
        }
    }

    init {
        debug("init $this, view=$view")
        view.setOnScrollChangeListener(onScrollChangeListener)
    }

    override val logTag: String = "FloatingNestedScrollViewAdapter"

    override fun getFloatingScrollable(): SeslScrollable? {
        return viewRef.get() as? SeslScrollable
    }

    override fun addSeslScrollableListener(listener: SeslScrollableListener) {
        scrollableListener.add(listener)
    }

    override fun removeSeslScrollableListener(listener: SeslScrollableListener) {
        scrollableListener.remove(listener)
    }

    override fun dispose() {
        debug("dispose $this")
        viewRef.get()?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        scrollableListener.clear()
    }

    override fun isInScreen(availHeight: Int, appbarOffset: Int, appbarRange: Int): Boolean {
        val nestedScrollView = (getFloatingScrollable() as? NestedScrollView) ?: return false
        if (nestedScrollView.childCount == 0) return true
        val firstChild = nestedScrollView.getChildAt(0)
        if (firstChild !is ViewGroup) return false
	    if (firstChild.childCount < 2) return false

        var lastIndex = firstChild.childCount - 1
        var lastChild: View? = null
        while (lastIndex >= 0) {
            val v = firstChild.getChildAt(lastIndex)
            if (v != null && v.isVisible) {
                lastChild = v
                break
            }
            lastIndex--
        }
        if (lastChild == null) return false

        if (isEmptyView(lastChild)) {
            var prevChild: View? = null
            for (i in lastIndex - 1 downTo 0) {
                val v = firstChild.getChildAt(i)
                if (v != null && v.isVisible) {
                    prevChild = v
                    break
                }
            }
            lastChild = prevChild
        }
        if (lastChild == null) return false

        if (lastChild.bottom <= firstChild.top + firstChild.height) {
            val availableBounds: Rect? = nestedScrollView.seslGetAvailableBounds()
            val availBottom = (availableBounds?.bottom ?: 0) + nestedScrollView.paddingBottom
            if (firstChild.top + lastChild.bottom <= availBottom + appbarRange) {
                return true
            }
        }
        return false
    }
}
