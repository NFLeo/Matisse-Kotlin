package com.matisse.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.gyf.barlibrary.ImmersionBar
import com.matisse.R
import com.matisse.entity.ConstValue
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.BitmapUtils
import com.matisse.utils.Platform
import com.matisse.utils.UIUtils
import com.matisse.widget.CropImageView
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_crop.*
import kotlinx.android.synthetic.main.include_view_navigation.*
import java.io.File

class ImageCropActivity : AppCompatActivity(), View.OnClickListener,
    CropImageView.OnBitmapSaveCompleteListener {

    private var bitmap: Bitmap? = null
    private var isSaveRectangle: Boolean = false
    private var outputX: Int = 0
    private var outputY: Int = 0
    private lateinit var spec: SelectionSpec
    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        spec = SelectionSpec.getInstance()
        setTheme(spec.themeId)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crop)

        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).titleBar(toolbar)?.statusBarDarkFont(spec.isDarkStatus)?.init()
        }

        if (spec.needOrientationRestriction()) {
            requestedOrientation = spec.orientation
        }

        imagePath = intent.getStringExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH)

        initView()
        initCropFun()
    }

    private fun initView() {
        button_complete.setOnClickListener(this)
        button_back.setOnClickListener(this)
    }

    private fun initCropFun() {
        outputX = spec.cropOutPutX
        outputY = spec.cropOutPutY
        isSaveRectangle = spec.isCropSaveRectangle

        val cropFocusNormalWidth =
            UIUtils.getScreenWidth(this) - UIUtils.dp2px(this, 50f).toInt()
        val cropFocusNormalHeight =
            UIUtils.getScreenHeight(this) - UIUtils.dp2px(this, 400f).toInt()

        val cropWidth = if (spec.cropFocusWidth in 1 until cropFocusNormalWidth)
            spec.cropFocusWidth else cropFocusNormalWidth

        val cropHeight = if (spec.cropFocusHeight in 1 until cropFocusNormalHeight)
            spec.cropFocusHeight else cropFocusNormalHeight

        cv_crop_image.setFocusStyle(spec.cropStyle)
        cv_crop_image.setFocusWidth(cropWidth)
        cv_crop_image.setFocusHeight(cropHeight)
        cv_crop_image.setOnBitmapSaveCompleteListener(this)

        //缩放图片
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        val displayMetrics = resources.displayMetrics
        options.inSampleSize =
            calculateInSampleSize(options, displayMetrics.widthPixels, displayMetrics.heightPixels)
        options.inJustDecodeBounds = false
        bitmap = BitmapFactory.decodeFile(imagePath, options)
        bitmap?.let {
            val rotateBitmap =
                cv_crop_image.rotate(it, BitmapUtils.getBitmapDegree(imagePath).toFloat())
            // 设置默认旋转角度
            cv_crop_image.setImageBitmap(rotateBitmap)
        }
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

    override fun onBitmapSaveSuccess(file: File) {
        val intent = Intent()
        intent.putExtra(ConstValue.EXTRA_RESULT_BUNDLE, file.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBitmapSaveError(file: File) {
        val incapableDialog = IncapableDialog.newInstance(
            "", getString(R.string.error_crop)
        )
        incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
    }

    override fun onClick(v: View?) {
        when (v) {
            button_complete -> cv_crop_image.saveBitmapToFile(
                getCropCacheFolder(this), outputX, outputY, isSaveRectangle
            )
            button_back -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun getCropCacheFolder(context: Context): File {
        return if (spec.cropCacheFolder != null && spec.cropCacheFolder?.exists() == true && spec.cropCacheFolder?.isDirectory == true) {
            spec.cropCacheFolder!!
        } else {
            File(context.cacheDir.toString() + "/Matisse/cropTemp/")
        }
    }

    override fun onDestroy() {
        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).destroy()
        }
        cv_crop_image.setOnBitmapSaveCompleteListener(null)
        if (null != bitmap && !bitmap?.isRecycled!!) {
            bitmap?.recycle()
            bitmap = null
        }
        super.onDestroy()
    }
}