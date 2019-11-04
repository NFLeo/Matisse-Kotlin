package com.matisse.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
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
import kotlin.math.log10
import kotlin.math.pow

/**
 * Created by Leo on 2018/8/29 on 15:24.
 */
object PhotoMetadataUtils {

    private const val MAX_WIDTH = 1600
    private const val SCHEME_CONTENT = "content"


    fun getPath(resolver: ContentResolver, uri: Uri): String? {
        if (SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null

            try {
                cursor = resolver.query(
                    uri, arrayOf(MediaStore.Images.ImageColumns.DATA),
                    null, null, null
                )
                if (cursor == null || !cursor.moveToFirst()) return null

                return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
            } finally {
                cursor?.close()
            }
        }

        return uri.path
    }

    /**
     * 遍历外部自定义过滤器
     */
    fun isAcceptable(context: Context, item: Item?): IncapableCause? {
        if (!isSelectableType(context, item))
            return IncapableCause(context.getString(R.string.error_file_type))

        if (SelectionSpec.getInstance().filters != null) {
            SelectionSpec.getInstance().filters?.forEach {
                return it.filter(context, item)
            }
        }
        return null
    }

    private fun isSelectableType(context: Context?, item: Item?): Boolean {
        val mimeTypeSet = SelectionSpec.getInstance().mimeTypeSet

        if (context == null || mimeTypeSet == null) return false

        val resolver = context.contentResolver
        for (type in mimeTypeSet) {
            if (MimeTypeManager.checkType(resolver, item?.getContentUri(), type.getValue())) {
                return true
            }
        }
        return false
    }

    /**
     * 获取资源的宽高尺寸
     */
    fun getBitmapBound(resolver: ContentResolver, uri: Uri?): Point {
        if (uri == null) return Point(0, 0)

        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            val width = options.outWidth
            val height = options.outHeight
            return Point(width, height)
        } catch (e: FileNotFoundException) {
            return Point(0, 0)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
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
        result = result.replace(",".toRegex(), ".") // in some case , 0.0 will be 0,0
        return java.lang.Float.valueOf(result)
    }

    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#")
            .format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun getBitmapSize(uri: Uri?, activity: Activity?): Point {
        val resolver = activity!!.contentResolver
        val imageSize = getBitmapBounds(resolver, uri!!)
        var w = imageSize.x
        var h = imageSize.y
        if (shouldRotate(resolver, uri)) {
            w = imageSize.y
            h = imageSize.x
        }
        if (h == 0) return Point(MAX_WIDTH, MAX_WIDTH)
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val widthScale = screenWidth / w
        val heightScale = screenHeight / h
        if (widthScale > heightScale) return Point((w * widthScale), (h * heightScale))

        return Point((w * widthScale), (h * heightScale))
    }

    private fun shouldRotate(resolver: ContentResolver, uri: Uri): Boolean {
        val exif: ExifInterface?
        try {
            exif = ExifInterfaceCompat.newInstance(getPath(resolver, uri))
        } catch (e: IOException) {
            return false
        }
        val orientation = exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
        return orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
    }

    private fun getBitmapBounds(resolver: ContentResolver?, uri: Uri): Point {
        var inStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inStream = resolver!!.openInputStream(uri)
            BitmapFactory.decodeStream(inStream, null, options)
            val width = options.outWidth
            val height = options.outHeight
            return Point(width, height)
        } catch (e: FileNotFoundException) {
            return Point(0, 0)
        } finally {
            if (inStream != null) {
                try {
                    inStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}