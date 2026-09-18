package com.google.android.material.oneui.common.internal.animation

interface BaseAnimation {
    fun isRunning(): Boolean
    fun skipToEnd()
    fun cancel()
}
