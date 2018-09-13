package com.matisse.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.utils.UIUtils
import com.matisse.widget.CheckView
import com.matisse.widget.MediaGrid

class AlbumMediaAdapter(context: Context, selectedCollection: SelectedItemCollection, recyclerView: RecyclerView) :
        RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(null), MediaGrid.OnMediaGridClickListener {

    private var mSelectedCollection: SelectedItemCollection = selectedCollection
    private var mPlaceholder: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_empty_zhihu)!!
    private var mSelectionSpec: SelectionSpec = SelectionSpec.getInstance()
    var mCheckStateListener: CheckStateListener? = null
    var mOnMediaClickListener: OnMediaClickListener? = null
    private var mRecyclerView: RecyclerView = recyclerView
    private var mImageResize: Int = 0

    companion object {
        const val VIEW_TYPE_CAPTURE = 0X01
        const val VIEW_TYPE_MEDIA = 0X02
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_CAPTURE -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_capture, parent, false)
                val holder = CaptureViewHolder(v)
                holder.itemView.setOnClickListener {
                    if (it.context is OnPhotoCapture) {
                        (it.context as OnPhotoCapture).capture()
                    }
                }
                return holder
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media_grid, parent, false)
                return MediaViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor) {
        if (holder is CaptureViewHolder) {
            val drawables = holder.mHint.compoundDrawables
            val ta = holder.itemView.context.theme
                    .obtainStyledAttributes(intArrayOf(R.attr.capture_textColor))
            val color = ta.getColor(0, 0)
            ta.recycle()

            for (i in drawables.indices) {
                val drawable = drawables[i]
                if (drawable != null) {
                    val state = drawable.constantState ?: continue

                    val newDrawable = state.newDrawable().mutate()
                    newDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    newDrawable.bounds = drawable.bounds
                    drawables[i] = newDrawable
                }
            }

            holder.mHint.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3])

        } else if (holder is MediaViewHolder) {
            val item = Item.valueOf(cursor)
            holder.mMediaGrid.preBindMedia(MediaGrid.PreBindInfo(getImageResize(holder.mMediaGrid.context),
                    mPlaceholder, mSelectionSpec.countable, holder))
            holder.mMediaGrid.bindMedia(item)
            holder.mMediaGrid.mListener = this
            setCheckStatus(item, holder.mMediaGrid)
        }
    }

    override fun getItemViewType(position: Int, cursor: Cursor) = if (Item.valueOf(cursor).isCapture()) VIEW_TYPE_CAPTURE else VIEW_TYPE_MEDIA

    private fun getImageResize(context: Context): Int {
        if (mImageResize != 0) {
            return mImageResize
        }

        val layoutManager = mRecyclerView.layoutManager as GridLayoutManager
        val spanCount = layoutManager.spanCount
        val screenWidth = context.resources.displayMetrics.widthPixels
        val availableWidth = screenWidth - context.resources.getDimensionPixelSize(
                R.dimen.media_grid_spacing) * (spanCount - 1)

        mImageResize = availableWidth / spanCount
        mImageResize = (mImageResize * mSelectionSpec.thumbnailScale).toInt()
        return mImageResize
    }

    private fun setCheckStatus(item: Item, mediaGrid: MediaGrid) {
        if (mSelectionSpec.countable) {
            val checkedNum = mSelectedCollection.checkedNumOf(item)

            if (checkedNum > 0) {
                mediaGrid.setCheckEnabled(true)
                mediaGrid.setCheckedNum(checkedNum)
            } else {
                if (mSelectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false)
                    mediaGrid.setCheckedNum(CheckView.UNCHECKED)
                } else {
                    mediaGrid.setCheckEnabled(true)
                    mediaGrid.setCheckedNum(checkedNum)
                }
            }
        } else {
            val selected = mSelectedCollection.isSelected(item)
            if (selected) {
                mediaGrid.setCheckEnabled(true)
                mediaGrid.setChecked(true)
            } else {
                if (mSelectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false)
                } else {
                    mediaGrid.setCheckEnabled(true)
                }
                mediaGrid.setChecked(false)
            }
        }
    }

    override fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder) {
        if (mOnMediaClickListener != null) {
            mOnMediaClickListener?.onMediaClick(null, item, holder.adapterPosition)
        }
    }

    override fun onCheckViewClicked(checkView: CheckView, item: Item, holder: RecyclerView.ViewHolder) {
        if (mSelectionSpec.countable) {
            val checkedNum = mSelectedCollection.checkedNumOf(item)
            if (checkedNum == CheckView.UNCHECKED) {
                if (assertAddSelection(holder.itemView.context, item)) {
                    mSelectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            } else {
                mSelectedCollection.remove(item)
                notifyCheckStateChanged()
            }
        } else {
            if (mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item)
                notifyCheckStateChanged()
            } else {
                if (assertAddSelection(holder.itemView.context, item)) {
                    mSelectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            }
        }
    }

    private fun notifyCheckStateChanged() {
        notifyDataSetChanged()
        if (mCheckStateListener != null) {
            mCheckStateListener?.onUpdate()
        }
    }

    private fun assertAddSelection(context: Context, item: Item): Boolean {
        val cause = mSelectedCollection.isAcceptable(item)
        UIUtils.handleCause(context, cause)
        return cause == null
    }

    interface CheckStateListener {
        fun onUpdate()
    }

    interface OnMediaClickListener {
        fun onMediaClick(album: Album?, item: Item, adapterPosition: Int)
    }

    interface OnPhotoCapture {
        fun capture()
    }

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mMediaGrid: MediaGrid = itemView as MediaGrid
    }

    class CaptureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mHint: TextView = itemView.findViewById(R.id.hint)
    }
}