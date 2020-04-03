@file:JvmName("IntentUtils")

package com.matisse.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.matisse.entity.ConstValue
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.ucrop.UCrop
import com.matisse.utils.Platform.aboveAndroidTen
import java.io.File

/**
 * 打开裁剪界面
 */
fun gotoImageCrop(activity: Activity, selectedPath: ArrayList<Uri>?) {
    if (selectedPath == null || selectedPath.isEmpty()) return

    startCrop(activity, selectedPath[0])
}

/**
 * 去裁剪
 *
 * @param originalPath
 */
fun startCrop(activity: Activity, originalPath: Uri) {

    val path = getPath(activity, originalPath) ?: ""

    val spec = SelectionSpec.getInstance()

    val options = UCrop.Options()
        .setCircleDimmedLayer(spec.isCircleCrop)
        .setDragFrameEnabled(true)
        .setCompressionQuality(50)
        .setFreeStyleCropEnabled(true)
        .setShowCropFrame(true)
        .setShowCropGrid(!spec.isCircleCrop)

    val isAndroidQ = aboveAndroidTen()
    val imgType = if (isAndroidQ)
        getLastImgSuffix(getMimeType(activity, originalPath))
    else {
        getLastImgType(path)
    }

    val file = File(
        getDiskCacheDir(activity), getCreateFileName("IMG_") + imgType
    )

    UCrop.of(originalPath, Uri.fromFile(file))
        .withAspectRatio(1f, 1f)
        .withOptions(options)
        .start(activity)
}

/**
 * 处理预览界面提交返回选中结果
 * @param originalEnable 是否原图
 * @param selectedItems 选中的资源Item
 */
fun handleIntentFromPreview(
    activity: Activity, originalEnable: Boolean, selectedItems: List<Item>?
) {
    if (selectedItems == null) return

    val selectedUris = arrayListOf<Uri>()
    val selectedId = arrayListOf<String>()
    selectedItems.forEach {
        selectedUris.add(it.getContentUri())
        selectedId.add(it.id.toString())
    }

    finishIntentToMain(
        activity, selectedUris, selectedId, originalEnable
    )
}

/**
 * 处理预览界面提交返回选中结果
 * @param selectedUris 选中的资源uri
 * @param selectedId 选中的资源id
 */
private fun finishIntentToMain(
    activity: Activity, selectedUris: ArrayList<Uri>,
    selectedId: ArrayList<String>, originalEnable: Boolean
) {
    Intent().apply {
        putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selectedUris)
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_ID, selectedId)
        putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        activity.setResult(Activity.RESULT_OK, this)
    }
    activity.finish()
}

/**
 * 裁剪完成返回裁剪结果
 * @param cropUri 需裁剪的图片路径
 */
fun finishIntentFromCrop(activity: Activity, cropUri: Uri?) {
    cropUri?.run {
        Intent().apply {
            putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, arrayListOf(cropUri))
            activity.setResult(Activity.RESULT_OK, this)
            activity.finish()
        }
    }
}

/**
 * 预览界面提交或者返回时的Intent
 */
fun finishIntentFromPreviewApply(
    activity: Activity, apply: Boolean,
    selectedCollection: SelectedItemCollection, originalEnable: Boolean
) {
    Intent().apply {
        putExtra(ConstValue.EXTRA_RESULT_BUNDLE, selectedCollection.getDataWithBundle())
        putExtra(ConstValue.EXTRA_RESULT_APPLY, apply)
        putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        activity.setResult(Activity.RESULT_OK, this)
    }
    if (apply) activity.finish()
}

/**
 * 裁剪成功带回裁剪结果
 */
fun finishIntentFromCropSuccess(activity: Activity, cropResultUri: Uri) {
    Intent().apply {
        putExtra(ConstValue.EXTRA_RESULT_CROP_BACK_BUNDLE, cropResultUri)
        activity.setResult(Activity.RESULT_OK, this)
    }
    activity.finish()
}

/**
 * 处理预览返回数据刷新
 * @param isApplyData 正常返回/提交带回 true=提交带回  false=正常返回
 */
fun handlePreviewIntent(
    activity: Activity, data: Intent?, originalEnable: Boolean,
    isApplyData: Boolean, selectedCollection: SelectedItemCollection
) {
    data?.apply {
        val resultBundle = getBundleExtra(ConstValue.EXTRA_RESULT_BUNDLE)
        resultBundle?.apply {
            val collectionType = getInt(ConstValue.STATE_COLLECTION_TYPE)
            val selected: ArrayList<Item>? = getParcelableArrayList(ConstValue.STATE_SELECTION)
            selected?.apply {
                if (isApplyData) {
                    // 从预览界面确认提交过来
                    handleIntentFromPreview(activity, originalEnable, this)
                } else {
                    // 从预览界面返回过来
                    selectedCollection.overwrite(this, collectionType)
                }
            }
        }
    }
}