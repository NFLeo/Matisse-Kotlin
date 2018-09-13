package com.leo.matisse

import android.Manifest
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.leo.matisse.R.id.btn_media_store
import com.matisse.Matisse
import com.matisse.MimeTypeManager.Companion.ofAll
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.ConstValue
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.matisse.utils.Platform
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.sample.Glide4Engine
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_media_store.setOnClickListener {
            RxPermissions(this@MainActivity)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                    .subscribe(object : Observer<Boolean> {
                        override fun onComplete() {
                        }

                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onNext(boolean: Boolean) {
                            if (!boolean) {
                                Toast.makeText(this@MainActivity, R.string.permission_request_denied, Toast.LENGTH_LONG).show()
                                return
                            }
                            Matisse.from(this@MainActivity)
                                    .choose(ofAll(), false)
                                    .countable(true)
                                    .capture(true)
                                    .theme(R.style.Matisse_Zhihu)
                                    .captureStrategy(CaptureStrategy(true, "${Platform.getPackageName(this@MainActivity)}.fileprovider"))
                                    .maxSelectable(9)
                                    .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                    //.imageEngine(new GlideEngine())  // for glide-V3
                                    .imageEngine(Glide4Engine())    // for glide-V4
                                    .setOnSelectedListener(object : OnSelectedListener {
                                        override fun onSelected(uriList: List<Uri>, pathList: List<String>) {
                                            // DO SOMETHING IMMEDIATELY HERE
                                            Log.e("onSelected", "onSelected: pathList=$pathList")
                                        }
                                    })
                                    .originalEnable(true)
                                    .maxOriginalSize(10)
                                    .setOnCheckedListener(object : OnCheckedListener {
                                        override fun onCheck(isChecked: Boolean) {
                                            // DO SOMETHING IMMEDIATELY HERE
                                            Log.e("isChecked", "onCheck: isChecked=$isChecked")
                                        }
                                    })
                                    .forResult(ConstValue.REQUEST_CODE_CHOOSE)
                        }

                        override fun onError(e: Throwable) {
                        }
                    })
        }
    }
}
