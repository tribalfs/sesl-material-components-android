package com.google.android.material.appbar.model.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.util.theme.SeslThemeResourceHelper
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor
import com.google.android.material.R

//sesl9
/**
 * A subclass of [ViewPagerAppBarView] that customizes the appearance of the ViewPager and page indicators
 * for a white background ("white case") theme.
 *
 * This class applies white-case specific color state lists and drawable tints to the embedded ViewPager
 * and page indicator, ensuring proper contrast and visual consistency when displayed on white or light-themed backgrounds.
 *
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
open class ViewPagerAppBarWhiteCaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPagerAppBarView(context, attrs) {

    private fun getViewPagerBackgroundColorStateList(context: Context): ColorStateList {
        return ColorStateList.valueOf(
            SeslThemeResourceHelper.getColorInt(
                context,
                SeslThemeResourceColor.OpenThemeResourceColor(
                    SeslThemeResourceColor.ThemeResourceColor(
                        R.color.sesl_viewpager_background,
                        R.color.sesl_viewpager_background_dark
                    ),
                    SeslThemeResourceColor.ThemeResourceColor(
                        R.color.sesl_viewpager_background_for_theme
                    )
                )
            )
        )
    }

    private fun getViewPagerIndicatorOffWithWhiteCaseColor(context: Context): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_off_with_white_case,
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off_dark
                ),
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_off_with_white_case_for_theme,
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off_dark_for_theme
                )
            )
        )
    }

    private fun getViewPagerIndicatorOnWithWhiteCaseColor(context: Context): Int {
        return SeslThemeResourceHelper.getColorInt(
            context,
            SeslThemeResourceColor.OpenThemeResourceColor(
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_on_with_white_case
                ),
                SeslThemeResourceColor.ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_on_with_white_case_for_theme
                )
            )
        )
    }

    override fun updateResource(context: Context) {
        adjustViewPagerLayout()

        viewpager?.let {
            it.backgroundTintList = getViewPagerBackgroundColorStateList(context)
            it.adapter?.notifyDataSetChanged()
        }

        val indicator = this@ViewPagerAppBarWhiteCaseView.indicator ?: return

	    indicator.defaultCircle = context.getDrawable(androidx.appcompat.R.drawable.sesl_viewpager_indicator_on_off)?.mutate()?.apply {
            if (this is GradientDrawable) {
	            color = ColorStateList.valueOf(getViewPagerIndicatorOffWithWhiteCaseColor(context))
            } else {
                setTint(getViewPagerIndicatorOffWithWhiteCaseColor(context))
            }
        }

	    indicator.selectCircle = context.getDrawable(androidx.appcompat.R.drawable.sesl_viewpager_indicator_on_off)?.mutate()?.apply {
            if (this is GradientDrawable) {
	            color = ColorStateList.valueOf(getViewPagerIndicatorOnWithWhiteCaseColor(context))
            } else {
                setTint(getViewPagerIndicatorOnWithWhiteCaseColor(context))
            }
        }
    }
}
