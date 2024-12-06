package com.google.android.material.appbar.model.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.Nullable
import org.jetbrains.annotations.NotNull

abstract class AppBarView @JvmOverloads constructor(
  @NotNull context: Context,
  @Nullable attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    abstract fun updateResource(context: Context)

}
