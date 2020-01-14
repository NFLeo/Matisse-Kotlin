package com.leo.matisse

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gyf.barlibrary.ImmersionBar
import com.matisse.Matisse
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.SelectionCreator
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.ConstValue
import com.matisse.entity.IncapableCause
import com.matisse.filter.Filter
import com.matisse.listener.MFunction
import com.matisse.listener.NoticeConsumer
import com.matisse.ui.activity.BaseActivity
import com.matisse.utils.MediaStoreCompat
import com.matisse.utils.PathUtils
import com.matisse.utils.Platform
import com.matisse.utils.gotoImageCrop
import com.matisse.widget.CropImageView
import com.matisse.widget.IncapableDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_example.*
import java.util.*

class ExampleActivity : AppCompatActivity(), View.OnClickListener {
    private var showType = MimeTypeManager.ofAll()
    private var showCustomizeType: MutableList<MimeType>? = null
    private var mediaTypeExclusive = true
    private var isSingleChoose = false
    private var isCountable = true
    private var defaultTheme = R.style.Matisse_Default
    private var maxCount = 5
    private var maxImageCount = 1
    private var maxVideoCount = 1
    private var isOpenCamera = false
    private var spanCount = 3
    private var gridSizePx = 0
    private var isInnerCompress = true
    private var isCrop = false
    private var cropWidth = -1
    private var cropHeight = -1
    private var isSaveRectangle = false
    private var cropType = CropImageView.Style.RECTANGLE

