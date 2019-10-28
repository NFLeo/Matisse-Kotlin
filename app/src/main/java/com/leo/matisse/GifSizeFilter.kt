/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leo.matisse

import android.content.Context
import com.leo.matisse.R
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.filter.Filter
import com.matisse.utils.PhotoMetadataUtils
import java.util.*

class GifSizeFilter(private val mMinWidth: Int, private val mMinHeight: Int, private val mMaxSize: Int) : Filter() {

    override fun constraintTypes(): Set<MimeType> {
        return MimeTypeManager.ofImage()
    }

    override fun filter(context: Context, item: Item?): IncapableCause? {
        if (!needFiltering(context, item))
            return null
        val size = PhotoMetadataUtils.getBitmapBound(context.contentResolver, item?.getContentUri())
        return if (size.x < mMinWidth || size.y < mMinHeight || item?.size ?: 0 > mMaxSize) {
            IncapableCause(IncapableCause.DIALOG, context.getString(R.string.error_gif, mMinWidth,
                    PhotoMetadataUtils.getSizeInMB(mMaxSize.toLong()).toString()))
        } else null
    }
}
