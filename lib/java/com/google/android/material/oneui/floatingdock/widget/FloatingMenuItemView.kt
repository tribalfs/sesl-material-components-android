package com.google.android.material.oneui.floatingdock.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.R
import kotlin.jvm.JvmOverloads

/**
 * A custom [AppCompatImageButton] that represents an item in a floating menu.
 *
 * This view automatically sets its background based on the current theme (light or dark)
 * using predefined ripple drawables.
 *
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view. May be null.
 */
class FloatingMenuItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs) {

    init {
        val bgRes = if (SeslMisc.isLightTheme(context)) {
            R.drawable.sesl_floating_pane_menu_item_background_ripple
        } else {
            R.drawable.sesl_floating_pane_menu_item_background_ripple_dark
        }
        background = context.getDrawable(bgRes)
    }
}