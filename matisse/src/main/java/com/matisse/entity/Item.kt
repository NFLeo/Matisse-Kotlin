package com.matisse.entity

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import com.matisse.MimeType
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Describe :
 * Created by Leo on 2018/9/4 on 16:18.
 */
@Parcelize
class Item(var id: Long, var mimeType: String, var size: Long = 0, var duration: Long = 0) : Parcelable {

    companion object {
        const val ITEM_ID_CAPTURE: Long = -1
        const val ITEM_DISPLAY_NAME_CAPTURE = "Capture"

        fun valueOf(cursor: Cursor): Item {
            return Item(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)),
                    cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)),
                    cursor.getLong(cursor.getColumnIndex("duration")))
        }
    }


    @IgnoredOnParcel
    private var uri: Uri

    init {
        val contentUri: Uri = when {
            isImage() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> // ?
                MediaStore.Files.getContentUri("external")
        }
        uri = ContentUris.withAppendedId(contentUri, id)
    }

    fun isImage(): Boolean {
        return mimeType == MimeType.JPEG.getKey()
                || mimeType == MimeType.PNG.getKey()
                || mimeType == MimeType.GIF.getKey()
                || mimeType == MimeType.BMP.getKey()
                || mimeType == MimeType.WEBP.getKey()
    }

    fun isGif(): Boolean {
        return mimeType == MimeType.GIF.getKey()
    }

    fun isVideo(): Boolean {
        return mimeType == MimeType.MPEG.getKey()
                || mimeType == MimeType.MP4.getKey()
                || mimeType == MimeType.QUICKTIME.getKey()
                || mimeType == MimeType.THREEGPP.getKey()
                || mimeType == MimeType.THREEGPP2.getKey()
                || mimeType == MimeType.MKV.getKey()
                || mimeType == MimeType.WEBM.getKey()
                || mimeType == MimeType.TS.getKey()
                || mimeType == MimeType.AVI.getKey()
    }

    fun getContentUri() = uri

    fun isCapture() = id == ITEM_ID_CAPTURE

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Item) {
            return false
        }

        val other = obj as Item?
        return ((id == other!!.id && (mimeType == other.mimeType)) && (uri == other.uri) && size == other.size && duration == other.duration)
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + java.lang.Long.valueOf(size).hashCode()
        result = 31 * result + java.lang.Long.valueOf(duration).hashCode()
        return result
    }
}