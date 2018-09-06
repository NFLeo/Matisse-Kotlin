package com.matisse.widget

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.matisse.R
import com.matisse.entity.Item
import com.matisse.entity.PreBindInfo
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.UIUtils

class MediaGrid : SquareFrameLayout, View.OnClickListener {

    private var mThumbnail: ImageView
    private var mCheckView: CheckView
    private var mGifTag: ImageView
    private var mVideoDuration: TextView

    private lateinit var mMedia: Item
    private lateinit var mPreBindInfo: PreBindInfo
    lateinit var mListener: OnMediaGridClickListener

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_media_grid_content, this, true)

        mThumbnail = findViewById(R.id.media_thumbnail)
        mCheckView = findViewById(R.id.check_view)
        mGifTag = findViewById(R.id.gif)
        mVideoDuration = findViewById(R.id.video_duration)

        mThumbnail.setOnClickListener(this)
        mCheckView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v) {
            mThumbnail -> mListener.onThumbnailClicked(mThumbnail, mMedia, mPreBindInfo.mViewHolder)
            mCheckView -> mListener.onCheckViewClicked(mCheckView, mMedia, mPreBindInfo.mViewHolder)
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
        UIUtils.setViewVisible(mMedia.isGif(), mGifTag)
    }

    private fun initCheckView() {
        mCheckView.setCountable(mPreBindInfo.mCheckViewCountable)
    }

    fun setCheckEnabled(enabled: Boolean) {
        mCheckView.isEnabled = enabled
    }

    fun setCheckedNum(checkedNum: Int) {
        mCheckView.setCheckedNum(checkedNum)
    }

    fun setChecked(checked: Boolean) {
        mCheckView.setChecked(checked)
    }

    private fun setImage() {
        if (mMedia.isGif()) {
            SelectionSpec.getInstance().imageEngine.loadGifThumbnail(context, mPreBindInfo.mResize,
                    mPreBindInfo.mPlaceholder, mThumbnail, mMedia.getContentUri()!!)
        } else {
            SelectionSpec.getInstance().imageEngine.loadThumbnail(context, mPreBindInfo.mResize,
                    mPreBindInfo.mPlaceholder, mThumbnail, mMedia.getContentUri()!!)
        }
    }

    private fun setVideoDuration() {
        if (mMedia.isVideo()) {
            UIUtils.setViewVisible(true, mVideoDuration)
            mVideoDuration.text = DateUtils.formatElapsedTime(mMedia.duration / 1000)
        } else {
            UIUtils.setViewVisible(false, mVideoDuration)
        }
    }
}