@file:Suppress("MemberVisibilityCanBePrivate")

package com.google.android.material.appbar.model.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.util.theme.SeslThemeResourceHelper.getColorInt
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor.OpenThemeResourceColor
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor.ThemeResourceColor
import androidx.appcompat.widget.SeslIndicator
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R
import com.google.android.material.appbar.SeslAppBarHelper
import com.google.android.material.appbar.internal.MaxWidthLinearLayout

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * A base view class that extends [AppBarView] and embeds a [ViewPager2] to display multiple swipeable suggestion pages.
 *
 * Each page within the ViewPager represents a distinct suggestion, which can be individually configured and dismissed.
 * The view’s components—including the ViewPager and its indicator—are managed by the associated
 * [ViewPagerAppBarModel][com.google.android.material.appbar.model.ViewPagerAppBarModel].
 *
 * The background and indicator colors of the ViewPager can be customized via theme attributes to match the app’s appearance.
 *
 * @param context The context in which the view is running, providing access to resources, themes, and more.
 * @param attributeSet The set of attributes from XML used to inflate the view, or null if created programmatically.
 *
 * @see AppBarView
 * @see ViewPager2
 * @see SeslIndicator
 */
@RequiresApi(23)
open class ViewPagerAppBarView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : AppBarView(context, attributeSet) {

    var bottomLayout: ViewGroup? = null
    var indicator: SeslIndicator? = null
    var viewPagerContainer: ViewGroup? = null
    var viewPagerParent: ViewGroup? = null
    var viewpager: ViewPager2? = null

    init {
        this.inflate()
    }

    override fun inflate() {
        val context = context

        val appBarViewPagerVG = LayoutInflater.from(context).inflate(
            R.layout.sesl_app_bar_viewpager, this, false
        ) as? ViewGroup ?: return

        appBarViewPagerVG.apply {
            viewpager = findViewById(R.id.app_bar_viewpager)
            viewPagerContainer = findViewById(R.id.app_bar_viewpager_container)
            viewPagerParent = findViewById(R.id.app_bar_viewpager_parent)
            bottomLayout = findViewById(R.id.bottom_layout)
        }

        indicator = SeslIndicator(context, null).apply {
            setOnItemClickListener { _, i -> viewpager?.setCurrentItem(i, true) }
        }

        viewpager?.let { vp ->
            vp.seslSetSuggestionPaging(true)
            val bgDrawable = vp.context.getDrawable(R.drawable.sesl_viewpager_background)
            vp.background = bgDrawable?.mutate()

            ViewCompat.setAccessibilityDelegate(vp, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val adapter = viewpager?.adapter
                    if (adapter != null && adapter.itemCount == 1 && viewpager?.currentItem == 0) {
                        info.className = ""
                    }
                }
            })
        }

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        bottomLayout?.addView(indicator, layoutParams)

        updateResource(context)
        addView(appBarViewPagerVG)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        adjustViewPagerLayout()
    }

    protected open fun adjustViewPagerLayout() {
        val suggestionAppBarSize = SeslAppBarHelper.getSuggestionAppBarSize(context, this)
        val cardWidth = suggestionAppBarSize.first
        val cardHeight = suggestionAppBarSize.second.let { if (it == 0) MATCH_PARENT else it }

        viewpager?.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, cardHeight)
        (viewPagerParent as? MaxWidthLinearLayout)?.maxWidth = cardWidth
    }

    /**
     * Updates the resources of the ViewPager and its indicator based on the current theme.
     *
     * This method sets the background tint of the ViewPager and updates the drawables for the
     * "on" and "off" states of the indicator circles. The colors used are retrieved from
     * theme attributes.
     *
     * @param context The Context used to access theme resources and drawables.
     */
    override fun updateResource(context: Context) {
        viewpager?.let { vp ->
            adjustViewPagerLayout()
            vp.backgroundTintList = getViewPagerBackgroundColorStateList(context)
            vp.adapter?.notifyDataSetChanged()
        }

        val indicator = indicator ?: return

        val offStateDrawable =
            context.getDrawable(androidx.appcompat.R.drawable.sesl_viewpager_indicator_on_off)?.mutate()?.apply {
                if (this is GradientDrawable) {
                    setColor(ColorStateList.valueOf(getViewPagerIndicatorOffColor(context)))
                } else {
                    setTint(getViewPagerIndicatorOffColor(context))
                }
            }
        indicator.defaultCircle = offStateDrawable

        val onStateDrawable =
            context.getDrawable(androidx.appcompat.R.drawable.sesl_viewpager_indicator_on_off)?.mutate()?.apply {
                if (this is GradientDrawable) {
                    setColor(ColorStateList.valueOf(getViewPagerIndicatorOnColor(context)))
                } else {
                    setTint(getViewPagerIndicatorOnColor(context))
                }
            }
        indicator.selectCircle = onStateDrawable
    }

    private fun getViewPagerBackgroundColorStateList(context: Context): ColorStateList =
        ColorStateList.valueOf(
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

    private fun getViewPagerIndicatorOffColor(context: Context): Int =
        getColorInt(
            context,
            OpenThemeResourceColor(
                ThemeResourceColor(
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off,
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off_dark
                ),
                ThemeResourceColor(
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off_for_theme,
                    androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_off_dark_for_theme
                )
            )
        )

    private fun getViewPagerIndicatorOnColor(context: Context): Int =
        getColorInt(
            context,
            OpenThemeResourceColor(
                ThemeResourceColor(androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_on),
                ThemeResourceColor(androidx.appcompat.R.color.sesl_appbar_viewpager_indicator_on_for_theme)
            )
        )
}
