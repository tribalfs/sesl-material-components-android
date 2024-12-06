package com.google.android.material.appbar.model.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.util.theme.SeslThemeResourceColor.OpenThemeResourceColor
import androidx.appcompat.util.theme.SeslThemeResourceColor.ThemeResourceColor
import androidx.appcompat.util.theme.SeslThemeResourceHelper
import androidx.appcompat.util.theme.SeslThemeResourceHelper.getColorInt
import androidx.appcompat.widget.SeslIndicator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.jvm.internal.Intrinsics

@RequiresApi(23)
abstract class BasicViewPagerAppBarWhiteCaseView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
): BasicViewPagerAppBarView(context, attributeSet) {

        private fun getViewPagerBackgroundColorStateList(context: Context): ColorStateList {
        val bgColor = ColorStateList.valueOf(
            getColorInt(
                context,
                OpenThemeResourceColor(
                    ThemeResourceColor(
                        R.color.sesl_viewpager_background,
                        R.color.sesl_viewpager_background_dark
                    ),
                    ThemeResourceColor(R.color.sesl_viewpager_background_for_theme)
                )
            )
        )
        return bgColor
    }

    private fun getViewPagerIndicatorOffWithWhiteCaseColor(context: Context): Int {
        return getColorInt(
            context,
            OpenThemeResourceColor(
                ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_off_with_white_case,
                    R.color.sesl_appbar_viewpager_indicator_off_dark
                ),
                ThemeResourceColor(
                    R.color.sesl_appbar_viewpager_indicator_off_with_white_case_for_theme,
                    R.color.sesl_appbar_viewpager_indicator_off_dark_for_theme
                )
            )
        )
    }

    private fun getViewPagerIndicatorOnWithWhiteCaseColor(context: Context): Int {
        return getColorInt(
            context,
            OpenThemeResourceColor(
                ThemeResourceColor(R.color.sesl_appbar_viewpager_indicator_on_with_white_case),
                ThemeResourceColor(R.color.sesl_appbar_viewpager_indicator_on_with_white_case_for_theme)
            )
        )
    }

    override fun updateResource(context: Context) {
        viewpager?.backgroundTintList = getViewPagerBackgroundColorStateList(context)

        val indicator = indicator ?: return
        val indicatorDrawableResId = R.drawable.sesl_viewpager_indicator_on_off

        val offStateDrawable = context.getDrawable(indicatorDrawableResId)?.mutate()?.apply {
            setTint(getViewPagerIndicatorOffWithWhiteCaseColor(context))
        }
        indicator.defaultCircle = offStateDrawable

        val onStateDrawable = context.getDrawable(indicatorDrawableResId)?.mutate()?.apply {
            setTint(getViewPagerIndicatorOnWithWhiteCaseColor(context))
        }
        indicator.selectCircle = onStateDrawable
    }
}