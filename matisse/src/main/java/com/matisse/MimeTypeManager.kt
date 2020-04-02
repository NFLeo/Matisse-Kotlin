package com.matisse

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.getRealFilePath
import java.util.*

/**
 * Describe : Define MediaType
 * Created by Leo on 2018/8/29 on 15:02.
 */
class MimeTypeManager {

    companion object {
        fun ofAll(): EnumSet<MimeType> = EnumSet.allOf(MimeType::class.java)

        fun of(first: MimeType, others: Array<MimeType>): EnumSet<MimeType> =
            EnumSet.of(first, *others)

        fun ofImage(): EnumSet<MimeType> = EnumSet.of(
            MimeType.JPEG, MimeType.JPG, MimeType.PNG, MimeType.GIF, MimeType.BMP, MimeType.WEBP
        )

        // 静态图
        fun ofMotionlessImage(): EnumSet<MimeType> = EnumSet.of(
            MimeType.JPEG, MimeType.JPG, MimeType.PNG, MimeType.BMP
        )

        fun ofVideo(): EnumSet<MimeType> = EnumSet.of(
            MimeType.MPEG, MimeType.MP4, MimeType.QUICKTIME, MimeType.THREEGPP, MimeType.THREEGPP2,
            MimeType.MKV, MimeType.WEBM, MimeType.TS, MimeType.AVI
        )

        fun isImage(mimeType: String?) =
            isMotionlessImage(mimeType)
                    || MimeType.GIF.getKey().contains(lowerCaseMimeType(mimeType))
                    || MimeType.WEBP.getKey().contains(lowerCaseMimeType(mimeType))

        private fun isMotionlessImage(mimeType: String?) =
            MimeType.JPEG.getKey().contains(lowerCaseMimeType(mimeType))
                    || MimeType.JPG.getKey().contains(lowerCaseMimeType(mimeType))
                    || MimeType.PNG.getKey().contains(lowerCaseMimeType(mimeType))
                    || MimeType.BMP.getKey().contains(lowerCaseMimeType(mimeType))

        fun isVideo(mimeType: String) = MimeType.MPEG.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.MP4.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.QUICKTIME.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.THREEGPP.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.THREEGPP2.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.MKV.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.WEBM.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.TS.getKey().contains(lowerCaseMimeType(mimeType))
                || MimeType.AVI.getKey().contains(lowerCaseMimeType(mimeType))

        fun isGif(mimeType: String) = MimeType.GIF.getKey().contains(lowerCaseMimeType(mimeType))

        fun arraySetOf(vararg suffixes: String) = ArraySet(mutableListOf(*suffixes))

        fun checkType(context: Context, uri: Uri?, mExtensions: Set<String>): Boolean {
            val map = MimeTypeMap.getSingleton()
            if (uri == null) return false

            val type = map.getExtensionFromMimeType(context.contentResolver.getType(uri))
            var path: String? = null
            // lazy load the path and prevent resolve for multiple times
            var pathParsed = false
            mExtensions.forEach {
                if (it == type) return true

                if (!pathParsed) {
                    // we only resolve the path for one time
                    path = getRealFilePath(context, uri)
                    if (!TextUtils.isEmpty(path)) path = path?.toLowerCase(Locale.US)
                    pathParsed = true
                }
                if (path != null && path?.endsWith(it) == true) return true
            }
            return false
        }

        private fun lowerCaseMimeType(mimeType: String?) = mimeType?.toLowerCase() ?: ""
    }
}