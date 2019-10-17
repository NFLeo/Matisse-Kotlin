package com.matisse.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
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

class AlbumMediaAdapter(
    context: Context, private var selectedCollection: SelectedItemCollection,
    private var recyclerView: RecyclerView
) : RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(null), MediaGrid.OnMediaGridClickListener {

    private var placeholder: Drawable? = null
    private var selectionSpec: SelectionSpec = SelectionSpec.getInstance()
    var checkStateListener: CheckStateListener? = null
    var onMediaClickListener: OnMediaClickListener? = null
    private var imageResize = 0
    private var layoutInflater: LayoutInflater

    init {
        val ta = context.theme.obtainStyledAttributes(intArrayOf(R.attr.item_placeholder))
        placeholder = ta.getDrawable(0)
        ta.recycle()

        layoutInflater = LayoutInflater.from(context)
    }

    companion object {
        const val VIEW_TYPE_CAPTURE = 0X01
        const val VIEW_TYPE_MEDIA = 0X02
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_CAPTURE -> {
                val v = layoutInflater.inflate(R.layout.item_photo_capture, parent, false)
                val holder = CaptureViewHolder(v)
                holder.itemView.setOnClickListener {
                    if (it.context is OnPhotoCapture) (it.context as OnPhotoCapture).capture()
                }
                return holder
            }
            else -> {
                val v = layoutInflater.inflate(R.layout.item_media_grid, parent, false)
                return MediaViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor) {
        holder.apply {
            when (this) {
                is CaptureViewHolder ->
                    UIUtils.setTextDrawable(itemView.context, hint, R.attr.Media_Camera_textColor)
                is MediaViewHolder -> {
                    val item = Item.valueOf(cursor)
                    mediaGrid.preBindMedia(
                        MediaGrid.PreBindInfo(
                            getImageResize(mediaGrid.context), placeholder,
                            selectionSpec.isCountable(), holder
                        )
                    )
                    mediaGrid.bindMedia(item)
                    mediaGrid.listener = this@AlbumMediaAdapter
                    setCheckStatus(item, mediaGrid)
                }
            }
        }
    }

    override fun getItemViewType(position: Int, cursor: Cursor) =
        if (Item.valueOf(cursor).isCapture()) VIEW_TYPE_CAPTURE else VIEW_TYPE_MEDIA

    private fun getImageResize(context: Context): Int {
        if (imageResize != 0) return imageResize

        val layoutManager = recyclerView.layoutManager as GridLayoutManager
        val spanCount = layoutManager.spanCount
        val screenWidth = context.resources.displayMetrics.widthPixels
        val availableWidth = screenWidth - context.resources.getDimensionPixelSize(
            R.dimen.media_grid_spacing
        ) * (spanCount - 1)

        imageResize = availableWidth / spanCount
        imageResize = (imageResize * selectionSpec.thumbnailScale).toInt()
        return imageResize
    }

    private fun setCheckStatus(item: Item, mediaGrid: MediaGrid) {
        if (selectionSpec.isCountable()) {
            // 单选不以数字方式显示
            val checkedNum = selectedCollection.checkedNumOf(item)

            if (checkedNum > 0) {
                mediaGrid.setCheckedNum(checkedNum)
            } else {
                mediaGrid.setCheckedNum(
                    if (selectedCollection.maxSelectableReached()) CheckView.UNCHECKED else checkedNum
                )
            }
        } else {
            val selected = selectedCollection.isSelected(item)
            mediaGrid.setChecked(selected)
        }
    }

    override fun onThumbnailClicked(
        thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder
    ) {
        onMediaClickListener?.onMediaClick(null, item, holder.adapterPosition)
    }

    override fun onCheckViewClicked(
        checkView: CheckView, item: Item, holder: RecyclerView.ViewHolder
    ) {
        if (selectionSpec.isCountable()) {
            val checkedNum = selectedCollection.checkedNumOf(item)
            if (checkedNum == CheckView.UNCHECKED) {
                if (assertAddSelection(holder.itemView.context, item)) {
                    selectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            } else {
                selectedCollection.remove(item)
                notifyCheckStateChanged()
            }
        } else {
            if (selectedCollection.isSelected(item)) {
                selectedCollection.remove(item)
                notifyCheckStateChanged()
            } else {
                if (selectionSpec.isSingleChoose()) {
                    selectedCollection.removeAll()
                    if (!assertAddSelection(holder.itemView.context, item)) return

                    selectedCollection.add(item)
                    notifyCheckStateChanged()
                } else {
                    if (!assertAddSelection(holder.itemView.context, item)) return

                    selectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            }
        }
    }

    /**
     * 单选
     */
    private fun singleCheckChanged() {
        // TODO
    }

    private fun notifyCheckStateChanged() {
        notifyDataSetChanged()
        if (checkStateListener != null) {
            checkStateListener?.onUpdate()
        }
    }

    private fun assertAddSelection(context: Context, item: Item): Boolean {
        val cause = selectedCollection.isAcceptable(item)
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
        var mediaGrid: MediaGrid = itemView as MediaGrid
    }

    class CaptureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var hint: TextView = itemView.findViewById(R.id.hint)
    }
}