package com.matisse.entity

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.matisse.MimeType
import java.time.Duration

/**
 * Describe :
 * Created by Leo on 2018/9/4 on 16:18.
 */
class Item() : Parcelable {

    var id: Long = 0
    var mimeType: String? = null
    var uri: Uri? = null
    var size: Long = 0
    var duration: Long = 0 // only for video, in ms

    constructor(id: Long, mimeType: String, size: Long, duration: Long) : this() {
        this.id = id
        this.mimeType = mimeType

        val contentUri: Uri = when {
            isImage() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> // ?
                MediaStore.Files.getContentUri("external")
        }

        this.uri = ContentUris.withAppendedId(contentUri, id)
        this.size = size
        this.duration = duration
    }

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        mimeType = parcel.readString()
        uri = parcel.readParcelable(Uri::class.java.classLoader)
        size = parcel.readLong()
        duration = parcel.readLong()
    }

    fun isImage(): Boolean {
        return if (mimeType == null) false else mimeType == MimeType.JPEG.toString()
                || mimeType == MimeType.PNG.toString()
                || mimeType == MimeType.GIF.toString()
                || mimeType == MimeType.BMP.toString()
                || mimeType == MimeType.WEBP.toString()
    }

    fun isGif(): Boolean {
        return if (mimeType == null) false else mimeType == MimeType.GIF.toString()
    }

    fun isVideo(): Boolean {
        return if (mimeType == null) false else mimeType == MimeType.MPEG.toString()
                || mimeType == MimeType.MP4.toString()
                || mimeType == MimeType.QUICKTIME.toString()
                || mimeType == MimeType.THREEGPP.toString()
                || mimeType == MimeType.THREEGPP2.toString()
                || mimeType == MimeType.MKV.toString()
                || mimeType == MimeType.WEBM.toString()
                || mimeType == MimeType.TS.toString()
                || mimeType == MimeType.AVI.toString()
    }

    fun getContentUri() = uri

    fun isCapture() = id == ITEM_ID_CAPTURE

    override fun equals(other: Any?): Boolean {
        if (other !is Item) {
            return false
        }

        return (id == other.id
                && (mimeType != null && mimeType == other.mimeType || mimeType == null && other.mimeType == null)
                && (uri != null && uri == other.uri || uri == null && other.uri == null)
                && size == other.size
                && duration == other.duration)
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + java.lang.Long.valueOf(id).hashCode()
        if (mimeType != null) {
            result = 31 * result + mimeType?.hashCode()!!
        }
        result = 31 * result + uri?.hashCode()!!
        result = 31 * result + java.lang.Long.valueOf(size).hashCode()
        result = 31 * result + java.lang.Long.valueOf(duration).hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(mimeType)
        parcel.writeParcelable(uri, flags)
        parcel.writeLong(size)
        parcel.writeLong(duration)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {

        val ITEM_ID_CAPTURE = -1L
        val ITEM_DISPLAY_NAME_CAPTURE = "Capture"

        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }

        fun valueOf(cursor: Cursor) = Item(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)),
                cursor.getLong(cursor.getColumnIndex("duration")))
    }
}