package com.matisse.internal.entity

import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.matisse.loader.AlbumLoader
import com.matisse.R

/**
 * Describe :
 * Created by Leo on 2018/8/29 on 16:15.
 */
class Album() : Parcelable {

    private var mId: String = ""
    private var mCoverPath: String = ""
    private var mDisplayName: String = ""
    private var mCount: Long = 0

    constructor(mId: String, mCoverPath: String, mDisplayName: String, mCount: Long) : this() {
        this.mId = mId
        this.mCoverPath = mCoverPath
        this.mDisplayName = mDisplayName
        this.mCount = mCount
    }

    fun getId(): String {
        return mId
    }

    fun getCoverPath(): String {
        return mCoverPath
    }

    fun getCount(): Long {
        return mCount
    }

    fun addCaptureCount() {
        mCount++
    }

    fun getDisplayName(context: Context): String {
        return if (isAll()) {
            context.getString(R.string.album_name_all)
        } else mDisplayName
    }

    fun isAll(): Boolean {
        return ALBUM_ID_ALL == mId
    }

    fun isEmpty(): Boolean {
        return mCount == 0L
    }

    constructor(parcel: Parcel) : this() {
        mId = parcel.readString()
        mCoverPath = parcel.readString()
        mDisplayName = parcel.readString()
        mCount = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mId)
        parcel.writeString(mCoverPath)
        parcel.writeString(mDisplayName)
        parcel.writeLong(mCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Album> {

        val ALBUM_ID_ALL = (-1).toString()
        val ALBUM_NAME_ALL = "All"

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
                cursor.getLong(cursor.getColumnIndex(AlbumLoader.COLUMN_COUNT)))
    }
}