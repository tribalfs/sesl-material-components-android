package com.google.android.material.appbar.internal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.R

//sesl9
/**
 * A [LinearLayout] subclass that allows dynamic maximum width constraint on its layout bounds.
 *
 * @property excludePaddingFromMaxWidth Whether horizontal padding ([paddingLeft] and [paddingRight])
 * should be excluded when calculating the effective maximum width.
 * @property maxWidth The maximum width constraint in pixels. If 0 or if the calculated bound is <= 0,
 * no maximum width constraint is enforced.
 */
class MaxWidthLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    val excludePaddingFromMaxWidth: Boolean

    /**
     * The maximum width constraint in pixels. Changing this property triggers a [requestLayout].
     */
    var maxWidth: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaxWidthLinearLayout)
        excludePaddingFromMaxWidth = typedArray.getBoolean(R.styleable.MaxWidthLinearLayout_excludePaddingFromMaxWidth, false)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxW = if (excludePaddingFromMaxWidth) {
            paddingLeft + paddingRight + maxWidth
        } else {
            maxWidth
        }
        if (maxWidth == 0 || maxW <= 0) {
            maxW = Int.MAX_VALUE
        }
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val clampedWidth = widthSize.coerceIn(suggestedMinimumWidth, maxW)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(clampedWidth, View.MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }
}
