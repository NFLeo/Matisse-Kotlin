@file:JvmName("IntentUtils")

package com.matisse.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.matisse.entity.ConstValue
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.activity.ImageCropActivity

/**
 * 打开裁剪界面
 */
fun gotoImageCrop(activity: Activity, selectedPath: ArrayList<String>) {
    if (selectedPath.isEmpty()) return

    val intentCrop = Intent(activity, ImageCropActivity::class.java)
    intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPath[0])
    activity.startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
}

/**
 * 预览界面提交返回选中结果
 * @param originalEnable 是否原图
 * @param selectedUris 选中图的uri
 * @param selectedPaths 选中图的文件路径
 */
fun finishIntentFromPreview(
    activity: Activity, originalEnable: Boolean,
    selectedUris: ArrayList<Uri>, selectedPaths: ArrayList<String>
) {
    Intent().apply {
        putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selectedUris)
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths)
        putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        activity.setResult(Activity.RESULT_OK, this)
    }
}

/**
 * 裁剪完成返回裁剪结果
 * @param cropPath 需裁剪的图片路径
 */
fun finishIntentFromCrop(activity: Activity, cropPath: String?) {
    Intent().apply {
        putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, arrayListOf<Uri>())
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, arrayListOf(cropPath ?: ""))
        activity.setResult(Activity.RESULT_OK, this)
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
fun finishIntentFromCropSuccess(activity: Activity, filePath: String) {
    Intent().apply {
        putExtra(ConstValue.EXTRA_RESULT_BUNDLE, filePath)
        activity.setResult(Activity.RESULT_OK, this)
    }
    activity.finish()
}
