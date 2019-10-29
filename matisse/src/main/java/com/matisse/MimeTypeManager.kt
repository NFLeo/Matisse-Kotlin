package com.matisse

import android.content.ContentResolver
import android.net.Uri
import androidx.collection.ArraySet
import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.matisse.utils.PhotoMetadataUtils
import java.util.*

/**
 * Describe : Define MediaType
 * Created by Leo on 2018/8/29 on 15:02.
 */
class MimeTypeManager {

    companion object {
        fun ofAll(): EnumSet<MimeType> = EnumSet.allOf(MimeType::class.java)

        fun of(type: MimeType, rest: Array<MimeType>): EnumSet<MimeType> = EnumSet.of(type, *rest)

        fun ofImage(): EnumSet<MimeType> = EnumSet.of(
            MimeType.JPEG, MimeType.PNG, MimeType.GIF, MimeType.BMP, MimeType.WEBP
        )

        fun ofVideo(): EnumSet<MimeType> = EnumSet.of(
            MimeType.MPEG, MimeType.MP4, MimeType.QUICKTIME, MimeType.THREEGPP, MimeType.THREEGPP2,
            MimeType.MKV, MimeType.WEBM, MimeType.TS, MimeType.AVI
        )

        fun isImage(mimeType: String) = MimeType.JPEG.getKey().contains(mimeType.toLowerCase())
                || MimeType.PNG.getKey().contains(mimeType.toLowerCase())
                || MimeType.GIF.getKey().contains(mimeType.toLowerCase())
                || MimeType.BMP.getKey().contains(mimeType.toLowerCase())
                || MimeType.WEBP.getKey().contains(mimeType.toLowerCase())

        fun isVideo(mimeType: String) = MimeType.MPEG.getKey().contains(mimeType.toLowerCase())
                || MimeType.MP4.getKey().contains(mimeType.toLowerCase())
                || MimeType.QUICKTIME.getKey().contains(mimeType.toLowerCase())
                || MimeType.THREEGPP.getKey().contains(mimeType.toLowerCase())
                || MimeType.THREEGPP2.getKey().contains(mimeType.toLowerCase())
                || MimeType.MKV.getKey().contains(mimeType.toLowerCase())
                || MimeType.WEBM.getKey().contains(mimeType.toLowerCase())
                || MimeType.TS.getKey().contains(mimeType.toLowerCase())
                || MimeType.AVI.getKey().contains(mimeType.toLowerCase())

        fun isGif(mimeType: String) = MimeType.GIF.toString().contains(mimeType.toLowerCase())

        fun arraySetOf(vararg suffixes: String) = ArraySet(mutableListOf(*suffixes))

        fun checkType(resolver: ContentResolver, uri: Uri?, mExtensions: Set<String>): Boolean {
            val map = MimeTypeMap.getSingleton()
            if (uri == null) return false

            val type = map.getExtensionFromMimeType(resolver.getType(uri))
            var path: String? = null
            // lazy load the path and prevent resolve for multiple times
            var pathParsed = false
            for (extension in mExtensions) {
                if (extension == type) return true

                if (!pathParsed) {
                    // we only resolve the path for one time
                    path = PhotoMetadataUtils.getPath(resolver, uri)
                    if (!TextUtils.isEmpty(path)) {
                        path = path?.toLowerCase(Locale.US)
                    }
                    pathParsed = true
                }
                if (path != null && path.endsWith(extension)) {
                    return true
                }
            }
            return false
        }
    }
}