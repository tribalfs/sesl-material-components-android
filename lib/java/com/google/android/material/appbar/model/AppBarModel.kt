package com.google.android.material.appbar.model

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.appbar.model.view.AppBarView
import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * The base model class for an AppBar suggestion.
 *
 * This class is responsible for creating and initializing an AppBarView.
 * It also provides an interface for handling click events on the AppBar.
 *
 * @param T The type of AppBarView that this model will create.
 * @property kclazz The KClass of the AppBarView.
 * @property context The context to use when creating the AppBarView.
 */
open class AppBarModel<T : AppBarView>(
    @NotNull private val kclazz: KClass<T>,
    @NotNull private val context: Context
) {

    /**
     * Interface definition for a callback to be invoked when a button in
     * an [AppBarView] is clicked.
     */
    fun interface OnClickListener {
        fun onClick(view: View, module: AppBarModel<out AppBarView>)
    }

    /**
     * Creates a new instance of the [AppBarView].
     *
     * This method calls the AppBarView class's `(Context, AttributeSet)` constructor with the
     * context and null as arguments. It uses Java reflection: Kotlin reflection
     * (`KClass.constructors`) needs kotlin-reflect at runtime, which apps may leave out.
     *
     * @return A new instance of the [AppBarView].
     */
    @NotNull
    open fun create(): T = init(
        //custom - Java reflection; sesl9's kclazz.constructors.first().call(context, null) needs kotlin-reflect
        kclazz.java.getConstructor(Context::class.java, AttributeSet::class.java).newInstance(this.context, null)//custom
    )

    /**
     * Initializes the given [AppBarView].
     *
     * This method is called after the AppBarView has been created intended to
     * be overridden by subclasses to perform any additional initialization that is required.
     *
     * @param moduleView The AppBarView to initialize.
     * @return The initialized AppBarView.
     */
    @NotNull
    open fun init(@NotNull moduleView: T): T = moduleView
}


