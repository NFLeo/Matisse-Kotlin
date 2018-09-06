package com.matisse.widget

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.matisse.entity.Item
import com.matisse.entity.PreBindInfo

class MediaGrid : SquareFrameLayout {

    private var mThumbnail: ImageView? = null
    private var mCheckView: CheckView? = null
    private var mGifTag: ImageView? = null
    private var mVideoDuration: TextView? = null

    private var mMedia: Item? = null
    private var mPreBindInfo: PreBindInfo? = null
    private var mListener: OnMediaGridClickListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    interface OnMediaGridClickListener {
        fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: RecyclerView.ViewHolder)
        fun onCheckViewClicked(checkView:CheckBox, item: Item, holder: RecyclerView.ViewHolder)
    }
}