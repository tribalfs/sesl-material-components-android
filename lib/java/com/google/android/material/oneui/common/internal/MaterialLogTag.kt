package com.google.android.material.oneui.common.internal

import android.util.Log
import androidx.core.oneui.common.internal.log.LogTag

interface MaterialLogTag : LogTag {
    override val prefix: String
        get() = ""

    override val version: String
        get() = "[sesl9-material:1.0.47]"

    override val isDebugTouch: Boolean
        get() = false

    override val isDebugVersion: Boolean
        get() = true
}

fun LogTag.debug(msg: String) {
    Log.d(logTag, msg)
}

fun LogTag.debugTouch(msg: String) {
    Log.d(logTag, msg)
}

fun LogTag.info(msg: String) {
    Log.i(logTag, msg)
}

fun LogTag.warn(msg: String) {
    Log.w(logTag, msg)
}

fun LogTag.error(msg: String) {
    Log.e(logTag, msg)
}
