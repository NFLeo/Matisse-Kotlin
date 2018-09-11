package com.matisse.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.matisse.MimeTypeManager
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Created by Leo on 2018/8/29 on 15:24.
 */
object PhotoMetadataUtils {
    private val SCHEME_CONTENT = "content"

    private val TAG = PhotoMetadataUtils::class.java.simpleName

    fun getPath(resolver: ContentResolver, uri: Uri): String? {
        if (SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null

            try {
                cursor = resolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
                if (cursor == null || !cursor.moveToFirst()) {
                    return null
                }

                return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
            } finally {
                cursor?.close()
            }
        }

        return uri.path
    }

    fun isAcceptable(context: Context, item: Item): IncapableCause? {
        if (!isSelectableType(context, item)) {
            return IncapableCause(context.getString(R.string.error_file_type))
        }

        if (SelectionSpec.getInstance().filters != null) {
            SelectionSpec.getInstance().filters?.forEach {
                return it.filter(context, item)
            }
        }
        return null
    }

    private fun isSelectableType(context: Context?, item: Item): Boolean {
        if (context == null) {
            return false
        }

        val resolver = context.contentResolver
        for (type in SelectionSpec.getInstance().mimeTypeSet) {
            if (MimeTypeManager.checkType(resolver, item.getContentUri(), type.getValue())) {
                return true
            }
        }
        return false
    }

    fun getBitmapBound(resolver: ContentResolver, uri: Uri): Point {
        var `is`: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            `is` = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(`is`, null, options)
            val width = options.outWidth
            val height = options.outHeight
            return Point(width, height)
        } catch (e: FileNotFoundException) {
            return Point(0, 0)
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    fun getSizeInMB(sizeInBytes: Long): Float {
        val df = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
        df.applyPattern("0.0")
        var result = df.format((sizeInBytes.toFloat() / 1024f / 1024f).toDouble())
        Log.e(TAG, "getSizeInMB: $result")
        result = result.replace(",".toRegex(), ".") // in some case , 0.0 will be 0,0
        return java.lang.Float.valueOf(result)
    }
}