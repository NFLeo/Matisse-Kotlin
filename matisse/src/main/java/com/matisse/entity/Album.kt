package com.matisse.entity

import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.matisse.R
import com.matisse.loader.AlbumLoader

class Album() : Parcelable {

    private var id = ""
    private var coverPath = ""
    private var displayName = ""
    private var count: Long = 0
    private var isCheck = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        coverPath = parcel.readString()
        displayName = parcel.readString()
        count = parcel.readLong()
        isCheck = parcel.readByte() != 0.toByte()
    }

    constructor(mId: String, mCoverPath: String, mDisplayName: String, mCount: Long) :
            this(mId, mCoverPath, mDisplayName, mCount, false)

    constructor(
        mId: String, mCoverPath: String, mDisplayName: String,
        mCount: Long, mIsCheck: Boolean
    ) : this() {
        this.id = mId
        this.coverPath = mCoverPath
        this.displayName = mDisplayName
        this.count = mCount
        this.isCheck = mIsCheck
    }

    fun getId() = id

    fun getCoverPath() = coverPath

    fun getCount() = count

    fun addCaptureCount() {
        count++
    }

    fun getDisplayName(context: Context): String {
        return if (isAll()) {
            context.getString(R.string.album_name_all)
        } else displayName
    }

    fun isAll() = ALBUM_ID_ALL == id

    fun isEmpty() = count == 0L

    fun isChecked() = isCheck

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(coverPath)
        parcel.writeString(displayName)
        parcel.writeLong(count)
        parcel.writeByte(if (isCheck) 1 else 0)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Album> {

        const val ALBUM_ID_ALL = (-1).toString()
        const val ALBUM_NAME_ALL = "All"

        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }

        fun valueOf(cursor: Cursor) = Album(
            cursor.getString(cursor.getColumnIndex(AlbumLoader.BUCKET_ID)),
            cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)),
            cursor.getString(cursor.getColumnIndex(AlbumLoader.BUCKET_DISPLAY_NAME)),
            cursor.getLong(cursor.getColumnIndex(AlbumLoader.COLUMN_COUNT))
        )
    }
}