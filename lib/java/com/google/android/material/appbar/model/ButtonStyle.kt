package com.google.android.material.appbar.model

import androidx.annotation.StyleRes

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * Data class containing the style resources for a button or set of buttons
 * in a AppBar suggestion view both for light and dark themes.
 *
 * @property defStyleRes The style resource ID for the button in a light theme.
 * @property defStyleResDark The style resource ID for the button in a dark theme.
 */
data class ButtonStyle(
    @StyleRes val defStyleRes: Int,
    @StyleRes val defStyleResDark: Int
)
