package com.google.android.material.appbar.model.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.Nullable
import org.jetbrains.annotations.NotNull

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */

/**
 * Abstract class for use as the custom view designed to show contextual suggestions or actions in an
 * expanded AppBar temporarily replacing the AppBar's expanded title and subtitle.
 *
 * This class provides a foundation for different types of AppBar suggestion view implementations.
 * Its subclasses are responsible for implementing the [inflate] and [updateResource] methods
 * to define their specific layout and resource handling.
 *
 * @param context The Context the view is running in, through which it can
 *                access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
abstract class AppBarView @JvmOverloads constructor(
  @NotNull context: Context,
  @Nullable attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

  abstract fun inflate()
  abstract fun updateResource(context: Context)

}
