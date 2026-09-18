package com.google.android.material.appbar.model

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * Data class for button properties of an AppBar suggestion.
 *
 * @param text The text to be displayed on the button.
 * @param clickListener The listener to be invoked when the button is clicked.
 * @param contentDescription The content description for the button, used for accessibility.
 */
data class ButtonModel @JvmOverloads constructor(
    val text: String? = null,
    val clickListener: AppBarModel.OnClickListener? = null,
    val contentDescription: String? = null
)
