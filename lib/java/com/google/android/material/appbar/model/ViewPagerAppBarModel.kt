package com.google.android.material.appbar.model

import android.content.Context
import com.google.android.material.appbar.model.view.AppBarView
import com.google.android.material.appbar.model.view.ViewPagerAppBarView
import kotlin.reflect.KClass


class ViewPagerAppBarModel<T : ViewPagerAppBarView?>(
    kclazz: KClass<Any>,
    context: Context,
    var appBarModels: List<AppBarModel<out AppBarView?>> = emptyList()
) : AppBarModel<T>(kclazz, context) {

    inner class Builder(private val context: Context) {

        fun build(): ViewPagerAppBarModel<ViewPagerAppBarView> {
            return ViewPagerAppBarModel(
                ViewPagerAppBarView::class as KClass<Any>,
                this.context,
                appBarModels
            )
        }

        fun setModels(models: List<AppBarModel<out AppBarView?>>): Builder {
            appBarModels = models
            return this
        }
    }
}