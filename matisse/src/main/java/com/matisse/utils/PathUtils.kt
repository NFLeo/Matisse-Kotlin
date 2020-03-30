@file:JvmName("PathUtils")

package com.matisse.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Describe : http://stackoverflow.com/a/27271131/4739220
 * Created by Leo on 2018/9/5 on 14:07.
 */

fun getSimpleDateFormat() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

fun getPath(context: Context?, uri: Uri?): String? {
    if (uri == null || context == null) return ""

    // DocumentProvider
    if (Platform.hasKitKat19() && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]

            if ("primary".equals(type, true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
            )

            return getDataColumn(context, contentUri, null, null)
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            var contentUri: Uri? = null
            when (type) {
                "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri.scheme, true)) {
        return getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, true)) { // File
        return uri.path
    }

    return null
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param context       The context.
 * @param uri           The Uri to query.
 * @param selection     (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
private fun getDataColumn(
    context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
): String? {

    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor =
            context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } finally {
        cursor?.close()
    }
    return null
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

fun createImageFile(
    context: Context, authority: String, otherEvent: ((absolutePath: String) -> Unit)? = null
): Uri {
    val imageFileName = String.format("JPEG_%s.jpg", getSimpleDateFormat())
    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (!storageDir.exists()) storageDir.mkdirs()
    val tempFile = File(storageDir, imageFileName)
    otherEvent?.invoke(tempFile.absolutePath)
    return FileProvider.getUriForFile(context, authority, tempFile)
}

fun createImageFileForQ(
    context: Context, otherEvent: ((uri: Uri?) -> Unit)? = null
): Uri? {
    val imageFileName = String.format("JPEG_%s.jpg", getSimpleDateFormat())

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val uri = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    otherEvent?.invoke(uri)
    return uri
}

/**
 * 根据时间戳创建文件名
 *
 * @param prefix 前缀名
 * @return
 */
fun getCreateFileName(prefix: String): String {
    val millis = System.currentTimeMillis()
    return prefix + SimpleDateFormat("yyyyMMdd_HHmmssSS").format(millis)
}

/**
 * @param ctx
 * @return
 */
fun getDiskCacheDir(ctx: Context): String {
    return ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path
}

/**
 * 获取图片后缀
 *
 * @param path
 * @return
 */
fun getLastImgType(path: String): String {
    try {
        val index = path.lastIndexOf(".")
        if (index > 0) {
            val imageType = path.substring(index)
            when (imageType) {
                ".png", ".PNG", ".jpg", ".jpeg", ".JPEG", ".WEBP", ".bmp", ".BMP", ".webp", ".gif", ".GIF" -> return imageType
                else -> return ".png"
            }
        } else {
            return ".png"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return ".png"
    }

}

/**
 * 获取图片后缀
 *
 * @param mineType
 * @return
 */
fun getLastImgSuffix(mineType: String): String {
    val defaultSuffix = ".png"
    try {
        val index = mineType.lastIndexOf("/") + 1
        if (index > 0) {
            return "." + mineType.substring(index)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return defaultSuffix
    }

    return defaultSuffix
}

/**
 * 根据uri获取MIME_TYPE
 *
 * @param uri
 * @return
 */
fun getMimeType(context: Context, uri: Uri): String {
    if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        val cursor = context.applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.Files.FileColumns.MIME_TYPE), null, null, null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                if (columnIndex > -1) {
                    return cursor.getString(columnIndex)
                }
            }
            cursor.close()
        }
    }
    return "image/jpeg"
}

