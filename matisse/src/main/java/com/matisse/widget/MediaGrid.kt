package com.matisse.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
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

    private lateinit var mMedia: Item
    private lateinit var mPreBindInfo: PreBindInfo
    lateinit var mListener: OnMediaGridClickListener

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_media_grid_content, this, true)

        media_thumbnail.setOnClickListener(this)
        check_view.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v) {
            media_thumbnail -> mListener.onThumbnailClicked(media_thumbnail, mMedia, mPreBindInfo.mViewHolder)
            check_view -> mListener.onCheckViewClicked(check_view, mMedia, mPreBindInfo.mViewHolder)
        }
    }

    interface OnMediaGridClickListener {
        fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder)
        fun onCheckViewClicked(checkView: CheckView, item: Item, holder: RecyclerView.ViewHolder)
    }

    fun preBindMedia(info: PreBindInfo) {
        mPreBindInfo = info
    }

    fun bindMedia(item: Item) {
        mMedia = item
        setGifTag()
        initCheckView()
        setImage()
        setVideoDuration()
    }

    private fun setGifTag() {
        UIUtils.setViewVisible(mMedia.isGif(), gif)
    }

    private fun initCheckView() {
        check_view.setCountable(mPreBindInfo.mCheckViewCountable)
    }

    fun setCheckEnabled(enabled: Boolean) {
        check_view.isEnabled = enabled
    }

    fun setCheckedNum(checkedNum: Int) {
        check_view.setCheckedNum(checkedNum)
    }

    fun setChecked(checked: Boolean) {
        check_view.setChecked(checked)
    }

    private fun setImage() {
        if (mMedia.isGif()) {
            SelectionSpec.getInstance().imageEngine?.loadGifThumbnail(context, mPreBindInfo.mResize,
                    mPreBindInfo.mPlaceholder, media_thumbnail, mMedia.getContentUri())
        } else {
            SelectionSpec.getInstance().imageEngine?.loadThumbnail(context, mPreBindInfo.mResize,
                    mPreBindInfo.mPlaceholder, media_thumbnail, mMedia.getContentUri())
        }
    }

    private fun setVideoDuration() {
        if (mMedia.isVideo()) {
            UIUtils.setViewVisible(true, video_duration)
            video_duration.text = DateUtils.formatElapsedTime(mMedia.duration / 1000)
        } else {
            UIUtils.setViewVisible(false, video_duration)
        }
    }

    class PreBindInfo(var mResize: Int, var mPlaceholder: Drawable, var mCheckViewCountable: Boolean, var mViewHolder: RecyclerView.ViewHolder)
}