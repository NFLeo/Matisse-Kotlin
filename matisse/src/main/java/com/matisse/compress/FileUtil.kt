package com.matisse.compress

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

/**
 * 文件工具类
 *
 * Author: nanchen
 * Email: liushilin520@foxmail.com
 * Date: 2017-03-08  9:03
 */
object FileUtil {

    const val FILES_PATH = "CompressHelper"

    /**
     * 根据文件路径获取文件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    fun getFileByPath(filePath: String) = File(filePath)

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return `true`: 存在<br></br>`false`: 不存在
     */
    fun isFileExists(filePath: String) = isFileExists(getFileByPath(filePath))

    /**
     * 判断文件是否存在
     *
     * @param file 文件
     * @return `true`: 存在<br></br>`false`: 不存在
     */
    private fun isFileExists(file: File?) = file != null && file.exists()

    /**
     * 判断是否是目录
     *
     * @param dirPath 目录路径
     * @return `true`: 是<br></br>`false`: 否
     */
    fun isDir(dirPath: String) = isDir(getFileByPath(dirPath))

    /**
     * 判断是否是目录
     *
     * @param file 文件
     * @return `true`: 是<br></br>`false`: 否
     */
    private fun isDir(file: File?) = isFileExists(file) && file!!.isDirectory

    /**
     * 判断是否是文件
     *
     * @param filePath 文件路径
     * @return `true`: 是<br></br>`false`: 否
     */
    fun isFile(filePath: String) = isFile(getFileByPath(filePath))

    /**
     * 判断是否是文件
     *
     * @param file 文件
     * @return `true`: 是<br></br>`false`: 否
     */
    private fun isFile(file: File?) = isFileExists(file) && file!!.isFile

    /**
     * 截取文件名称
     * @param fileName  文件名称
     */
    fun splitFileName(fileName: String): Array<String> {
        var name = fileName
        var extension = ""
        val i = fileName.lastIndexOf(".")
        if (i != -1) {
            name = fileName.substring(0, i)
            extension = fileName.substring(i)
        }

        return arrayOf(name, extension)
    }

    /**
     * 获取文件名称
     * @param context   上下文
     * @param uri       uri
     * @return          文件名称
     */
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf(File.separator)
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * 获取真实的路径
     * @param context   上下文
     * @param uri       uri
     * @return          文件路径
     */
    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor == null) {
            uri.path
        } else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            val realPath = cursor.getString(index)
            cursor.close()
            realPath
        }
    }
}
