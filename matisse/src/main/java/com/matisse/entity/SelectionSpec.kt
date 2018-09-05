package com.matisse.internal.entity

import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.filter.Filter

/**
 * Describe : Builder to get config values
 * Created by Leo on 2018/8/29 on 14:54.
 */
class SelectionSpec {
    lateinit var mimeTypeSet: Set<MimeType>
    var mediaTypeExclusive: Boolean = false
    var showSingleMediaType: Boolean = false
    var filters: List<Filter>? = null
    var maxSelectable: Int = 0
    var maxImageSelectable: Int = 0
    var maxVideoSelectable: Int = 0

    class InstanceHolder {
        companion object {
            val INSTANCE: SelectionSpec = SelectionSpec()
        }
    }

    companion object {
        fun getInstance() = InstanceHolder.INSTANCE

        fun getCleanInstance() = {
            val selectionSpec = getInstance()
            selectionSpec.reset()
            selectionSpec
        }
    }

    private fun reset() {}

    fun onlyShowImages(): Boolean {
        return showSingleMediaType && MimeTypeManager.ofImage().containsAll(mimeTypeSet)
    }

    fun onlyShowVideos(): Boolean {
        return showSingleMediaType && MimeTypeManager.ofVideo().containsAll(mimeTypeSet)
    }
}