package com.google.android.material.util

import android.widget.TextView
import androidx.annotation.DimenRes

enum class MaxFontScaleRatio(val ratio: Float) {
    SMALL(1.0f),
    MEDIUM(1.15f),
    LARGE(1.3f),
    EXTRA_LARGE(1.5f),
    HUGE(1.7f),
    EXTRA_HUGE(2.0f)
}

fun checkMaxFontScale(textView: TextView, @DimenRes baseSizeDp: Int, maxFontScale: MaxFontScaleRatio) {
    textView.setTextSize(0, textView.resources.getDimensionPixelSize(baseSizeDp) *
            textView.resources.configuration.fontScale.coerceAtMost(maxFontScale.ratio)
    )
}
