package com.google.android.material.appbar.model

import android.content.Context
import com.google.android.material.appbar.model.view.AppBarView
import com.google.android.material.appbar.model.view.ViewPagerAppBarView
import kotlin.reflect.KClass


class ViewPagerAppBarModel<T : ViewPagerAppBarView> internal constructor(
    kclazz: KClass<T>,
    context: Context,
    val appBarModels: List<AppBarModel<out AppBarView>>
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