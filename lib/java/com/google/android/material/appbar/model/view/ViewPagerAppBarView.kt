@file:Suppress("MemberVisibilityCanBePrivate")

package com.google.android.material.appbar.model.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.widget.SeslIndicator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R

@RequiresApi(23)
open class ViewPagerAppBarView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : AppBarView(context, attributeSet) {

    @JvmField
    var bottomLayout: ViewGroup? = null

    @JvmField
    var indicator: SeslIndicator? = null

    @JvmField
    var viewpager: ViewPager2? = null

    init {
        inflate()
    }

    private fun inflate() {
        val context = context

        val appBarViewPagerVG = LayoutInflater.from(context).inflate(
            R.layout.sesl_app_bar_viewpager, this, false) as? ViewGroup ?: return

        appBarViewPagerVG.apply {
            viewpager = findViewById(R.id.app_bar_viewpager)
            bottomLayout = findViewById(R.id.bottom_layout)
        }

        indicator = SeslIndicator(context, null).apply {
            setOnItemClickListener { _, i ->
                viewpager?.setCurrentItem(i, true)
            }
        }

        viewpager?.seslSetSuggestionPaging(true)
        bottomLayout?.addView(indicator, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER })
        addView(appBarViewPagerVG)
    }


    override fun updateResource(context: Context) {
        viewpager?.setBackgroundResource(
            if (SeslMisc.isLightTheme(context)) R.drawable.sesl_viewpager_background
            else R.drawable.sesl_viewpager_background_dark
        )
    }
}
