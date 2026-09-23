package com.google.android.material.oneui.common.internal

import android.os.Build
import android.util.Log
import androidx.core.oneui.common.internal.log.LogTag
import java.util.Locale

interface MaterialLogTag : LogTag {
    override val prefix: String
        get() = ""

    override val version: String
        get() = "[sesl9-material:1.0.47]"

    override val isDebugTouch: Boolean
        get() = false

    override val isDebugVersion: Boolean
        get() = Build.TYPE.lowercase(Locale.ROOT)
            .let { it == "eng" || it == "userdebug" }
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
