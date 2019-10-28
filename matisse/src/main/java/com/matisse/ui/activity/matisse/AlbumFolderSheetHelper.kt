package com.matisse.ui.activity.matisse

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.ui.view.FolderBottomSheet

class AlbumFolderSheetHelper(
    private var context: Context, private var sheetCallback: FolderBottomSheet.BottomSheetCallback
) {
    private var albumFolderCursor: Cursor? = null
    private var albumFolderList: ArrayList<Album>? = null
    private var bottomSheet: FolderBottomSheet? = null
    private var lastFolderCheckedPosition = 0

    fun createFolderSheetDialog() {
        bottomSheet = FolderBottomSheet.instance(
            context, lastFolderCheckedPosition, "Folder"
        )

        bottomSheet?.callback = sheetCallback
    }

    fun readAlbumFromCursor(): ArrayList<Album>? {
        if (albumFolderList?.size ?: 0 > 0) return albumFolderList

        if (albumFolderCursor == null) return null

        var allFolderCoverPath: Uri? = null
        var allFolderCount = 0L
        if (albumFolderList == null) {
            albumFolderList = arrayListOf()
        }

        albumFolderCursor?.moveToFirst()
        while (albumFolderCursor!!.moveToNext()) {
            val album = Album.valueOf(albumFolderCursor!!)
            if (albumFolderList?.size == 0) {
                allFolderCoverPath = album.getCoverPath()
            }
            albumFolderList?.add(album)
            allFolderCount += album.getCount()
        }
        albumFolderList?.add(
            0, Album(allFolderCoverPath, context.getString(R.string.album_name_all), allFolderCount)
        )
        return albumFolderList
    }

    fun insetAlbumToFolder(capturePath: Uri) {
        readAlbumFromCursor()

        albumFolderList?.apply {
            // 全部相册需添加一张
            this[0].addCaptureCount()
            this[0].setCoverPath(capturePath)

            /**
             * 拍照后图片保存在Pictures目录下
             * Pictures为空时，需手动创建
             */
            // TODO 2019/10/28 Leo 查询相册下图片需指定id，无法手动生成
//            val listDCIM: List<Album>? =
//                filter { Environment.DIRECTORY_PICTURES == it.getDisplayName(context) }
//            if (listDCIM == null || listDCIM.isEmpty()) {
//                albumFolderList?.add(Album(Environment.DIRECTORY_PICTURES, 0))
//            }

            // Pictures目录手动添加一张图片
            filter { Environment.DIRECTORY_PICTURES == it.getDisplayName(context) }.forEach {
                it.addCaptureCount()
                it.setCoverPath(capturePath)
            }
        }
    }

    /**
     * 记录上次选中位置
     * @return true=记录成功   false=记录失败
     */
    fun setLastFolderCheckedPosition(lastPosition: Int): Boolean {
        if (lastFolderCheckedPosition == lastPosition) return false
        lastFolderCheckedPosition = lastPosition
        return true
    }

    fun setAlbumFolderCursor(cursor: Cursor) {
        albumFolderCursor = cursor
        readAlbumFromCursor()
    }

    fun getAlbumFolderList() = albumFolderList

    fun clearFolderSheetDialog() {
        if (bottomSheet != null && bottomSheet?.adapter != null) {
            albumFolderCursor = null
            bottomSheet?.adapter?.setListData(null)
        }
    }
}