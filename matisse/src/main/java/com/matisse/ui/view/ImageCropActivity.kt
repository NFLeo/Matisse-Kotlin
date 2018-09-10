package com.matisse.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.matisse.R
import com.matisse.R.id.*
import com.matisse.entity.ConstValue
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.BitmapUtils
import com.matisse.utils.Platform
import com.matisse.utils.UIUtils
import com.matisse.widget.CropImageView
import java.io.File

class ImageCropActivity : AppCompatActivity(), View.OnClickListener, CropImageView.OnBitmapSaveCompleteListener {

    private lateinit var btnPreview:TextView
    private lateinit var btnApply:TextView
    private lateinit var cropImageView: CropImageView

    private var mBitmap: Bitmap? = null
    private var mIsSaveRectangle: Boolean = false
    private var mOutputX: Int = 0
    private var mOutputY: Int = 0
    private lateinit var mSpec: SelectionSpec
    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        mSpec = SelectionSpec.getInstance()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        if (Platform.hasKitKat19()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        imagePath = intent.getStringExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH)

        initView()
        initCropFun()
    }

    private fun initView() {

        btnPreview = findViewById(button_preview)
        btnApply = findViewById(button_apply)
        cropImageView = findViewById(cv_crop_image)

        btnPreview.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnApply.text = getString(R.string.button_ok)
        btnApply.setOnClickListener(this)
    }

    private fun initCropFun() {
        mOutputX = mSpec.cropOutPutX
        mOutputY = mSpec.cropOutPutY
        mIsSaveRectangle = mSpec.isCropSaveRectangle

        val cropFocusNormalWidth = UIUtils.getScreenWidth(this) - UIUtils.dp2px(this, 50f).toInt()
        val cropFocusNormalHeight = UIUtils.getScreenHeight(this) - UIUtils.dp2px(this, 400f).toInt()

        val cropWidth = if (mSpec.cropFocusWidth in 1..(cropFocusNormalWidth - 1))
            mSpec.cropFocusWidth else cropFocusNormalWidth

        val cropHeight = if (mSpec.cropFocusHeight in 1..(cropFocusNormalHeight - 1))
            mSpec.cropFocusHeight else cropFocusNormalHeight

        cropImageView.setFocusStyle(mSpec.cropStyle)
        cropImageView.setFocusWidth(cropWidth)
        cropImageView.setFocusHeight(cropHeight)
        cropImageView.setOnBitmapSaveCompleteListener(this)

        //缩放图片
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        val displayMetrics = resources.displayMetrics
        options.inSampleSize = calculateInSampleSize(options, displayMetrics.widthPixels, displayMetrics.heightPixels)
        options.inJustDecodeBounds = false
        mBitmap = BitmapFactory.decodeFile(imagePath, options)
        //设置默认旋转角度
        cropImageView.setImageBitmap(cropImageView.rotate(mBitmap!!, BitmapUtils.getBitmapDegree(imagePath).toFloat()))
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
        Toast.makeText(this, "Crop Success! " + file.absolutePath, Toast.LENGTH_SHORT).show()
        val intent = Intent()
        intent.putExtra(ConstValue.EXTRA_RESULT_BUNDLE, file.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBitmapSaveError(file: File) {}

    override fun onClick(v: View?) {
        when (v) {
            btnApply -> cropImageView.saveBitmapToFile(getCropCacheFolder(this), mOutputX, mOutputY, mIsSaveRectangle)
        }
    }

    private fun getCropCacheFolder(context: Context): File {
        return File(context.cacheDir.toString() + "/Matisse/cropTemp/")
    }

    override fun onDestroy() {
        super.onDestroy()
        cropImageView.setOnBitmapSaveCompleteListener(null)
        if (null != mBitmap && !mBitmap?.isRecycled!!) {
            mBitmap?.recycle()
            mBitmap = null
        }
    }
}