@file:JvmName("IntentUtils")

package com.matisse.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.matisse.compress.CompressHelper
import com.matisse.compress.FileUtil
import com.matisse.entity.ConstValue
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.activity.ImageCropActivity

/**
 * 打开裁剪界面
 */
fun gotoImageCrop(activity: Activity, selectedPath: ArrayList<String>?) {
    if (selectedPath == null || selectedPath.isEmpty()) return

    val intentCrop = Intent(activity, ImageCropActivity::class.java)
    intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPath[0])
    activity.startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
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
    val selectedPaths = arrayListOf<String>()
    val selectedId = arrayListOf<String>()
    selectedItems.forEach {
        selectedUris.add(it.getContentUri())
        selectedId.add(it.id.toString())
        selectedPaths.add(PathUtils.getPath(activity, it.getContentUri()) ?: "")
    }

    var compressPicture: ArrayList<String>? = null
    if (SelectionSpec.getInstance().isInnerCompress) {
        compressPicture = compressPicture(activity, selectedUris, selectedPaths)
    }

    finishIntentToMain(
        activity, selectedUris, selectedPaths, selectedId,
        originalEnable, compressPicture
    )
}

/**
 * 处理预览界面提交返回选中结果
 * @param originalEnable 是否原图
 * @param selectedUris 选中的资源uri
 * @param selectedPaths 选中的资源path
 * @param selectedId 选中的资源id
 */
private fun finishIntentToMain(
    activity: Activity, selectedUris: ArrayList<Uri>, selectedPaths: ArrayList<String>,
    selectedId: ArrayList<String>, originalEnable: Boolean, compressPicture: ArrayList<String>?
) {
    Intent().apply {
        putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selectedUris)
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths)
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_COMPRESS, compressPicture)
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_ID, selectedId)
        putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        activity.setResult(Activity.RESULT_OK, this)
    }
    activity.finish()
}

/**
 * 裁剪完成返回裁剪结果
 * @param cropPath 需裁剪的图片路径
 */
fun finishIntentFromCrop(activity: Activity, cropPath: String?) {
    if (cropPath == null || cropPath == "") return

    var compressPicture = ""
    if (SelectionSpec.getInstance().isInnerCompress) {
        compressPicture = CompressHelper.getDefault(activity)
            ?.compressToFile(FileUtil.getFileByPath(cropPath))?.path ?: cropPath
    }

    Intent().apply {
        putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, arrayListOf<Uri>())
        putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, arrayListOf(cropPath))
        putStringArrayListExtra(
            ConstValue.EXTRA_RESULT_SELECTION_COMPRESS, arrayListOf(compressPicture)
        )
        activity.setResult(Activity.RESULT_OK, this)
        activity.finish()
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

/**
 * 内部压缩图片
 * @param selectedPaths 文件压缩不成功时，用该集合填充对应位置
 * @return 压缩后的文件地址
 */
fun compressPicture(
    activity: Activity, selectedUri: ArrayList<Uri>, selectedPaths: ArrayList<String>
): ArrayList<String>? {
    if (selectedUri.isEmpty()) return null

    var filePathList: ArrayList<String>? = null
    selectedUri.forEachIndexed { index, uri ->
        if (filePathList == null) filePathList = arrayListOf()
        val file1 = CompressHelper.getDefault(activity)
            ?.compressToFile(uri)?.path ?: selectedPaths[index]
        filePathList?.add(file1)
    }

    return filePathList
}
