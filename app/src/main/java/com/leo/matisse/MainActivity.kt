package com.leo.matisse

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.matisse.Matisse
import com.matisse.MimeTypeManager
import com.matisse.compress.CompressHelper
import com.matisse.compress.FileUtil
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.ConstValue
import com.matisse.listener.NoticeConsumer
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.Platform
import com.matisse.utils.UIUtils
import com.matisse.widget.CropImageView
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AppCompatButton>(R.id.btn_media_store).setOnClickListener {
            RxPermissions(this@MainActivity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .subscribe {
                    if (!it) {
                        Toast.makeText(
                            this@MainActivity, R.string.permission_request_denied, Toast.LENGTH_LONG
                        ).show()
                        return@subscribe
                    }
                    Matisse.from(this@MainActivity)
                        .choose(MimeTypeManager.ofAll())
                        .countable(false)
                        .capture(true)
                        .isCrop(true)
                        .cropStyle(CropImageView.Style.CIRCLE)
                        .isCropSaveRectangle(true)
                        .maxSelectable(1)
                        .theme(R.style.CustomMatisseStyle)
                        .captureStrategy(
                            CaptureStrategy(
                                true,
                                "${Platform.getPackageName(this@MainActivity)}.fileprovider",
                                "Leo"
                            )
                        )
                        .thumbnailScale(0.8f)
                        .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .imageEngine(Glide4Engine())
                        .theme(R.style.CustomMatisseStyle)
                        .setNoticeConsumer(object : NoticeConsumer {
                            override fun accept(
                                context: Context, noticeType: Int, title: String, message: String
                            ) {
                                showToast(message)
                            }
                        })
                        .forResult(ConstValue.REQUEST_CODE_CHOOSE)
                }
        }

        findViewById<AppCompatButton>(R.id.btn_media_multi).setOnClickListener {
            RxPermissions(this@MainActivity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .subscribe {
                    if (!it) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.permission_request_denied,
                            Toast.LENGTH_LONG
                        ).show()
                        return@subscribe
                    }
                    Matisse.from(this@MainActivity)
                        .choose(MimeTypeManager.ofAll(), true)
                        .countable(false)
                        .capture(true)
                        .isCrop(true)
                        .cropStyle(CropImageView.Style.RECTANGLE)
                        .cropFocusWidthPx(UIUtils.dp2px(this, 250f).toInt())
                        .cropFocusHeightPx(UIUtils.dp2px(this, 500f).toInt())
                        .theme(R.style.CustomMatisseStyle)
                        .captureStrategy(
                            CaptureStrategy(
                                true,
                                "${Platform.getPackageName(this@MainActivity)}.fileprovider"
                            )
                        )
                        .maxSelectable(1)
                        .thumbnailScale(0.8f)
                        .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        .imageEngine(Glide4Engine())
                        .setOnSelectedListener(object : OnSelectedListener {
                            override fun onSelected(uriList: List<Uri>, pathList: List<String>) {
                                // DO SOMETHING IMMEDIATELY HERE
                                Log.e("onSelected", "onSelected: pathList=$pathList")
                            }
                        })
                        .setOnCheckedListener(object : OnCheckedListener {
                            override fun onCheck(isChecked: Boolean) {
                                // DO SOMETHING IMMEDIATELY HERE
                                Log.e("isChecked", "onCheck: isChecked=$isChecked")
                            }
                        })
                        .forResult(ConstValue.REQUEST_CODE_CHOOSE)
                }
        }
    }

    private fun showToast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return

        if (requestCode == ConstValue.REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            var string = ""
            val uriList = Matisse.obtainResult(data)
            val strList = Matisse.obtainPathResult(data)

            uriList.forEach {
                string += it.toString() + "\n"
            }

            string += "\n"

            strList?.forEach {
                string += it + "\n"
            }

            val path = Matisse.obtainPathResult(data) ?: return

            // 原文件
            val file = FileUtil.getFileByPath(path[0])

            Glide.with(this).load(file).into(iv_image)
            // 压缩后的文件         （多个文件压缩可以循环压缩）
            val file1 = CompressHelper.getDefault(applicationContext)?.compressToFile(file)
            string += PhotoMetadataUtils.getSizeInMB(file.length()).toString() + " PK " +
                    PhotoMetadataUtils.getSizeInMB(file1?.length() ?: 0)
            string = "\n\n$string"

            text.text = "\n\n$string"
        }
    }
}
