package com.matisse.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Created by Leo on 2018/9/5 on 14:09.
 */
object Platform {

    fun hasKitKat19(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    fun hasKitO26(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun getPackageName(context: Context?): String? {
        if (context == null) {
            return ""
        }

        val manager = context.packageManager

        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            return info.packageName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return ""
    }
}