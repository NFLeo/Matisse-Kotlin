package com.matisse.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.matisse.entity.Item
import com.matisse.ui.view.PreviewItemFragment

/**
 * Created by liubo on 2018/9/6.
 */
class PreviewPagerAdapter(manager: FragmentManager, listener: OnPrimaryItemSetListener?) :
    FragmentPagerAdapter(manager, 0) {

    var items: ArrayList<Item> = ArrayList()
    var kListener: OnPrimaryItemSetListener? = null

    init {
        this.kListener = listener
    }

    override fun getCount() = items.size

    override fun getItem(position: Int) = PreviewItemFragment.newInstance(items[position])

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