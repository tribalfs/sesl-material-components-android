package com.google.android.material.appbar.model.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.appcompat.util.theme.SeslThemeResourceHelper
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor
import androidx.appcompat.util.theme.resource.SeslThemeResourceDrawable
import com.google.android.material.R

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * A subclass of [SuggestAppBarItemView] that customizes the appearance of the item for a white background.
 */
open class SuggestAppBarItemWhiteCaseView(
    context: Context
) : SuggestAppBarItemView(context, null) {

    override fun updateResource(context: Context) {
        super.updateResource(context)
        rootView?.let {
            it.backgroundTintList = ColorStateList.valueOf(getViewPagerItemBackgroundWhiteWhiteCaseColor(context))
        }
        titleView?.setTextColor(getSuggestTitleWithWhiteCaseColor(context))
        subTitleView?.setTextColor(getSuggestSubTitleWithWhiteCaseColor(context))
        close?.background = getCloseDrawable(context)
        updateButtons(buttons)
    }

    private fun getCloseDrawable(context: Context): Drawable? {
        return SeslThemeResourceHelper.getDrawable(
            context,
            SeslThemeResourceDrawable.OpenThemeResourceDrawable(
                SeslThemeResourceDrawable.ThemeResourceDrawable(
                    R.drawable.sesl_close_button_recoil_background_with_white_case,
                    R.drawable.sesl_close_button_recoil_background_dark
                ),
                SeslThemeResourceDrawable.ThemeResourceDrawable(
                    R.drawable.sesl_close_button_recoil_background_with_white_case_for_theme,
                    R.drawable.sesl_close_button_recoil_background_dark_for_theme
                )
            )
        )
    }

    private fun getSuggestButtonTextColorWithWhiteCase(): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_suggest_button_text_color_with_white_case,
                    R.color.sesl_suggest_button_text_color_dark
                ),
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_suggest_button_text_color_with_white_case,
                    R.color.sesl_suggest_button_text_color_dark_for_theme
                )
            )
        )
    }

    private fun getSuggestSubTitleWithWhiteCaseColor(context: Context): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.ThemeResourceColor(
                R.color.sesl_appbar_suggest_title_with_white_case,
                R.color.sesl_appbar_suggest_title_dark
            )
        )
    }

    private fun getSuggestTitleWithWhiteCaseColor(context: Context): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.ThemeResourceColor(
                R.color.sesl_appbar_suggest_title_with_white_case,
                R.color.sesl_appbar_suggest_title_dark
            )
        )
    }

    private fun getViewPagerItemBackgroundWhiteWhiteCaseColor(context: Context): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_viewpager_item_background_with_white_case,
                    R.color.sesl_viewpager_item_background_dark
                ),
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_viewpager_item_background_with_white_case_for_theme,
                    R.color.sesl_viewpager_item_background_dark
                )
            )
        )
    }
}
