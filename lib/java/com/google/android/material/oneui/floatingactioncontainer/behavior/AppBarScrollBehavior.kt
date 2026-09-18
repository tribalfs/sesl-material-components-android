package com.google.android.material.oneui.floatingactioncontainer.behavior

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

/**
 * Base [CoordinatorLayout.Behavior] for floating layouts that react to an [AppBarLayout].
 */
abstract class AppBarScrollBehavior<T : View>(
    context: Context,
    attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<T>(context, attrs) {
    private val tmpRect = Rect()

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: T,
        dependency: View
    ): Boolean = dependency is AppBarLayout

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: T,
        dependency: View
    ): Boolean = dependency is AppBarLayout
}
