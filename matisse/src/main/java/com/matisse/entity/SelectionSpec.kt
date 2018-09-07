package com.matisse.internal.entity

import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.engine.GlideEngine
import com.matisse.engine.ImageEngine
import com.matisse.filter.Filter

/**
 * Describe : Builder to get config values
 * Created by Leo on 2018/8/29 on 14:54.
 */
class SelectionSpec {
    lateinit var mimeTypeSet: Set<MimeType>
    var mediaTypeExclusive: Boolean = false
    var showSingleMediaType: Boolean = false
    lateinit var filters: List<Filter>
    var maxSelectable: Int = 0
    var maxImageSelectable: Int = 0
    var maxVideoSelectable: Int = 0
    var thumbnailScale: Float = 0.5f
    var countable: Boolean = false
    var capture: Boolean = false
    var gridExpectedSize: Int = 0
    var spanCount: Int = 3

    var imageEngine: ImageEngine = GlideEngine()

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

    private fun reset() {
        imageEngine = GlideEngine()
    }

    fun onlyShowImages(): Boolean {
        return showSingleMediaType && MimeTypeManager.ofImage().containsAll(mimeTypeSet)
    }

    fun onlyShowVideos(): Boolean {
        return showSingleMediaType && MimeTypeManager.ofVideo().containsAll(mimeTypeSet)
    }
}