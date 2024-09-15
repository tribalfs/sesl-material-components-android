package com.google.android.material.appbar.model.view

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R

@RequiresApi(23)
class SuggestAppBarItemWhiteCaseView(
    context: Context
) : SuggestAppBarItemView(context, null) {

    override fun updateResource(context: Context) {
        super.updateResource(context)

        var isLightTheme: Boolean? = null

        rootView?.apply {
            isLightTheme = SeslMisc.isLightTheme(context)
            setBackgroundResource(
                if (isLightTheme!!) {
                    R.drawable.sesl_viewpager_item_background_with_white_case
                } else R.drawable.sesl_viewpager_item_background_dark
            )
        }

        titleView?.apply {
            isLightTheme = isLightTheme ?: SeslMisc.isLightTheme(context)
            setTextColor(resources.getColor(
                if (isLightTheme!!) R.color.sesl_appbar_suggest_title_with_white_case
                else R.color.sesl_appbar_suggest_title_dark,
                context.theme))
        }

        closeButton?.apply {
            isLightTheme = isLightTheme ?: SeslMisc.isLightTheme(context)
            setBackgroundResource(getCloseRes(isLightTheme!!))
        }
    }

    private fun getCloseRes(isLightTheme: Boolean): Int {
        return if (Build.VERSION.SDK_INT >= 29) {
            if (isLightTheme) R.drawable.sesl_close_button_recoil_background_with_white_case
            else R.drawable.sesl_close_button_recoil_background_dark
        }else {
            if (isLightTheme) R.drawable.sesl_ic_close_with_white_case
            else R.drawable.sesl_ic_close_dark
        }
    }
}
