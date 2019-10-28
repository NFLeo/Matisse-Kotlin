package com.matisse.ui.activity.matisse

import android.os.Bundle
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection

class AlbumLoadHelper(
    private var activity: MatisseActivity, private var albumLoadCallback: AlbumCallbacks
) {

    private var albumCollection: AlbumCollection? = null

    init {
        albumCollection = AlbumCollection()
        loadAlbumData()
    }

    fun loadAlbumData() {
        albumCollection?.apply {
            onCreate(activity, albumLoadCallback)
            activity.instanceState?.apply {
                albumCollection?.onRestoreInstanceState(this)
            }
            loadAlbums()
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        albumCollection?.onSaveInstanceState(outState)
    }

    /**
     * 设置当前选中位置，用于数据回收后恢复
     */
    fun setStateCurrentSelection(position: Int) {
        albumCollection?.setStateCurrentSelection(position)
    }

    fun onDestroy() {
        albumCollection?.onDestroy()
    }
}