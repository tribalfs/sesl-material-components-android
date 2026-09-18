package com.google.android.material.appbar.model.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.util.theme.SeslThemeResourceHelper
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor
import androidx.core.content.ContextCompat
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R
import com.google.android.material.oneui.common.internal.util.MaxFontScaleRatio
import com.google.android.material.oneui.common.internal.util.checkMaxFontScale

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * A view representing a single suggestion or action page within a [ViewPager2], extending [SuggestAppBarView].
 * This class provides functionality for handling title, close button, and bottom layout elements.
 * It also manages resource updates based on the current theme (light or dark) and applies font scaling for accessibility.
 *
 * @param context The Context the view is running in, through which it can
 *                access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
open class SuggestAppBarItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SuggestAppBarView(context, attrs) {

    var rootView: ViewGroup? = null

    override fun inflate() {
        val viewInflate = LayoutInflater.from(context).inflate(R.layout.sesl_app_bar_suggest_in_viewpager, this as ViewGroup, false)
        val viewGroup = viewInflate as? ViewGroup ?: return

        val viewGroup2 = viewGroup.findViewById<ViewGroup>(R.id.sesl_appbar_suggest_in_viewpager)
        viewGroup2.setBackgroundResource(R.drawable.sesl_viewpager_item_background)
        this.rootView = viewGroup2

        topImageView = viewGroup.findViewById(R.id.suggest_app_bar_top_image)
        titleView = viewGroup.findViewById(R.id.suggest_app_bar_title)
        subTitleView = viewGroup.findViewById(R.id.suggest_app_bar_sub_title)

        val imageButton2: ImageButton? = viewGroup.findViewById(R.id.suggest_app_bar_close)
        if (imageButton2 != null) {
            SeslViewReflector.semSetHoverPopupType(imageButton2, SeslHoverPopupWindowReflector.getField_TYPE_NONE())
        }
        close = imageButton2
        bottomLayout = viewGroup.findViewById(R.id.suggest_app_bar_bottom_layout)

        updateResource(context)
        addView(viewGroup)
    }

    override fun updateResource(context: Context) {
        super.updateResource(context)
        val isLight = SeslMisc.isLightTheme(context)
        rootView?.let {
            val colorRes = if (isLight) R.color.sesl_viewpager_item_background else R.color.sesl_viewpager_item_background_dark
            it.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        }
        titleView?.checkMaxFontScale(R.dimen.sesl_appbar_suggest_title_text_size, MaxFontScaleRatio.SMALL)
        subTitleView?.checkMaxFontScale(R.dimen.sesl_appbar_suggest_sub_title_size, MaxFontScaleRatio.SMALL)
        updateButtons(buttons)
    }

    protected fun updateButtons(buttons: List<Button>) {
        for (button in buttons) {
            updateButton(button)
        }
    }

    private fun updateButton(button: Button) {
        button.setTextColor(getButtonTextColor())
        button.checkMaxFontScale(R.dimen.sesl_appbar_button_text_size, MaxFontScaleRatio.MEDIUM)
    }

    private fun getButtonTextColor(): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(R.color.sesl_button_text_color, R.color.sesl_button_text_color_dark),
                SeslThemeResourceColor.ThemeResourceColor(R.color.sesl_button_text_color, R.color.sesl_button_text_color_dark_for_theme)
            )
        )
    }

    private fun getSuggestButtonTextColor(): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(R.color.sesl_suggest_button_text_color, R.color.sesl_suggest_button_text_color_dark),
                SeslThemeResourceColor.ThemeResourceColor(R.color.sesl_suggest_button_text_color, R.color.sesl_suggest_button_text_color_dark_for_theme)
            )
        )
    }
}
