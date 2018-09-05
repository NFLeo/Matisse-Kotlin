package com.matisse.utils

import android.os.Build

/**
 * Created by Leo on 2018/9/5 on 14:09.
 */
object Platform {

    fun hasKitKat19(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }
}