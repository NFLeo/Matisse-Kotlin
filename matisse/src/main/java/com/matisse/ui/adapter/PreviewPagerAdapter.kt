package com.matisse.ui.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import com.matisse.entity.Item
import com.matisse.ui.view.PreviewItemFragment

/**
 * Created by liubo on 2018/9/6.
 */
class PreviewPagerAdapter(manager: FragmentManager, listener: OnPrimaryItemSetListener?) : FragmentPagerAdapter(manager) {
    var mItems: ArrayList<Item> = ArrayList()
    var mListener: OnPrimaryItemSetListener? = null

    init {
        this.mListener = listener
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): Fragment {
        return PreviewItemFragment.newInstance(mItems[position])
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        mListener?.onPrimaryItemSet(position)
    }

    fun getMediaItem(position: Int): Item? {
        return mItems[position]
    }

    fun addAll(items: List<Item>) {
        mItems.addAll(items)
    }

    interface OnPrimaryItemSetListener {
        fun onPrimaryItemSet(position: Int)
    }
}