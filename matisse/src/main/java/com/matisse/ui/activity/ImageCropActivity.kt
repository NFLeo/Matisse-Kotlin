package com.matisse.ui.activity

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import com.matisse.Matisse
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.utils.*
import com.matisse.widget.CropImageView
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_crop.*
import kotlinx.android.synthetic.main.include_view_navigation.*
import java.io.File
import kotlin.math.min

/**
 * desc：图片裁剪</br>
 * time: 2018/9/17-14:16</br>
 * author：Leo </br>
 * since V 1.0.0 </br>
 */
class ImageCropActivity : BaseActivity(), View.OnClickListener,
    CropImageView.OnBitmapSaveCompleteListener {

    private var bitmap: Bitmap? = null
    private var isSaveRectangle = false
    private var outputX = 0
    private var outputY = 0
    //    private lateinit var imagePath: String
    private lateinit var imageUri: Uri


    override fun getResourceLayoutId() = R.layout.activity_crop

    override fun configActivity() {
        super.configActivity()
        spec?.statusBarFuture?.invoke(this, toolbar)
    }

    override fun setViewData() {
        Matisse.obtainResult(intent)?.run {
            if (size > 0) imageUri = this[0] else return
        }

        setCropImageView()

        //缩放图片
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            getBitmapFromUri(this@ImageCropActivity, imageUri, this)
            resources.displayMetrics.let {
                inSampleSize = calculateInSampleSize(this, it.widthPixels, it.heightPixels)
            }

            inJustDecodeBounds = false
        }
        bitmap = getBitmapFromUri(this@ImageCropActivity, imageUri, options)
        bitmap?.let {
            // TODO Leo 2020-01-18 无法获取旋转信息
            val rotateBitmap = cv_crop_image.rotate(it, getBitmapDegree(imageUri.path).toFloat())
            // 设置默认旋转角度
            cv_crop_image.setImageBitmap(rotateBitmap)
        }
    }

    private fun setCropImageView() {
        spec?.apply {
            val cropFocusNormalWidth = getScreenWidth(activity) -
                    dp2px(activity, 30f).toInt()
            val cropFocusNormalHeight = getScreenHeight(activity) -
                    dp2px(activity, 200f).toInt()

            val cropWidth = if (cropFocusWidthPx in 1 until cropFocusNormalWidth)
                cropFocusWidthPx else cropFocusNormalWidth

            val cropHeight = if (cropFocusHeightPx in 1 until cropFocusNormalHeight)
                cropFocusHeightPx else cropFocusNormalHeight

            // 圆形裁剪时，取宽高最短边作为裁剪结果
            // 方形裁剪时，取焦点框尺寸作为裁剪结果尺寸
            if (cropStyle == CropImageView.Style.CIRCLE) {
                outputX = min(cropWidth, cropHeight)
                outputY = outputX
            } else {
                outputX = cropWidth
                outputY = cropHeight
            }
            isSaveRectangle = isCropSaveRectangle

            cv_crop_image.run {
                setFocusStyle(cropStyle)
                setFocusWidth(cropWidth)
                setFocusHeight(cropHeight)
            }
        }
    }

    override fun initListener() {
        setOnClickListener(this, button_complete, button_back)
        cv_crop_image.setOnBitmapSaveCompleteListener(this)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            inSampleSize = if (width > height) {
                width / reqWidth
            } else {
                height / reqHeight
            }
        }
        return inSampleSize
    }

    override fun onBitmapSaveSuccess(fileUri: Uri) {
        handleCauseTips(form = IncapableCause.LOADING, dismissLoading = true)
        finishIntentFromCropSuccess(this, fileUri)
    }

    override fun onBitmapSaveError(fileUri: Uri) {
        handleCauseTips(form = IncapableCause.LOADING, dismissLoading = true)
        val incapableDialog = IncapableDialog.newInstance("", getString(R.string.error_crop))
        incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
    }

    override fun onClick(v: View?) {
        when (v) {
            button_complete -> {
                handleCauseTips(form = IncapableCause.LOADING, dismissLoading = false)
                cv_crop_image.saveBitmapToFile(
                    getCropCacheFolder(this), outputX, outputY, isSaveRectangle
                )
            }
            button_back -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun getCropCacheFolder(context: Context): File {
        return if (spec?.cropCacheFolder != null && spec?.cropCacheFolder?.exists() == true
            && spec?.cropCacheFolder?.isDirectory == true
        ) {
            spec?.cropCacheFolder!!
        } else {
            File(context.cacheDir.toString() + "/Matisse/cropTemp/")
        }
    }

    override fun onDestroy() {
        cv_crop_image?.setOnBitmapSaveCompleteListener(null)
        if (null != bitmap && bitmap?.isRecycled == false) {
            bitmap?.recycle()
            bitmap = null
        }
        super.onDestroy()
    }
}