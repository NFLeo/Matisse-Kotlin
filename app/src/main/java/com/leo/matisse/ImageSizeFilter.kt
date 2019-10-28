package com.leo.matisse

import android.content.Context
import com.matisse.MimeTypeManager
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.filter.Filter
import com.matisse.utils.PhotoMetadataUtils

/**
 * desc：不允许选择大于itemSize字节的图片</br>
 * time: 2019/10/23-10:12</br>
 * author：Leo </br>
 * since V 1.2.2 </br>
 */
class ImageSizeFilter(private var itemSize: Long) : Filter() {

    // 设置需过滤的资源类型,此处过滤类型为图片
    override fun constraintTypes() = MimeTypeManager.ofImage()

    override fun filter(context: Context, item: Item?): IncapableCause? {
        // 1. 判断当前选中的item是否属于constraintTypes中定义的图片资源
        if (!needFiltering(context, item)) return null

        if (item?.size ?: 0 > itemSize) {
            return IncapableCause(
                IncapableCause.DIALOG, "需选择小于${PhotoMetadataUtils.getSizeInMB(itemSize)}M的图片"
            )
        }

        return null
    }
}