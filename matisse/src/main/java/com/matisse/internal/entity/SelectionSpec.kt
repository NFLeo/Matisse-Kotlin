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
    var mediaTypeExclusive = false
    var showSingleMediaType = false
    var filters: List<Filter>? = null
    var maxSelectable = 0
    var maxImageSelectable = 0
    var maxVideoSelectable = 0
    var thumbnailScale = 0.5f
    var countable = false
    var capture = false
    var gridExpectedSize = 0
    var spanCount = 3
    var captureStrategy: CaptureStrategy? = null
    @StyleRes
    var themeId = 0
    var orientation = 0
    var originalable = false
    var originalMaxSize = 0
    var imageEngine: ImageEngine? = null
    var onSelectedListener: OnSelectedListener? = null
    var onCheckedListener: OnCheckedListener? = null

    var isCrop = false                              // 裁剪
    var isCropSaveRectangle = false                 // 裁剪后的图片是否是矩形，否则跟随裁剪框的形状，只适用于圆形裁剪
    var cropOutPutX = 300                           // 裁剪保存宽度
    var cropOutPutY = 300                           // 裁剪保存高度
    var cropFocusWidth = 0                          // 焦点框的宽度
    var cropFocusHeight = 0                         // 焦点框的高度
    var cropStyle = CropImageView.Style.RECTANGLE   // 裁剪框的形状
    var cropCacheFolder: File? = null               // 裁剪后文件保存路径

    var hasInited = false

    var isDarkStatus: Boolean = false

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

        isDarkStatus = false
    }

    fun openCrop() = isCrop && maxSelectable == 1

    fun isSupportCrop(item: Item?) = item != null && item.isImage() && !item.isGif()

    fun onlyShowImages() = if (mimeTypeSet != null)
        showSingleMediaType && MimeTypeManager.ofImage().containsAll(mimeTypeSet!!) else false

    fun onlyShowVideos() = if (mimeTypeSet != null)
        showSingleMediaType && MimeTypeManager.ofVideo().containsAll(mimeTypeSet!!) else false

    fun singleSelectionModeEnabled() =
        !countable && (maxSelectable == 1 || maxImageSelectable == 1 && maxVideoSelectable == 1)

    fun needOrientationRestriction() = orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}