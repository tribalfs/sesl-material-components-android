package com.google.android.material.appbar

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.R

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

//Added in sesl7
class SeslAppBarHelper {

    companion object {
        const val TAG: String = "SeslAppBarHelper"

	    //Sesl9
        private const val COVER_SCREEN_HEIGHT_MAX = 751
        private const val COVER_SCREEN_HEIGHT_MIN = 441
        private const val FLIP_COVER_SCREEN_HEIGHT_MAX = 562
        private const val LARGE_SCREEN_SMALLEST_SCREEN_WIDTH = 600

        private const val SUGGESTION_CARD_HEIGHT_DP_142 = 142
        private const val SUGGESTION_CARD_HEIGHT_DP_172 = 172
        private const val SUGGESTION_CARD_HEIGHT_DP_188 = 188
        private const val SUGGESTION_CARD_MARGIN_HORIZONTAL_DP = 10
        private const val SUGGESTION_CARD_WIDTH_DP_272 = 272
        private const val SUGGESTION_CARD_WIDTH_DP_320 = 320
        private const val SUGGESTION_CARD_WIDTH_DP_360 = 360
        private const val SUGGESTION_CARD_WIDTH_DP_408 = 408
        private const val SUGGESTION_CARD_WIDTH_DP_420 = 420
        private const val SUGGESTION_CARD_WIDTH_DP_468 = 468
        private const val SUGGESTION_HEIGHT_DP_1055 = 1055
        private const val SUGGESTION_HEIGHT_DP_870 = 870
        private const val SUGGESTION_HEIGHT_DP_874 = 874
        private const val SUGGESTION_WIDTH_DP_0_291_MAX = 291
        private const val SUGGESTION_WIDTH_DP_1056_1580_MAX = 1580
        private const val SUGGESTION_WIDTH_DP_292_799_MAX = 799
        private const val SUGGESTION_WIDTH_DP_800_875_MAX = 875
        private const val SUGGESTION_WIDTH_DP_876_1055_MAX = 1055
	    //sesl9

        @JvmStatic
        fun getAppBarProPortion(context: Context): Float {
            val resources = context.resources
            val configuration: Configuration = resources.configuration
            if (Build.VERSION.SDK_INT < 35) {
                return ResourcesCompat.getFloat(resources, R.dimen.sesl_appbar_height_proportion)
            }
            val fullWindowHeightDp = getFullWindowHeightDp(context)
            Log.d(TAG, "orientation=${configuration.orientation}, fullWindowHeightDp=$fullWindowHeightDp")
            return when {
                fullWindowHeightDp < COVER_SCREEN_HEIGHT_MIN -> 0.0f
                fullWindowHeightDp < 545.0f -> 0.59f
                fullWindowHeightDp < 580.0f -> 0.51f
                fullWindowHeightDp < 616.0f -> 0.48f
                fullWindowHeightDp < 655.0f -> 0.44f
                fullWindowHeightDp < 700.0f -> 0.41f
                fullWindowHeightDp < 750.0f -> 0.38f
                fullWindowHeightDp < 780.0f -> 0.36f
                fullWindowHeightDp < 830.0f -> 0.34f
                fullWindowHeightDp < 960.0f -> 0.32f
                fullWindowHeightDp < 1200.0f -> 0.29f
                fullWindowHeightDp < 1400.0f -> 0.25f
                fullWindowHeightDp < 1690.0f -> 0.22f
                else -> 0.2f
            }//sesl9
        }
	    @RequiresApi(34)
        @JvmStatic
        fun getFullWindowHeightDp(context: Context): Float {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = context.resources.displayMetrics
            val currentWindowMetrics = windowManager.currentWindowMetrics
            val bounds = currentWindowMetrics.bounds
            val height = bounds.height()
            val deriveDimension: Float = TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_DIP, height.toFloat(), displayMetrics)
            Log.d(TAG, "fullWindowHeight(dp)=$deriveDimension, fullWindowHeight(px)=$height, screenHeightDp=${context.resources.configuration.screenHeightDp}")
            return deriveDimension
        }

