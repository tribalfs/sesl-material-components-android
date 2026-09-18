package com.google.android.material.appbar.model

import android.content.Context
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.model.view.AppBarView
import com.google.android.material.appbar.model.view.ViewPagerAppBarView
import kotlin.reflect.KClass

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * This model class extends [AppBarModel] and provides functionality to manage the data and
 * behavior of a [ViewPagerAppBarView] or its subclass.
 *
 * This class holds a list of [AppBarModel] instances or its subclass, each representing a page within
 * the [ViewPagerAppBarView]. It provides functionality to set the title, define action buttons,
 * and handle click events for both the close button and the action buttons.
 *
 * Use the [Builder] class to construct instances of [ViewPagerAppBarModel].
 *
 * @param T The type of the [ViewPagerAppBarView] associated with this model.
 * @property kclazz The [KClass] of the [ViewPagerAppBarView].
 * @property context The [Context] in which the AppBar is used.
 * @property appBarModels A list of [AppBarModel] instances representing the pages in the [ViewPager2].
 */
open class ViewPagerAppBarModel<T : ViewPagerAppBarView> (
    kclazz: KClass<T>,
    context: Context,
    private val appBarModels: List<AppBarModel<out AppBarView>>
) : AppBarModel<T>(kclazz, context) {

    override fun init(moduleView: T): T {
        return moduleView
    }

    class Builder(private val context: Context,
                  private var appBarModels: List<AppBarModel<out AppBarView>> = emptyList()) {

        fun setModels(models: List<AppBarModel<out AppBarView>>): Builder {
            appBarModels = models
            return this
        }

        fun build(): ViewPagerAppBarModel<ViewPagerAppBarView> {
            return ViewPagerAppBarModel(
                ViewPagerAppBarView::class,
                this.context,
                appBarModels
            )
        }

    }
}