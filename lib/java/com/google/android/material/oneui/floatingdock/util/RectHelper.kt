package com.google.android.material.oneui.floatingdock.util

import android.graphics.Rect

/**
 * Moves this [Rect] inside the given [moveableArea] if it's out of bounds,
 * and intersects it with [moveableArea]. Returns true if the rect was moved.
 */
fun Rect.moveInsideAndIntersect(moveableArea: Rect): Boolean {
    var dx = 0
    var dy = 0

    if (left < moveableArea.left) {
        dx = moveableArea.left - left
    } else if (right > moveableArea.right) {
        dx = moveableArea.right - right
    }

    if (top < moveableArea.top) {
        dy = moveableArea.top - top
    } else if (bottom > moveableArea.bottom) {
        dy = moveableArea.bottom - bottom
    }

    if (dx == 0 && dy == 0) return false

    offset(dx, dy)
    setIntersect(this, moveableArea)
    return true
}