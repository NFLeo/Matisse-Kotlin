package com.matisse.entity

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Describe :
 * Created by Leo on 2018/9/4 on 16:18.
 */
@Parcelize
class Item(
    var id: Long, private var mimeType: String, var size: Long = 0,
    var duration: Long = 0, var positionInList: Int = -1
) : Parcelable {

    companion object {
        const val ITEM_ID_CAPTURE: Long = -1
        const val ITEM_DISPLAY_NAME_CAPTURE = "Capture"

        // * 注：资源文件size单位为字节byte
        fun valueOf(cursor: Cursor, positionInList: Int = -1) = Item(
            cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
            cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)),
            cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)),
            cursor.getLong(cursor.getColumnIndex("duration")),
            positionInList
        )
    }

    @IgnoredOnParcel
    private var uri: Uri


    init {
        val contentUri = when {
            isImage() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
        uri = ContentUris.withAppendedId(contentUri, id)
    }

    fun isImage() = MimeTypeManager.isImage(mimeType)

    fun isGif() = MimeTypeManager.isGif(mimeType)

    fun isVideo() = MimeTypeManager.isVideo(mimeType)

    fun getContentUri() = uri

    fun isCapture() = id == ITEM_ID_CAPTURE

    override fun describeContents() = 0

    override fun equals(other: Any?): Boolean {
        if (other !is Item) return false

        val otherItem = other as Item?
        return ((id == otherItem?.id && (mimeType == otherItem.mimeType))
                && (uri == otherItem.uri) && size == otherItem.size && duration == otherItem.duration)
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + size.toString().hashCode()
        result = 31 * result + duration.toString().hashCode()
        return result
    }
}