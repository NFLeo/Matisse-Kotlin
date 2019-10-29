package com.matisse.compress

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

import java.io.File

/**
 * 压缩方法工具类
 *
 * Author: nanchen
 * Email: liushilin520@foxmail.com
 * Date: 2017-03-08  9:03
 */
class CompressHelper(private val context: Context) {

    /**
     * 最大宽度，默认为720
     */
    private var maxWidth = 720.0f
    /**
     * 最大高度,默认为960
     */
    private var maxHeight = 960.0f
    /**
     * 默认压缩后的方式为JPEG
     */
    private var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG

    /**
     * 默认的图片处理方式是ARGB_8888
     */
    private var bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888
    /**
     * 默认压缩质量为80
     */
    private var quality = 80
    /**
     * 存储路径
     */
    private var destinationDirectoryPath: String = ""
    /**
     * 文件名前缀
     */
    private var fileNamePrefix: String = ""
    /**
     * 文件名
     */
    private var fileName: String = ""


    init {
        destinationDirectoryPath = context.cacheDir.path + File.pathSeparator + FileUtil.FILES_PATH
    }

    /**
     * 压缩成文件
     * @param file  原始文件
     * @return      压缩后的文件
     */
    fun compressToFile(file: File): File {
        return BitmapUtil.compressImage(
            context, Uri.fromFile(file), maxWidth, maxHeight,
            compressFormat, bitmapConfig, quality, destinationDirectoryPath,
            fileNamePrefix, fileName
        )
    }

    /**
     * 压缩成文件
     * @param uri  原始文件
     * @return      压缩后的文件
     */
    fun compressToFile(uri: Uri): File {
        return BitmapUtil.compressImage(
            context, uri, maxWidth, maxHeight,
            compressFormat, bitmapConfig, quality, destinationDirectoryPath,
            fileNamePrefix, fileName
        )
    }

    /**
     * 压缩为Bitmap
     * @param file  原始文件
     * @return      压缩后的Bitmap
     */
    fun compressToBitmap(file: File): Bitmap? {
        return BitmapUtil.getScaledBitmap(
            context, Uri.fromFile(file), maxWidth, maxHeight, bitmapConfig
        )
    }

    /**
     * 采用建造者模式，设置Builder
     */
    class Builder(context: Context) {
        private val mCompressHelper: CompressHelper = CompressHelper(context)

        /**
         * 设置图片最大宽度
         * @param maxWidth  最大宽度
         */
        fun setMaxWidth(maxWidth: Float): Builder {
            mCompressHelper.maxWidth = maxWidth
            return this
        }

        /**
         * 设置图片最大高度
         * @param maxHeight 最大高度
         */
        fun setMaxHeight(maxHeight: Float): Builder {
            mCompressHelper.maxHeight = maxHeight
            return this
        }

        /**
         * 设置压缩的后缀格式
         */
        fun setCompressFormat(compressFormat: Bitmap.CompressFormat): Builder {
            mCompressHelper.compressFormat = compressFormat
            return this
        }

        /**
         * 设置Bitmap的参数
         */
        fun setBitmapConfig(bitmapConfig: Bitmap.Config): Builder {
            mCompressHelper.bitmapConfig = bitmapConfig
            return this
        }

        /**
         * 设置压缩质量，建议80
         * @param quality   压缩质量，[0,100]
         */
        fun setQuality(quality: Int): Builder {
            mCompressHelper.quality = quality
            return this
        }

        /**
         * 设置目的存储路径
         * @param destinationDirectoryPath  目的路径
         */
        fun setDestinationDirectoryPath(destinationDirectoryPath: String): Builder {
            mCompressHelper.destinationDirectoryPath = destinationDirectoryPath
            return this
        }

        /**
         * 设置文件前缀
         * @param prefix    前缀
         */
        fun setFileNamePrefix(prefix: String): Builder {
            mCompressHelper.fileNamePrefix = prefix
            return this
        }

        /**
         * 设置文件名称
         * @param fileName  文件名
         */
        fun setFileName(fileName: String): Builder {
            mCompressHelper.fileName = fileName
            return this
        }

        fun build(): CompressHelper {
            return mCompressHelper
        }
    }

    companion object {

        private var INSTANCE: CompressHelper? = null

        fun getDefault(context: Context): CompressHelper? {
            if (INSTANCE == null) {
                synchronized(CompressHelper::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = CompressHelper(context)
                    }
                }
            }
            return INSTANCE
        }
    }
}