    private var isColumnNum = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        initListener()
    }

    private fun initListener() {
        rg_show.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnAll -> showType = MimeTypeManager.ofAll()
                R.id.btnVideo -> showType = MimeTypeManager.ofVideo()
                R.id.btnImage -> showType = MimeTypeManager.ofImage()
            }
        }

        rg_media_exclusive.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnMixed -> {
                    mediaTypeExclusive = false
                    ev_max_1.visibility = View.VISIBLE
                    ev_max_2.visibility = if (isSingleChoose) View.GONE else View.VISIBLE
                    tv_max_1.text = "图片最大选择数"
                }
                R.id.btnExclusive -> {
                    mediaTypeExclusive = true
                    ev_max_1.visibility = View.VISIBLE
                    ev_max_2.visibility = View.GONE
                    tv_max_1.text = "最大可选择数"
                }
            }
        }

        rg_theme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_normal_theme -> defaultTheme = R.style.Matisse_Default
                R.id.btn_customize_theme -> defaultTheme = R.style.CustomMatisseStyle
                R.id.btn_jc_theme -> defaultTheme = R.style.JCStyle
            }
        }

        rg_column.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_num_column -> {
                    isColumnNum = true
                    ev_column.setText("3")
                }
                R.id.btn_size_column -> {
                    isColumnNum = false
                    ev_column.setText("300")
                }
            }
        }

        if (showCustomizeType != null && showCustomizeType?.size ?: 0 > 0) {
            showType =
                MimeTypeManager.of(showCustomizeType!![0], showCustomizeType?.toTypedArray()!!)
        }

        switch_choose_type.setOnCheckedChangeListener { _, isChecked ->
            isSingleChoose = isChecked
            if (isSingleChoose) {
                maxCount = 1
                maxImageCount = 1
                maxVideoCount = 1
                ev_max_1.visibility = View.GONE
                ev_max_2.visibility = View.GONE

                // 单选才支持裁剪
                ll_crop.visibility = View.VISIBLE
            } else {
                if (mediaTypeExclusive) {
                    tv_max_1.text = "最大可选择数"
                    ev_max_2.visibility = View.GONE
                    ev_max_1.visibility = View.VISIBLE
                } else {
                    tv_max_1.text = "图片最大选择数"
                    ev_max_1.visibility = View.VISIBLE
                    ev_max_2.visibility = View.VISIBLE
                }

                ll_crop.visibility = View.GONE
            }
        }

        switch_check_type.setOnCheckedChangeListener { _, isChecked -> isCountable = !isChecked }

        switch_capture.setOnCheckedChangeListener { _, isChecked -> isOpenCamera = isChecked }

        switch_compress.setOnCheckedChangeListener { _, isChecked -> isInnerCompress = isChecked }

        switch_crop.setOnCheckedChangeListener { _, isChecked ->
            isCrop = isChecked

            if (isChecked) {
                ll_crop_size.visibility = View.VISIBLE
                ll_crop_type.visibility = View.VISIBLE
            } else {
                ll_crop_size.visibility = View.GONE
                ll_crop_type.visibility = View.GONE
            }
        }

        switch_rectangle_save.setOnCheckedChangeListener { _, isChecked ->
            isSaveRectangle = isChecked
        }

        switch_crop_type.setOnCheckedChangeListener { _, isChecked ->
            cropType = if (isChecked) CropImageView.Style.CIRCLE else CropImageView.Style.RECTANGLE
        }

        chb_jpeg.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_png.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_gif.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_bmp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_webp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mpeg.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mp4.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_quick_time.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_threegpp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_threegpp2.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mkv.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_webm.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_ts.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_avi.setOnCheckedChangeListener(checkedOnCheckedListener)

        btn_open_matisse.setOnClickListener(this)
        btn_open_capture.setOnClickListener(this)
    }

    @SuppressLint("CheckResult")
    override fun onClick(v: View?) {

        RxPermissions(this@ExampleActivity)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe {
                if (!it) {
                    showToast(
                        this, IncapableCause.TOAST, "",
                        getString(R.string.permission_request_denied)
                    )
                    return@subscribe
                }

                when (v) {
                    btn_open_matisse -> {
                        createMatisse()
                        openMatisse()
                    }
                    btn_open_capture -> {
                        createMediaStoreCompat()
                        mediaStoreCompat?.dispatchCaptureIntent(
                            this, ConstValue.REQUEST_CODE_CAPTURE
                        )
                    }
                }
            }
    }

    private fun showToast(context: Context, noticeType: Int, title: String, message: String) {
        if (noticeType == IncapableCause.TOAST) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else if (noticeType == IncapableCause.DIALOG) {
            // 外部弹窗，可外部定义样式
            val incapableDialog = IncapableDialog.newInstance(title, message)
            incapableDialog.show(
                (context as BaseActivity).supportFragmentManager, IncapableDialog::class.java.name
            )
        }
    }

    private var checkedOnCheckedListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val mimeType = when (buttonView) {
                chb_jpeg -> MimeType.JPEG
                chb_png -> MimeType.PNG
                chb_gif -> MimeType.GIF
                chb_bmp -> MimeType.BMP
                chb_webp -> MimeType.WEBP
                chb_mpeg -> MimeType.MPEG
                chb_mp4 -> MimeType.MP4
                chb_quick_time -> MimeType.QUICKTIME
                chb_threegpp -> MimeType.THREEGPP
                chb_threegpp2 -> MimeType.THREEGPP2
                chb_mkv -> MimeType.MKV
                chb_webm -> MimeType.WEBM
                chb_ts -> MimeType.TS
                chb_avi -> MimeType.AVI
                else -> null
            } ?: return@OnCheckedChangeListener

            if (showCustomizeType == null)
                showCustomizeType = mutableListOf()

            if (isChecked) {
                showCustomizeType?.add(mimeType)
            } else {
                showCustomizeType?.remove(mimeType)
            }
        }

    private var mediaStoreCompat: MediaStoreCompat? = null
    private var selectionCreator: SelectionCreator? = null
    private var selectedPathIds: List<String>? = null

    private fun createMatisse() {

        setEditText()
        selectionCreator =
            Matisse.from(this@ExampleActivity)                              // 绑定Activity/Fragment
                .choose(showType, mediaTypeExclusive)                               // 设置显示类型，单一/混合选择模式
                .theme(defaultTheme)                                                // 外部设置主题样式
                .countable(isCountable)                                             // 设置选中计数方式
                .isCrop(isCrop)                                                     // 设置开启裁剪
                .cropStyle(cropType)                                                // 裁剪类型，圆形/方形
                .cropFocusWidthPx(cropWidth)                                        // 裁剪框宽度
                .cropFocusHeightPx(cropHeight)                                      // 裁剪框高度
                .isCropSaveRectangle(isSaveRectangle)                               // 圆形裁剪下是否方形保存
                .maxSelectable(maxCount)                                            // 单一选择下 最大选择数量
                .maxSelectablePerMediaType(maxImageCount, maxVideoCount)            // 混合选择下 视频/图片最大选择数量
                .capture(isOpenCamera)                                              // 是否开启内部拍摄
                .captureStrategy(                                                   // 拍照设置Strategy
                    CaptureStrategy(
                        true,
                        "${Platform.getPackageName(this@ExampleActivity)}.fileprovider"
                    )
                )
                .thumbnailScale(0.6f)                                         // 图片显示压缩比
                .spanCount(spanCount)                                               // 资源显示列数
                .addFilter(SizeFilter(1 * Filter.K * Filter.K))         // 过滤器设置
                .gridExpectedSize(gridSizePx)                                       // 资源显示网格列宽度
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)      // 强制屏幕方向
                .imageEngine(Glide4Engine())                                        // 图片加载实现方式
                .setLastChoosePicturesIdOrUri(selectedPathIds as ArrayList<String>?)// 预选中
                .setNoticeConsumer(object : NoticeConsumer {                        // 外部实现弹窗、吐司
                    override fun accept(
                        context: Context, noticeType: Int, title: String, message: String
                    ) {
                        // 外部提示，可外部定义样式
                        showToast(context, noticeType, title, message)
                    }
                })
                .setStatusBarFuture(object : MFunction<BaseActivity> {              // 外部处理状态栏
                    override fun accept(params: BaseActivity, view: View?) {
                        // 外部设置状态栏
                        ImmersionBar.with(params)?.run {
                            statusBarDarkFont(true)
                            view?.apply { titleBar(this) }
                            init()
                        }

                        // 外部可隐藏Matisse界面中的标题栏
                        // view?.visibility = if (isDarkStatus) View.VISIBLE else View.GONE
                    }
                })
                .setIsInnerCompress(isInnerCompress)                                // 是否是用内部提供压缩
    }

    private fun createMediaStoreCompat() {
        if (mediaStoreCompat != null) return

        val captureStrategy =
            CaptureStrategy(
                true,
                "${Platform.getPackageName(this@ExampleActivity)}.fileprovider"
            )
        mediaStoreCompat = MediaStoreCompat(this, null)
        mediaStoreCompat?.setCaptureStrategy(captureStrategy)
    }

    private fun openMatisse() {
        selectionCreator?.forResult(ConstValue.REQUEST_CODE_CHOOSE)
    }

    private fun setEditText() {
        if (!mediaTypeExclusive) {
            maxImageCount = formatStrTo0(ev_max_1.text.toString())
            maxVideoCount = formatStrTo0(ev_max_2.text.toString())
        } else {
            if (!isSingleChoose) {
                maxCount = formatStrTo0(ev_max_1.text.toString())
            }
        }

        if (isColumnNum) {
            spanCount = formatStrTo0(ev_column.text.toString())
            gridSizePx = 0
        } else {
            gridSizePx = formatStrTo0(ev_column.text.toString())
            spanCount = 0
        }

        cropWidth = formatStrTo0(ev_crop_width.text.toString())
        cropHeight = formatStrTo0(ev_crop_height.text.toString())
    }

    private fun formatStrTo0(s: String?): Int {
        if (s == null || s.toString() == "") {
            Toast.makeText(this, "请保证所有输入非0非空，否则崩溃", Toast.LENGTH_SHORT).show()
            return 0
        }
        return s.toString().toInt()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            ConstValue.REQUEST_CODE_CHOOSE -> doActivityResultForChoose(data)
            ConstValue.REQUEST_CODE_CAPTURE -> doActivityResultForCapture()
            ConstValue.REQUEST_CODE_CROP -> doActivityResultForCrop(data)
        }
    }

    private fun doActivityResultForChoose(data: Intent?) {
        if (data == null) return
        // 获取uri返回值  裁剪结果不返回uri
        val uriList = Matisse.obtainResult(data)
        // 获取文件路径返回值
        val strList = Matisse.obtainPathResult(data)
        val compressedList = Matisse.obtainCompressResult(data)
        selectedPathIds = Matisse.obtainPathIdResult(data)

        strList?.apply {
            Glide.with(this@ExampleActivity).load(this[0]).into(iv_image)
        }

        showPictureResult(uriList, strList, compressedList)
    }

    private fun doActivityResultForCapture() {
        val photoPath = mediaStoreCompat?.getCurrentPhotoPath() ?: ""
        if (isCrop) {
            gotoImageCrop(this, arrayListOf(photoPath))
        } else {
            showCompressedPath(photoPath)
        }
    }

    private fun doActivityResultForCrop(data: Intent?) {
        if (data == null) return
        val cropPath = data.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE) ?: ""
        showCompressedPath(cropPath)
    }

    private fun showCompressedPath(path: String) {
        val compressedPath = PathUtils.getCompressedPath(this, path)
        showPictureResult(null, arrayListOf(path), arrayListOf(compressedPath))
        Glide.with(this).load(compressedPath).into(iv_image)
    }

    private fun showPictureResult(
        uriList: List<Uri>?, strList: List<String>?, compressedList: List<String>?
    ) {
        var string = "uri 路径集合：\n"

        uriList?.forEach {
            string += it.toString() + "\n"
        }

        string += "\npath 路径集合：\n"

        strList?.forEach {
            string += it + "\n"
        }

        string += "\n压缩后路径集合：\n"

        compressedList?.forEach {
            string += it + "\n"
        }

        text.text = "\n\n$string"
    }
}
