@file:JvmName("ItemSelectUtils")

package com.matisse.utils

import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection

/**
 * 返回选中图片中，超过原图大小上限的图片数量
 * @param selectedCollection 资源选中操作类
 */
fun countOverMaxSize(selectedCollection: SelectedItemCollection): Int {
    var count = 0
    selectedCollection.asList().filter { it.isImage() }.forEach {
        val size = PhotoMetadataUtils.getSizeInMB(it.size)
        if (size > SelectionSpec.getInstance().originalMaxSize) count++
    }
    return count
}