package com.matisse.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.matisse.entity.Item
import com.matisse.ui.view.PicturePreviewItemFragment

/**
 * Created by liubo on 2018/9/6.
 */
class PreviewPagerAdapter(manager: FragmentManager, listener: OnPrimaryItemSetListener?) :
    FragmentStatePagerAdapter(manager) {

    var items: ArrayList<Item> = ArrayList()
    var kListener: OnPrimaryItemSetListener? = null

    init {
        this.kListener = listener
    }

    override fun getCount() = items.size

    override fun getItem(position: Int) = PicturePreviewItemFragment.newInstance(items[position])

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