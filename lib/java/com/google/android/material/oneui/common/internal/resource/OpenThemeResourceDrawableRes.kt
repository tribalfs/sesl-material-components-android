package com.google.android.material.oneui.common.internal.resource

import android.content.Context
import androidx.appcompat.util.SeslMisc

class ThemeResourceDrawableRes(
    val lightRes: Int,
    val darkRes: Int
)

class OpenThemeResourceDrawableRes(
    val mainThemeRes: ThemeResourceDrawableRes,
    val openThemeRes: ThemeResourceDrawableRes
) {
    fun getResource(context: Context): Int {
        val light = SeslMisc.isLightTheme(context)
        return if (light) mainThemeRes.lightRes else mainThemeRes.darkRes
    }
}
