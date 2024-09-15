package com.google.android.material.appbar.model


data class ButtonListModel(
    @JvmField
    val buttonStyle: ButtonStyle,
    @JvmField
    val buttonModels: List<ButtonModel>
)
