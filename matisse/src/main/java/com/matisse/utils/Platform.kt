package com.matisse.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Created by Leo on 2018/9/5 on 14:09.
 */
object Platform {

    fun hasKitKat19() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    fun hasKitO26() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun beforeAndroidTen() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun getPackageName(context: Context?): String? {
        if (context == null) return ""

        val manager = context.packageManager

        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            return info.packageName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return ""
    }

    fun isClassExists(classFullName: String): Boolean {
        return try {
            Class.forName(classFullName)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}