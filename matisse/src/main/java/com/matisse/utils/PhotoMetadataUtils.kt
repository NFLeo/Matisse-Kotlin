package com.matisse.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.icu.text.NumberFormat
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
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
import java.util.*

/**
 * Created by Leo on 2018/8/29 on 15:24.
 */
object PhotoMetadataUtils {
    private val TAG = PhotoMetadataUtils::class.java.simpleName
    private val MAX_WIDTH = 1600
    private val SCHEME_CONTENT = "content"

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

    fun getBitmapSize(uri: Uri?, activity: Activity?): Point {
        val resolver = activity!!.contentResolver
        var imageSize = getBitmapBounds(resolver, uri!!)
        var w = imageSize.x
        var h = imageSize.y
        if (PhotoMetadataUtils.shouldRotate(resolver, uri)) {
            w = imageSize.y
            h = imageSize.x
        }
        if (h == 0) return Point(MAX_WIDTH, MAX_WIDTH)
        var metrics: DisplayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        var screenWidth = metrics.widthPixels
        var screenHeight = metrics.heightPixels
        var widthScale = screenWidth / w
        var heightScale = screenHeight / h
        if (widthScale > heightScale) {
            return Point((w * widthScale), (h * heightScale))
        }

        return Point((w * widthScale), (h * heightScale))

    }

    fun shouldRotate(resolver: ContentResolver, uri: Uri): Boolean {
        var exif: ExifInterface? = null
        try {
            exif = ExifInterfaceCompat.newInstance(getPath(resolver, uri)!!)
        } catch (e: IOException) {
            Log.e(TAG, "could not read exif info of the image: $uri")
            return false
        }
        var orientation = exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
        return orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
    }

    fun getBitmapBounds(resolver: ContentResolver?, uri: Uri): Point {
        var inStream: InputStream? = null
        try {
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inStream = resolver!!.openInputStream(uri)
            BitmapFactory.decodeStream(inStream, null, options)
            val width = options.outWidth
            val height = options.outHeight
            return Point(width, height)
        } catch (e: FileNotFoundException) {
            return Point(0, 0)
        } finally {
            inStream == null ?: try {
                inStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

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


    fun getSizeInMB(sizeInBytes: Long): Float {
        val df = java.text.NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
        df.applyPattern("0.00")
        var result = df.format(sizeInBytes.toFloat() / 1024 / 1024)
        result = result.replace(",".toRegex(), ".")
        return result.toFloat()
    }
}