package com.matisse.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.matisse.R
import com.matisse.entity.Item
import com.matisse.internal.entity.Album
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection

class AlbumMediaAdapter : RecyclerViewCursorAdapter<RecyclerView.ViewHolder> {

    private lateinit var mSelectedCollection: SelectedItemCollection
    private lateinit var mPlaceholder: Drawable
    private lateinit var mSelectionSpec: SelectionSpec
    private var mCheckStateListener: CheckStateListener? = null
    private var mOnMediaClickListener: OnMediaClickListener? = null
    private lateinit var mRecyclerView: RecyclerView
    private var mImageResize: Int = 0

    companion object {
        val VIEW_TYPE_CAPTURE = 0X01
        val VIEW_TYPE_MEDIA = 0X02
    }

    constructor(context: Context, selectedCollection: SelectedItemCollection, recyclerView: RecyclerView) : super(null) {
        mSelectionSpec = SelectionSpec.getInstance()
        mSelectedCollection = selectedCollection

        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_CAPTURE) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_capture, parent, false)

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor) {
    }

    override fun getItemViewType(position: Int, cursor: Cursor): Int {
    }

    interface CheckStateListener {
        fun onUpdate()
    }

    interface OnMediaClickListener {
        fun onMedoaClick(album: Album, item: Item, adapterPosition: Int)
    }
}