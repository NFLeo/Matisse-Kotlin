package com.matisse.widget

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.matisse.R
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.UIUtils
import kotlinx.android.synthetic.main.view_media_grid_content.view.*

class MediaGrid : SquareFrameLayout, View.OnClickListener {

    private lateinit var media: Item
    private lateinit var preBindInfo: PreBindInfo
    lateinit var listener: OnMediaGridClickListener

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.view_media_grid_content, this, true)

        media_thumbnail.setOnClickListener(this)
        check_view.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v) {
            media_thumbnail -> listener.onThumbnailClicked(
                media_thumbnail, media, preBindInfo.viewHolder
            )
            check_view -> listener.onCheckViewClicked(check_view, media, preBindInfo.viewHolder)
        }
    }

    interface OnMediaGridClickListener {
        fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder)
        fun onCheckViewClicked(checkView: CheckView, item: Item, holder: RecyclerView.ViewHolder)
    }

    fun preBindMedia(info: PreBindInfo) {
        preBindInfo = info
    }

    fun bindMedia(item: Item) {
        media = item
        setGifTag()
        initCheckView()
        setImage()
        setVideoDuration()
    }

    private fun setGifTag() {
        UIUtils.setViewVisible(media.isGif(), gif)
    }

    private fun initCheckView() {
        check_view.setCountable(preBindInfo.checkViewCountable)
    }

    fun setCheckedNum(checkedNum: Int) {
        check_view.setCheckedNum(checkedNum)
    }

    fun setChecked(checked: Boolean) {
        check_view.setChecked(checked)
    }

    private fun setImage() {
        if (media.isGif()) {
            SelectionSpec.getInstance().imageEngine?.loadGifThumbnail(
                context, preBindInfo.resize, preBindInfo.placeholder,
                media_thumbnail, media.getContentUri()
            )
        } else {
            SelectionSpec.getInstance().imageEngine?.loadThumbnail(
                context, preBindInfo.resize, preBindInfo.placeholder,
                media_thumbnail, media.getContentUri()
            )
        }
    }

    private fun setVideoDuration() {
        if (media.isVideo()) {
            UIUtils.setViewVisible(true, video_duration)
            video_duration.text = DateUtils.formatElapsedTime(media.duration / 1000)
        } else {
            UIUtils.setViewVisible(false, video_duration)
        }
    }

    class PreBindInfo(
        var resize: Int, var placeholder: Drawable?,
        var checkViewCountable: Boolean, var viewHolder: RecyclerView.ViewHolder
    )
}