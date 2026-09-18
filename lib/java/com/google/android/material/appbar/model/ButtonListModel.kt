package com.google.android.material.appbar.model

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * This data class encapsulates the style and the individual models for a list of buttons
 * for a [SuggestAppBarView][com.google.android.material.appbar.model.view.SuggestAppBarView]
 * or its subclass.
 *
 * @property buttonStyle The [ButtonStyle] to be applied to all buttons in the list.
 * @property buttonModels A list of [ButtonModel] objects, each representing an individual button.
 */
data class ButtonListModel(
    @JvmField
    val buttonStyle: ButtonStyle,
    @JvmField
    val buttonModels: List<ButtonModel>
)
