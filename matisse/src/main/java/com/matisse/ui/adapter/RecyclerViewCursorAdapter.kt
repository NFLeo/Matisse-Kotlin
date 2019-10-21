package com.matisse.ui.adapter

import android.database.Cursor
import android.provider.MediaStore
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewCursorAdapter<VH : RecyclerView.ViewHolder>(c: Cursor?) :
    RecyclerView.Adapter<VH>() {

    private var cursor: Cursor? = null
    private var rowIDColumn = 0

    init {
        setHasStableIds(true)
        swapCursor(c)
    }

    abstract fun onBindViewHolder(holder: VH, cursor: Cursor, position: Int)

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (!isDataValid(cursor)) {
            throw IllegalStateException("Cannot bind view holder when cursor is in invalid state.")
        }

        if (!cursor?.moveToPosition(position)!!) {
            throw IllegalStateException("Could not move cursor to position $position when trying to bind view holder")
        }

        onBindViewHolder(holder, cursor!!, position)
    }

    override fun getItemViewType(position: Int): Int {
        if (!cursor?.moveToPosition(position)!!) {
            throw IllegalStateException(
                "Could not move cursor to position $position when trying to get item view type."
            )
        }
        return getItemViewType(position, cursor!!)
    }

    abstract fun getItemViewType(position: Int, cursor: Cursor): Int

    override fun getItemCount() = if (isDataValid(cursor)) cursor?.count!! else 0

    override fun getItemId(position: Int): Long {
        if (!isDataValid(cursor)) {
            throw IllegalStateException("Cannot lookup item id when cursor is in invalid state.")
        }
        if (!cursor?.moveToPosition(position)!!) {
            throw IllegalStateException(
                "Could not move cursor to position $position when trying to get an item id"
            )
        }

        return cursor?.getLong(rowIDColumn) ?: 0
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor == cursor) return

        if (newCursor == null) {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            rowIDColumn = -1
        } else {
            cursor = newCursor
            rowIDColumn = cursor?.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) ?: 0
            // notify the observers about the new cursor
            notifyDataSetChanged()
        }
    }

    private fun isDataValid(cursor: Cursor?) = cursor != null && !cursor.isClosed

    fun getCursor() = cursor
}