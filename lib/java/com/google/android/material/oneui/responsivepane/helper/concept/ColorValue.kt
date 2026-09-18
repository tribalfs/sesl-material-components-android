package com.google.android.material.oneui.responsivepane.helper.concept

data class ColorValue(
    val alpha: Float,
    val color: Int
) {
    override fun toString(): String {
        return "ColorValue(alpha=$alpha, color=${Integer.toHexString(color)})"
    }
}
