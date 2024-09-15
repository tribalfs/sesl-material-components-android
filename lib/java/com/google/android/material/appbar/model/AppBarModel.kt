package com.google.android.material.appbar.model

import android.content.Context
import android.view.View
import com.google.android.material.appbar.model.view.AppBarView
import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass


open class AppBarModel<T : AppBarView?>(
    @NotNull private val kclazz: KClass<Any>,
    @NotNull private val context: Context
) {

    fun interface OnClickListener {
        fun onClick(view: View, appBarModel: AppBarModel<out AppBarView?>)
    }

    @NotNull
    fun create(): T {
        kclazz::class
        throw Error("Kotlin reflection implementation is not found at runtime. Make sure you have kotlin-reflect.jar in the classpath")
    }

    @NotNull
    open fun init(@NotNull moduleView: T): T {
        return moduleView
    }
}


