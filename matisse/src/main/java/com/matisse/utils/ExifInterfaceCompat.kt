package com.matisse.utils

import android.media.ExifInterface

/**
 * Created by liubo on 2018/9/6.
 */
object ExifInterfaceCompat {
    fun newInstance(fileName: String?): ExifInterface? {
        if (fileName == null) {
            throw  NullPointerException("filename should not be null")
        }
        return ExifInterface(fileName)
    }
}