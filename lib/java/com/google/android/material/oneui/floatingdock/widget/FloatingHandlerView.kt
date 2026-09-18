package com.google.android.material.oneui.floatingdock.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView

class FloatingHandlerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    override fun getAccessibilityClassName(): CharSequence {
        return Button::class.java.name
    }
}
