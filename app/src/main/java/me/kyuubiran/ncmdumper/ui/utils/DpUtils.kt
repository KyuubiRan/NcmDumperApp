package me.kyuubiran.ncmdumper.ui.utils

import android.content.res.Resources

private val density = Resources.getSystem().displayMetrics.density

val Float.px2dp: Float
    get() = this / density