package com.matisse.internal.entity

import android.content.pm.ActivityInfo
import androidx.annotation.StyleRes
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.R
import com.matisse.engine.ImageEngine
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.Item
import com.matisse.filter.Filter
import com.matisse.listener.NoticeConsumer
import com.matisse.listener.MFunction
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.matisse.ui.activity.BaseActivity
import com.matisse.widget.CropImageView
import java.io.File

/**
 * Describe : Builder to get config values
 * Created by Leo on 2018/8/29 on 14:54.
 */
class SelectionSpec {
    var mimeTypeSet: Set<MimeType>? = null
    var mediaTypeExclusive = false                      // 设置单种/多种媒体资源选择 默认支持多种
    var filters: MutableList<Filter>? = null
    var maxSelectable = 1
    var maxImageSelectable = 0
    var maxVideoSelectable = 0
    var thumbnailScale = 0.5f
    var countable = false
    var capture = false
    var gridExpectedSize = 0
    var spanCount = 3
    var captureStrategy: CaptureStrategy? = null
    @StyleRes
    var themeId = R.style.Matisse_Default
    var orientation = 0
    var originalable = false
    var originalMaxSize = 0
    var imageEngine: ImageEngine? = null
    var onSelectedListener: OnSelectedListener? = null
    var onCheckedListener: OnCheckedListener? = null

    var isCrop = false                              // 裁剪
    var isCropSaveRectangle = false                 // 裁剪后的图片是否是矩形，否则跟随裁剪框的形状，只适用于圆形裁剪
    var cropFocusWidthPx = 0                        // 焦点框的宽度
    var cropFocusHeightPx = 0                       // 焦点框的高度
    var cropStyle = CropImageView.Style.RECTANGLE   // 裁剪框的形状
    var cropCacheFolder: File? = null               // 裁剪后文件保存路径

    var hasInited = false                                   // 是否初始化完成

    var noticeConsumer: NoticeConsumer? = null              // 库内提示具体回调
    var statusBarFuture: MFunction<BaseActivity>? = null    // 状态栏处理回调

    var isInnerCompress = true                              // 是否开启内部压缩

    var lastChoosePictureIdsOrUris: ArrayList<String>? = null   // 上次选中的图片Id

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
        mediaTypeExclusive = false
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
        isCrop = false
        isCropSaveRectangle = false
        cropFocusWidthPx = 0
        cropFocusHeightPx = 0
        cropStyle = CropImageView.Style.RECTANGLE

        // return original setting
        originalable = false
        originalMaxSize = Integer.MAX_VALUE

        noticeConsumer = null
        statusBarFuture = null

        isInnerCompress = true

        lastChoosePictureIdsOrUris = null
    }

    // 是否可计数
    fun isCountable() = countable && !isSingleChoose()

    // 是否可单选
    fun isSingleChoose() =
        maxSelectable == 1 || (maxImageSelectable == 1 && maxVideoSelectable == 1)

    // 是否可裁剪
    fun openCrop() = isCrop && isSingleChoose()

    fun isSupportCrop(item: Item?) = item != null && item.isImage() && !item.isGif()

    // 是否单一资源选择方式
    fun isMediaTypeExclusive() =
        mediaTypeExclusive && (maxImageSelectable + maxVideoSelectable == 0)

    fun onlyShowImages() =
        if (mimeTypeSet != null) MimeTypeManager.ofImage().containsAll(mimeTypeSet!!) else false

    fun onlyShowVideos() =
        if (mimeTypeSet != null) MimeTypeManager.ofVideo().containsAll(mimeTypeSet!!) else false

    fun singleSelectionModeEnabled() = !countable && isSingleChoose()

    fun needOrientationRestriction() = orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}