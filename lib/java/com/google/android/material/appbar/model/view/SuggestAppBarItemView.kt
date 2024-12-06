package com.google.android.material.appbar.model.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R

@RequiresApi(23)
open class SuggestAppBarItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : SuggestAppBarView(context, attributeSet) {

    private var mRootView: ViewGroup? = null

    init {
        inflate()
    }

    private fun inflate() {
        val context = context

        val appBarSuggestInViewPager = LayoutInflater.from(context)
            .inflate(R.layout.sesl_app_bar_suggest_in_viewpager, this, false) as? ViewGroup
            ?: return

        appBarSuggestInViewPager.apply {
            mRootView = findViewById(R.id.sesl_appbar_suggest_in_viewpager)
            titleView = findViewById(R.id.suggest_app_bar_title)
            closeButton = findViewById(R.id.suggest_app_bar_close)
            bottomLayout = findViewById(R.id.suggest_app_bar_bottom_layout)
        }

        updateResource(context)

        addView(appBarSuggestInViewPager)
    }

    fun setRootView(viewGroup: ViewGroup?) {
        mRootView = viewGroup
    }

    override fun getRootView(): ViewGroup? = mRootView

    override fun updateResource(context: Context) {
        super.updateResource(context)
        mRootView?.setBackgroundResource(
            if (SeslMisc.isLightTheme(context)) R.drawable.sesl_viewpager_item_background
            else R.drawable.sesl_viewpager_item_background_dark
        )
    }
}
