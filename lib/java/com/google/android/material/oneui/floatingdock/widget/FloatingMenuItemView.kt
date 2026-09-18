package com.google.android.material.oneui.floatingdock.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.R

class FloatingMenuItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    init {
        val bgRes = if (SeslMisc.isLightTheme(context)) {
            R.drawable.sesl_floating_pane_menu_item_background_ripple
        } else {
            R.drawable.sesl_floating_pane_menu_item_background_ripple_dark
        }
        setBackground(context.getDrawable(bgRes))
    }

    override fun getAccessibilityClassName(): CharSequence {
        return Button::class.java.name
    }
}
