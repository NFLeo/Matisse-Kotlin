package com.leo.matisse

import android.content.Context
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.filter.Filter
import com.matisse.utils.PhotoMetadataUtils

class SizeFilter(private val maxSizeByte: Int) : Filter() {

    override fun constraintTypes(): Set<MimeType> {
        return MimeTypeManager.ofMotionlessImage()
    }

    override fun filter(context: Context, item: Item?): IncapableCause? {
        if (!needFiltering(context, item))
            return null
        return if (item?.size ?: 0 > maxSizeByte) {
            IncapableCause(
                IncapableCause.TOAST,
                "not larger than ${PhotoMetadataUtils.getSizeInMB(maxSizeByte.toLong())} MB"
            )
        } else null
    }
}
