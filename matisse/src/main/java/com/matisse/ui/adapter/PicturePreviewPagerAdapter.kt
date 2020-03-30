package com.matisse.ui.adapter

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.matisse.R
import com.matisse.entity.Item

class PicturePreviewPagerAdapter(listener: OnPrimaryItemSetListener?) : PagerAdapter() {

    /**
     * 最大缓存图片数量
     */
    private val MAX_CACHE_SIZE = 18
    /**
     * 缓存view
     */
    private var mCacheView: SparseArray<View>? = null

    fun clear() {
        if (null != mCacheView) {
            mCacheView!!.clear()
            mCacheView = null
        }
    }

    var items: ArrayList<Item> = ArrayList()
    var kListener: OnPrimaryItemSetListener? = null

    init {
        this.kListener = listener
    }

    override fun getCount() = items.size

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
        if (mCacheView?.size() ?: 0 > MAX_CACHE_SIZE) {
            mCacheView?.remove(position)
        }
    }

    override fun isViewFromObject(view: View, `object`: Any) = view == `object`

    override fun instantiateItem(container: ViewGroup, position: Int): View {
        var contentView = mCacheView?.get(position)
        if (contentView == null) {
            contentView = LayoutInflater.from(container.context)
                .inflate(R.layout.fragment_preview_item, container, false)
            items[position].run {

            }
            mCacheView?.put(position, contentView)
        }
        container.addView(contentView, 0)
        return contentView!!
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        kListener?.onPrimaryItemSet(position)
    }

    fun getMediaItem(position: Int): Item? {
        if (count > position) {
            return items[position]
        }

        return null
    }

    fun addAll(items: List<Item>) {
        this.items.addAll(items)
    }

    interface OnPrimaryItemSetListener {
        fun onPrimaryItemSet(position: Int)
    }
}