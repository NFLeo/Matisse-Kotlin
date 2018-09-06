package com.matisse.ui.adapter

import android.database.Cursor
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewCursorAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH> {

    private var mCursor: Cursor? = null
    private var mRowIDColumn: Int = 0

    constructor(c: Cursor) {
        setHasStableIds(true)
        swapCursor(c)
    }

    abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (!isDataValid(mCursor)) {
            throw IllegalStateException("Cannot bind view holder when cursor is in invalid state.")
        }

        if (!mCursor?.moveToPosition(position)!!) {
            throw IllegalStateException("Could not move cursor to position $position when trying to bind view holder")
        }

        onBindViewHolder(holder, mCursor!!)
    }

    override fun getItemViewType(position: Int): Int {
        if (!mCursor?.moveToPosition(position)!!) {
            throw IllegalStateException("Could not move cursor to position " + position
                    + " when trying to get item view type.")
        }
        return getItemViewType(position, mCursor!!)
    }

    abstract fun getItemViewType(position: Int, cursor: Cursor): Int

    override fun getItemCount(): Int {
        return if (isDataValid(mCursor)) {
            mCursor?.count!!
        } else {
            0
        }
    }

    override fun getItemId(position: Int): Long {
        if (!isDataValid(mCursor)) {
            throw IllegalStateException("Cannot lookup item id when cursor is in invalid state.")
        }
        if (!mCursor?.moveToPosition(position)!!) {
            throw IllegalStateException("Could not move cursor to position " + position
                    + " when trying to get an item id")
        }

        return mCursor?.getLong(mRowIDColumn) ?: 0
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor == mCursor) {
            return
        }

        if (newCursor == null) {
            notifyItemRangeRemoved(0, itemCount)
            mCursor = null
            mRowIDColumn = -1
        } else {
            mCursor = newCursor
            mRowIDColumn = mCursor?.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) ?: 0
            // notify the observers about the new cursor
            notifyDataSetChanged()
        }
    }

    private fun isDataValid(cursor: Cursor?) = cursor != null && !cursor.isClosed

    fun getCursor() = mCursor
}