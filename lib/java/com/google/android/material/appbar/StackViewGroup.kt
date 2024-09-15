package com.google.android.material.appbar

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Stack

class StackViewGroup(@JvmField val rootView: FrameLayout) {
    private val sceneStack: SceneStack<ViewGroup?>

    class SceneStack<T : View?> : Stack<T>() {
        @Synchronized
        override fun pop(): T {
            val result: T
            try {
                result = super.pop()
                if (size > 0) {
                    peek()!!.visibility = VISIBLE
                }
            } catch (th: Throwable) {
                throw th
            }
            return result
        }


        override fun push(item: T): T {
            if (size > 0) {
                val peek = peek()
                peek!!.visibility = GONE
            }
            val push = super.push(item)
            return push as T
        }


        override fun remove(element: T?): Boolean {
            val remove = super.remove(element)
            if (size > 0) {
                peek()!!.visibility = VISIBLE
            }
            return remove
        }
    }

    init {
        sceneStack = SceneStack()
    }

    fun pop(): ViewGroup? {
        val pop = if (!sceneStack.isEmpty()) sceneStack.pop() else return null
        rootView.removeView(pop)
        return pop
    }

    fun push(viewGroup: ViewGroup) {
        sceneStack.push(viewGroup)
        rootView.addView(viewGroup)
    }

    fun remove(viewGroup: ViewGroup) {
        sceneStack.remove(viewGroup)
        rootView.removeView(viewGroup)
    }
}