        @JvmStatic
        fun getScreenHeight(view: View): Int {
            val context: Context = view.context
            if (Build.VERSION.SDK_INT < 35) {
                return context.resources.displayMetrics.heightPixels
            }
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val currentWindowMetrics = windowManager.currentWindowMetrics
            val insets = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
            val height = currentWindowMetrics.bounds.height() - insets.top - insets.bottom
            Log.d(TAG, "screenHeight(px)=$height, status=${insets.top}, navi=${insets.bottom}")
            return height
        }

	    //Sesl9
        @JvmOverloads
        @JvmStatic
        fun isDefaultCollapsedCondition(context: Context, force: Boolean? = false): Boolean {
            val sw = context.resources.configuration.smallestScreenWidthDp
            val heightDp = context.resources.displayMetrics.heightPixels.pxToDp(context)
            if (force == true) {
                return heightDp <= FLIP_COVER_SCREEN_HEIGHT_MAX
            }
            return sw >= LARGE_SCREEN_SMALLEST_SCREEN_WIDTH || heightDp <= COVER_SCREEN_HEIGHT_MAX
        }

        @JvmStatic
        fun getSuggestionAppBarSize(context: Context, view: View): Pair<Int, Int> {
            val widthPx = context.resources.displayMetrics.widthPixels
            val screenHeightPx = getScreenHeight(view)
            val widthDp = widthPx.pxToDp(context)
            val heightDp = screenHeightPx.pxToDp(context)

            val pair: Pair<Int, Int> = when {
                widthDp <= SUGGESTION_WIDTH_DP_0_291_MAX -> {
                    Pair((widthDp - SUGGESTION_CARD_MARGIN_HORIZONTAL_DP * 2).coerceAtLeast(0), SUGGESTION_CARD_HEIGHT_DP_142)
                }
                widthDp <= SUGGESTION_WIDTH_DP_292_799_MAX -> {
                    Pair(SUGGESTION_CARD_WIDTH_DP_272, SUGGESTION_CARD_HEIGHT_DP_142)
                }
                widthDp <= SUGGESTION_WIDTH_DP_800_875_MAX -> {
                    Pair(SUGGESTION_CARD_WIDTH_DP_320, SUGGESTION_CARD_HEIGHT_DP_142)
                }
                widthDp <= SUGGESTION_WIDTH_DP_876_1055_MAX -> {
                    if (heightDp < SUGGESTION_HEIGHT_DP_870) Pair(SUGGESTION_CARD_WIDTH_DP_320, SUGGESTION_CARD_HEIGHT_DP_142)
                    else Pair(SUGGESTION_CARD_WIDTH_DP_360, SUGGESTION_CARD_HEIGHT_DP_172)
                }
                widthDp > SUGGESTION_WIDTH_DP_1056_1580_MAX -> {
                    if (heightDp < SUGGESTION_HEIGHT_DP_1055) Pair(SUGGESTION_CARD_WIDTH_DP_408, SUGGESTION_CARD_HEIGHT_DP_172)
                    else Pair(SUGGESTION_CARD_WIDTH_DP_468, SUGGESTION_CARD_HEIGHT_DP_188)
                }
                heightDp <= SUGGESTION_HEIGHT_DP_874 -> {
                    Pair(SUGGESTION_CARD_WIDTH_DP_320, SUGGESTION_CARD_HEIGHT_DP_142)
                }
                else -> {
                    if (heightDp < SUGGESTION_HEIGHT_DP_1055) Pair(SUGGESTION_CARD_WIDTH_DP_408, SUGGESTION_CARD_HEIGHT_DP_172)
                    else Pair(SUGGESTION_CARD_WIDTH_DP_420, SUGGESTION_CARD_HEIGHT_DP_188)
                }
            }
            Log.d(TAG, "getSuggestionAppBarSize: widthDp=$widthDp, heightDp=$heightDp -> cardDp=(${pair.first}, ${pair.second})")
            return Pair(pair.first.dpToPx(context), pair.second.dpToPx(context))
        }

        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }

	    private fun Int.pxToDp(context: Context): Int {
            return (this / context.resources.displayMetrics.density).toInt()
        }
	    //sesl9
    }
}
