package com.matisse.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.utils.UIUtils
import com.matisse.widget.CheckView
import com.matisse.widget.MediaGrid

class AlbumMediaAdapter(
    private var context: Context, private var selectedCollection: SelectedItemCollection,
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
        return when (viewType) {
            VIEW_TYPE_CAPTURE -> {
                val v = layoutInflater.inflate(R.layout.item_photo_capture, parent, false)
                CaptureViewHolder(v).run {
                    itemView.setOnClickListener {
                        if (it.context is OnPhotoCapture) (it.context as OnPhotoCapture).capture()
                    }
                    this
                }
            }
            else -> {
                val v = layoutInflater.inflate(R.layout.item_media_grid, parent, false)
                MediaViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        holder.apply {
            when (this) {
                is CaptureViewHolder ->
                    UIUtils.setTextDrawable(itemView.context, hint, R.attr.Media_Camera_textColor)
                is MediaViewHolder -> {
                    val item = Item.valueOf(cursor, position)
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

    /**
     * 初始化选择框选中状态
     */
    private fun setCheckStatus(item: Item, mediaGrid: MediaGrid) {
        // 初始化时 添加上次选中的图片
        setLastChooseItems(item)
        if (selectionSpec.isCountable()) {
            val checkedNum = selectedCollection.checkedNumOf(item)

            if (checkedNum > 0) {
                mediaGrid.setCheckedNum(checkedNum)
            } else {
                mediaGrid.setCheckedNum(
                    if (selectedCollection.maxSelectableReached(item)) CheckView.UNCHECKED else checkedNum
                )
            }
        } else {
            mediaGrid.setChecked(selectedCollection.isSelected(item))
        }
    }

    override fun onThumbnailClicked(
        thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder
    ) {
        onMediaClickListener?.onMediaClick(null, item, holder.adapterPosition)
    }

    /**
     * 单选：
     *     a.选中：刷新当前项与上次选择项
     *     b.取消选中：刷新当前项与上次选择项
     *
     * 多选：
     *      1. 按序号计数
     *          a.选中：仅刷新选中的item
     *          b.取消选中：
     *              取消最后一位：仅刷新当前操作的item
     *              取消非最后一位：刷新所有选中的item
     *      2. 无序号计数
     *          a.选中：仅刷新选中的item
     *          b.取消选中：仅刷新选中的item
     */
    override fun onCheckViewClicked(
        checkView: CheckView, item: Item, holder: RecyclerView.ViewHolder
    ) {
        if (selectionSpec.isSingleChoose()) {
            notifySingleChooseData(item)
        } else {
            notifyMultiChooseData(item)
        }
    }

    /**
     * 单选刷新数据
     */
    private fun notifySingleChooseData(item: Item) {
        if (selectedCollection.isSelected(item)) {
            selectedCollection.remove(item)
            notifyItemChanged(item.positionInList)
        } else {
            notifyLastItem()
            if (!addItem(item)) return
            notifyItemChanged(item.positionInList)
        }
        notifyCheckStateChanged()
    }

    private fun notifyLastItem() {
        val itemLists = selectedCollection.asList()
        if (itemLists.size > 0) {
            selectedCollection.remove(itemLists[0])
            notifyItemChanged(itemLists[0].positionInList)
        }
    }

    /**
     * 多选刷新数据
     *      1. 按序号计数
     *          a.选中：仅刷新选中的item
     *          b.取消选中：
     *              取消最后一位：仅刷新当前操作的item
     *              取消非最后一位：刷新所有选中的item
     *      2. 无序号计数
     *          a.选中：仅刷新选中的item
     *          b.取消选中：仅刷新选中的item
     */
    private fun notifyMultiChooseData(item: Item) {
        if (selectionSpec.isCountable()) {
            if (notifyMultiCountableItem(item)) return
        } else {
            if (selectedCollection.isSelected(item)) {
                selectedCollection.remove(item)
            } else {
                if (!addItem(item)) return
            }

            notifyItemChanged(item.positionInList)
        }

        notifyCheckStateChanged()
    }

    /**
     * @return 是否拦截 true=拦截  false=不拦截
     */
    private fun notifyMultiCountableItem(item: Item): Boolean {
        val checkedNum = selectedCollection.checkedNumOf(item)
        if (checkedNum == CheckView.UNCHECKED) {
            if (!addItem(item)) return true
            notifyItemChanged(item.positionInList)
        } else {
            selectedCollection.remove(item)
            // 取消选中中间序号时，刷新所有选中item
            if (checkedNum != selectedCollection.count() + 1) {
                selectedCollection.asList().forEach {
                    notifyItemChanged(it.positionInList)
                }
            }
            notifyItemChanged(item.positionInList)
        }
        return false
    }

    private fun notifyCheckStateChanged() {
        checkStateListener?.onSelectUpdate()
    }

    private fun addItem(item: Item): Boolean {
        if (!assertAddSelection(context, item)) return false
        selectedCollection.add(item)
        return true
    }

    private fun assertAddSelection(context: Context, item: Item): Boolean {
        val cause = selectedCollection.isAcceptable(item)
        UIUtils.handleCause(context, cause)
        return cause == null
    }

    /**
     * 初始化外部传入上次选中的图片
     */
    private fun setLastChooseItems(item: Item) {
        if (selectionSpec.lastChoosePictureIdsOrUris == null) return

        selectionSpec.lastChoosePictureIdsOrUris?.forEachIndexed { index, s ->
            if (s == item.id.toString() || s == item.getContentUri().toString()) {
                selectedCollection.add(item)
                selectionSpec.lastChoosePictureIdsOrUris!![index] = ""
            }
        }
    }

    interface CheckStateListener {
        fun onSelectUpdate()
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