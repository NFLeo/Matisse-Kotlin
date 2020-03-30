package com.matisse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo.*
import android.os.Build
import android.view.View
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import com.matisse.engine.ImageEngine
import com.matisse.entity.CaptureStrategy
import com.matisse.filter.Filter
import com.matisse.internal.entity.SelectionSpec
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.matisse.ui.activity.BaseActivity
import com.matisse.ui.activity.matisse.MatisseActivity
import java.io.File

/**
 * Fluent API for building media select specification.
 * Constructs a new specification builder on the context.
 *
 * @param matisse   a requester context wrapper.
 * @param mimeTypes MIME type set to select.
 */
class SelectionCreator(
    private val matisse: Matisse, mimeTypes: Set<MimeType>, mediaTypeExclusive: Boolean
) {
    private val selectionSpec: SelectionSpec = SelectionSpec.getCleanInstance()

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @IntDef(
        SCREEN_ORIENTATION_UNSPECIFIED, SCREEN_ORIENTATION_LANDSCAPE,
        SCREEN_ORIENTATION_PORTRAIT, SCREEN_ORIENTATION_USER, SCREEN_ORIENTATION_BEHIND,
        SCREEN_ORIENTATION_SENSOR, SCREEN_ORIENTATION_NOSENSOR, SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        SCREEN_ORIENTATION_SENSOR_PORTRAIT, SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        SCREEN_ORIENTATION_REVERSE_PORTRAIT, SCREEN_ORIENTATION_FULL_SENSOR,
        SCREEN_ORIENTATION_USER_LANDSCAPE, SCREEN_ORIENTATION_USER_PORTRAIT,
        SCREEN_ORIENTATION_FULL_USER, SCREEN_ORIENTATION_LOCKED
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    internal annotation class ScreenOrientation

    init {
        selectionSpec.run {
            this.mimeTypeSet = mimeTypes
            this.mediaTypeExclusive = mediaTypeExclusive
            this.orientation = SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /**
     * Theme for media selecting Activity.
     *
     * There are two built-in themes:
     * you can define a custom theme derived from the above ones or other themes.
     *
     * @param themeId theme resource id. Default value is R.style.Matisse_Zhihu.
     * @return [SelectionCreator] for fluent API.
     */
    fun theme(@StyleRes themeId: Int) = this.apply { selectionSpec.themeId = themeId }

    /**
     * Show a auto-increased number or a check mark when user select media.
     *
     * @param countable true for a auto-increased number from 1, false for a check mark. Default
     * value is false.
     * @return [SelectionCreator] for fluent API.
     */
    fun countable(countable: Boolean) = this.apply { selectionSpec.countable = countable }

    /**
     * Maximum selectable count.
     * mediaTypeExclusive true
     *      use maxSelectable
     * mediaTypeExclusive false
     *      use maxImageSelectable and maxVideoSelectable
     * @param maxSelectable Maximum selectable count. Default value is 1.
     * @return [SelectionCreator] for fluent API.
     */
    fun maxSelectable(maxSelectable: Int) = this.apply {
        if (!selectionSpec.mediaTypeExclusive) return this
        require(maxSelectable >= 1) { "maxSelectable must be greater than or equal to one" }
        check(!(selectionSpec.maxImageSelectable > 0 || selectionSpec.maxVideoSelectable > 0)) {
            "already set maxImageSelectable and maxVideoSelectable"
        }
        selectionSpec.maxSelectable = maxSelectable
    }

    /**
     * Only useful when [SelectionSpec.mediaTypeExclusive] set true and you want to set different maximum
     * selectable files for image and video media types.
     *
     * @param maxImageSelectable Maximum selectable count for image.
     * @param maxVideoSelectable Maximum selectable count for video.
     * @return
     */
    fun maxSelectablePerMediaType(maxImageSelectable: Int, maxVideoSelectable: Int) = this.apply {
        if (selectionSpec.mediaTypeExclusive) return this
        require(!(maxImageSelectable < 1 || maxVideoSelectable < 1)) {
            "mediaTypeExclusive must be false and max selectable must be greater than or equal to one"
        }
        selectionSpec.maxSelectable = -1
        selectionSpec.maxImageSelectable = maxImageSelectable
        selectionSpec.maxVideoSelectable = maxVideoSelectable
    }

    /**
     * Add filter to filter each selecting item.
     *
     * @param filter [Filter]
     * @return [SelectionCreator] for fluent API.
     */
    fun addFilter(filter: Filter) = apply {
        if (selectionSpec.filters == null) selectionSpec.filters = mutableListOf()
        selectionSpec.filters?.add(filter)
    }

    /**
     * Determines whether the photo capturing is enabled or not on the media grid view.
     * If this value is set true, photo capturing entry will appear only on All Media's page.
     *
     * @param enable Whether to enable capturing or not. Default value is false;
     * @return [SelectionCreator] for fluent API.
     */
    fun capture(enable: Boolean) = this.apply { selectionSpec.capture = enable }

    /**
     * Show a original photo check options.Let users decide whether use original photo after select
     *
     * @param enable Whether to enable original photo or not
     * @return [SelectionCreator] for fluent API.
     */
    fun originalEnable(enable: Boolean) = this.apply { selectionSpec.originalable = enable }

    /**
     * Maximum original size,the unit is MB. Only useful when {link@originalEnable} set true
     *
     * @param size Maximum original size. Default value is Integer.MAX_VALUE
     * @return [SelectionCreator] for fluent API.
     */
    fun maxOriginalSize(size: Int) = this.apply { selectionSpec.originalMaxSize = size }

    /**
     * Capture strategy provided for the location to save photos including internal and external
     * storage and also a authority for [androidx.core.content.FileProvider].
     *
     * @param captureStrategy [CaptureStrategy], needed only when capturing is enabled.
     * @return [SelectionCreator] for fluent API.
     */
    fun captureStrategy(captureStrategy: CaptureStrategy) = this.apply {
        selectionSpec.captureStrategy = captureStrategy
    }

    /**
     * Set the desired orientation of this activity.
     *
     * @param orientation An orientation constant as used in [ScreenOrientation].
     * Default value is [android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT].
     * @return [SelectionCreator] for fluent API.
     * @see Activity.setRequestedOrientation
     */
    fun restrictOrientation(@ScreenOrientation orientation: Int) = this.apply {
        selectionSpec.orientation = orientation
    }

    /**
     * Set a fixed span count for the media grid. Same for different screen orientations.
     * This will be ignored when [.gridExpectedSize] is set.
     * [get gridExpectedSize first]
     * @param spanCount Requested span count.
     * @return [SelectionCreator] for fluent API.
     */
    fun spanCount(spanCount: Int) = this.apply {
        if (selectionSpec.gridExpectedSize > 0) return this
        selectionSpec.spanCount = spanCount
    }

    /**
     * Set expected size for media grid to adapt to different screen sizes. This won't necessarily
     * be applied cause the media grid should fill the view container. The measured media grid's
     * size will be as close to this value as possible.
     *
     * @param sizePx Expected media grid size in pixel.
     * @return [SelectionCreator] for fluent API.
     */
    fun gridExpectedSize(sizePx: Int) = this.apply { selectionSpec.gridExpectedSize = sizePx }

    /**
     * Photo thumbnail's scale compared to the View's size. It should be a float value in (0.0,1.0].
     *
     * @param scale Thumbnail's scale in (0.0, 1.0]. Default value is 0.5.
     * @return [SelectionCreator] for fluent API.
     */
    fun thumbnailScale(scale: Float) = this.apply {
        require(!(scale <= 0f || scale > 1f)) { "Thumbnail scale must be between (0.0, 1.0]" }
        selectionSpec.thumbnailScale = scale
    }

    /**
     * Provide an image engine.
     * There are two built-in image engines:
     * And you can implement your own image engine.
     *
     * @param imageEngine [ImageEngine]
     * @return [SelectionCreator] for fluent API.
     */
    fun imageEngine(imageEngine: ImageEngine) = this.apply {
        selectionSpec.imageEngine = imageEngine
        selectionSpec.imageEngine?.init(matisse.activity?.applicationContext!!)
    }

    /**
     * Whether to support crop
     * If this value is set true, it will support function crop.
     * @param crop Whether to support crop or not. Default value is false;
     * @return [SelectionCreator] for fluent API.
     */
    fun isCrop(crop: Boolean) = this.apply { selectionSpec.isCrop = crop }

    /**
     * isCircleCrop
     * default is RECTANGLE CROP
     */
    fun isCircleCrop(isCircle: Boolean) = this.apply {
        selectionSpec.isCircleCrop = isCircle
    }

    /**
     * provide file to save image after crop
     */
    fun cropCacheFolder(cropCacheFolder: File) = this.apply {
        selectionSpec.cropCacheFolder = cropCacheFolder
    }

    /**
     * Set listener for callback immediately when user select or unselect something.
     *
     * It's a redundant API with [Matisse.obtainResult],
     * we only suggest you to use this API when you need to do something immediately.
     *
     * @param listener [OnSelectedListener]
     * @return [SelectionCreator] for fluent API.
     */
    fun setOnSelectedListener(listener: OnSelectedListener?) = this.apply {
        selectionSpec.onSelectedListener = listener
    }

    /**
     * Set listener for callback immediately when user check or uncheck original.
     *
     * @param listener [OnSelectedListener]
     * @return [SelectionCreator] for fluent API.
     */
    fun setOnCheckedListener(listener: OnCheckedListener?) = this.apply {
        selectionSpec.onCheckedListener = listener
    }

    /**
     * set notice type for matisse
     */
    fun setNoticeConsumer(
        noticeConsumer: ((context: Context, noticeType: Int, title: String, message: String) -> Unit)?
    ) = this.apply {
        selectionSpec.noticeEvent = noticeConsumer
    }

    /**
     * set Status Bar
     */
    fun setStatusBarFuture(statusBarFunction: ((params: BaseActivity, view: View?) -> Unit)?) =
        this.apply {
            selectionSpec.statusBarFuture = statusBarFunction
        }

    /**
     * set last choose pictures ids
     * id is cursor id. not support crop picture
     * 预选中上次带回的图片
     * 注：暂时无法保持预选中图片的顺序
     */
    fun setLastChoosePicturesIdOrUri(list: ArrayList<String>?) = this.apply {
        selectionSpec.lastChoosePictureIdsOrUris = list
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    fun forResult(requestCode: Int) {
        val activity = matisse.activity ?: return

        val intent = Intent(activity, MatisseActivity::class.java)

        val fragment = matisse.fragment
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
