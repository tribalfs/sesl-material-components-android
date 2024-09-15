package com.google.android.material.appbar.model

data class ButtonModel @JvmOverloads constructor(
    @JvmField
    val text: String? = null,
    @JvmField
    val clickListener: AppBarModel.OnClickListener? = null,
    @JvmField
    val contentDescription: String? = null
)
