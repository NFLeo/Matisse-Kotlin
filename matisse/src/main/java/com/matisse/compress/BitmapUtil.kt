package com.matisse.compress

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.text.TextUtils
import java.io.*
import kotlin.math.roundToInt

/**
 * 图片处理工具类
 *
 * Author: nanchen
 * Email: liushilin520@foxmail.com
 * Date: 2017-03-08  9:03
 */

object BitmapUtil {

    fun getScaledBitmap(
        context: Context, imageUri: Uri, maxWidth: Float,
        maxHeight: Float, bitmapConfig: Bitmap.Config
    ): Bitmap? {
        val filePath = FileUtil.getRealPathFromURI(context, imageUri)
        var scaledBitmap: Bitmap? = null

        val options = BitmapFactory.Options()

        //by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp: Bitmap? = BitmapFactory.decodeFile(filePath, options)
        if (bmp == null) {
            var inputStream: InputStream?
            try {
                inputStream = FileInputStream(filePath!!)
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
            } catch (exception: FileNotFoundException) {
                exception.printStackTrace()
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }

        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        if (actualHeight == -1 || actualWidth == -1) {
            try {
                val exifInterface = ExifInterface(filePath)
                // 获取图片的高度
                actualHeight = exifInterface.getAttributeInt(
                    ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL
                )
                // 获取图片的宽度
                actualWidth = exifInterface.getAttributeInt(
                    ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (actualWidth <= 0 || actualHeight <= 0) {
            val bitmap2 = BitmapFactory.decodeFile(filePath)
            if (bitmap2 != null) {
                actualWidth = bitmap2.width
                actualHeight = bitmap2.height
            } else {
                return null
            }
        }

        var imgRatio = actualWidth.toFloat() / actualHeight
        val maxRatio = maxWidth / maxHeight

        //width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }
                imgRatio > maxRatio -> {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }
                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

        //this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)

        try {
            // load the bitmap getTempFile its path
            bmp = BitmapFactory.decodeFile(filePath, options)
            if (bmp == null) {
                val inputStream: InputStream?
                try {
                    inputStream = FileInputStream(filePath!!)
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }
            }
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }

        if (actualHeight <= 0 || actualWidth <= 0) return null

        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, bitmapConfig)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }

        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, 0f, 0f)

        Canvas(scaledBitmap!!).apply {
            setMatrix(scaleMatrix)
            drawBitmap(bmp!!, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // 采用 ExitInterface 设置图片旋转方向
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            val matrix = Matrix()
            when (orientation) {
                6 -> matrix.postRotate(90f)
                3 -> matrix.postRotate(180f)
                8 -> matrix.postRotate(270f)
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0,
                scaledBitmap.width, scaledBitmap.height,
                matrix, true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return scaledBitmap
    }

    fun compressImage(
        context: Context, imageUri: Uri, maxWidth: Float, maxHeight: Float,
        compressFormat: Bitmap.CompressFormat, bitmapConfig: Bitmap.Config,
        quality: Int, parentPath: String, prefix: String, fileName: String
    ): File {
        var out: FileOutputStream? = null
        val filename = generateFilePath(
            context, parentPath, imageUri, compressFormat.name.toLowerCase(), prefix, fileName
        )
        try {
            out = FileOutputStream(filename)
            // 通过文件名写入
            val newBmp = getScaledBitmap(context, imageUri, maxWidth, maxHeight, bitmapConfig)
            newBmp?.compress(compressFormat, quality, out)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (ignored: IOException) {
            }
        }

        return File(filename)
    }

    private fun generateFilePath(
        context: Context, parentPath: String, uri: Uri,
        extension: String, prefix: String, fileName: String
    ): String {
        var cPrefix = prefix
        var cFileName = fileName
        val file = File(parentPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        // if prefix is null, set prefix ""
        cPrefix = if (TextUtils.isEmpty(cPrefix)) "" else cPrefix
        // reset fileName by prefix and custom file name
        cFileName = if (TextUtils.isEmpty(cFileName)) cPrefix + FileUtil.splitFileName(
            FileUtil.getFileName(context, uri)
        )[0] else cFileName
        return file.absolutePath + File.separator + cFileName + "." + extension
    }


    /**
     * 计算inSampleSize
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }

        return inSampleSize
    }
}
