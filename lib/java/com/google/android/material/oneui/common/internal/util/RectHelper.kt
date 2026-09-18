package com.google.android.material.oneui.common.internal.util

import android.graphics.Rect

fun Rect.moveInsideAndIntersect(moveableArea: Rect): Boolean {
    val i = left
    val i5 = moveableArea.left
    val i6 = if (i >= i5 && right <= moveableArea.right) 0 else i5 - i
    val i7 = top
    val i8 = moveableArea.top
    val i9 = if (i7 >= i8 && bottom <= moveableArea.bottom) 0 else i8 - i7
    if (i6 == 0 && i9 == 0) {
        return false
    }
    offset(i6, i9)
    setIntersect(this, moveableArea)
    return true
}
