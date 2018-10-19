package com.matisse.internal.entity

import android.content.pm.ActivityInfo
import android.support.annotation.StyleRes
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.R
import com.matisse.engine.ImageEngine
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.Item
import com.matisse.filter.Filter
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.matisse.widget.CropImageView
import java.io.File

/**
 * Describe : Builder to get config values
 * Created by Leo on 2018/8/29 on 14:54.
 */
class SelectionSpec {
    var mimeTypeSet: Set<MimeType>? = null
    var mediaTypeExclusive: Boolean = false
    var showSingleMediaType: Boolean = false
    var filters: List<Filter>? = null
    var maxSelectable: Int = 0
    var maxImageSelectable: Int = 0
    var maxVideoSelectable: Int = 0
    var thumbnailScale: Float = 0.5f
    var countable: Boolean = false
    var capture: Boolean = false
    var gridExpectedSize: Int = 0
    var spanCount: Int = 3
    var captureStrategy: CaptureStrategy? = null
    @StyleRes
    var themeId: Int = 0
    var orientation: Int = 0
    var originalable: Boolean = false
    var originalMaxSize: Int = 0
    var imageEngine: ImageEngine? = null
    var onSelectedListener: OnSelectedListener? = null
    var onCheckedListener: OnCheckedListener? = null

    var isCrop: Boolean = false                     // 裁剪
    var isCropSaveRectangle: Boolean = false        // 裁剪后的图片是否是矩形，否则跟随裁剪框的形状，只适用于圆形裁剪
    var cropOutPutX: Int = 300                      // 裁剪保存宽度
    var cropOutPutY: Int = 300                      // 裁剪保存高度
    var cropFocusWidth: Int = 0                     // 焦点框的宽度
    var cropFocusHeight: Int = 0                    // 焦点框的高度
    var cropStyle = CropImageView.Style.RECTANGLE   // 裁剪框的形状
    var cropCacheFolder: File? = null               // 裁剪后文件保存路径

    var hasInited: Boolean = false

    class InstanceHolder {
        companion object {
            val INSTANCE: SelectionSpec = SelectionSpec()
        }
    }

    companion object {
        fun getInstance() = InstanceHolder.INSTANCE

        fun getCleanInstance(): SelectionSpec {
            val selectionSpec = getInstance()
            selectionSpec.reset()
            return selectionSpec
        }
    }

    private fun reset() {
        mimeTypeSet = null
        mediaTypeExclusive = true
        showSingleMediaType = false
        themeId = R.style.Matisse_Default
        orientation = 0
        countable = false
        maxSelectable = 1
        maxImageSelectable = 0
        maxVideoSelectable = 0
        filters = null
        capture = false
        captureStrategy = null
        spanCount = 3
        gridExpectedSize = 0
        thumbnailScale = 0.5f

        imageEngine = null
        hasInited = true

        // crop
        isCrop = true
        isCropSaveRectangle = false
        cropOutPutX = 300
        cropOutPutY = 300
        cropFocusWidth = 0
        cropFocusHeight = 0
        cropStyle = CropImageView.Style.RECTANGLE

        // return original setting
        originalable = false
        originalMaxSize = Integer.MAX_VALUE
    }

    fun openCrop(): Boolean {
        return isCrop && maxSelectable == 1
    }

    fun isSupportCrop(item: Item?): Boolean {
        return item != null && item.isImage() && !item.isGif()
    }

    fun onlyShowImages(): Boolean {
        if (mimeTypeSet == null) {
            return false
        }
        return showSingleMediaType && MimeTypeManager.ofImage().containsAll(mimeTypeSet!!)
    }

    fun onlyShowVideos(): Boolean {
        if (mimeTypeSet == null) {
            return false
        }

        return showSingleMediaType && MimeTypeManager.ofVideo().containsAll(mimeTypeSet!!)
    }

    fun singleSelectionModeEnabled(): Boolean {
        return !countable && (maxSelectable == 1 || maxImageSelectable == 1 && maxVideoSelectable == 1)
    }

    fun needOrientationRestriction() = orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}