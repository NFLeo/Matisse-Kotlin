package com.leo.matisse

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.matisse.Matisse
import com.matisse.MimeTypeManager.Companion.ofAll
import com.matisse.entity.CaptureStrategy
import com.matisse.filter.Filter
import com.matisse.listener.OnCheckedListener
import com.matisse.listener.OnSelectedListener
import com.zhihu.matisse.sample.GifSizeFilter
import com.zhihu.matisse.sample.Glide4Engine
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_CHOOSE = 23

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_media_store.setOnClickListener {
            Matisse.from(this@MainActivity)
                    .choose(ofAll(), false)
                    .countable(true)
                    .capture(true)
                    .theme(R.style.AppTheme)
                    .captureStrategy(
                            CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider"))
                    .maxSelectable(9)
                    .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    .gridExpectedSize(
                            resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    .thumbnailScale(0.85f)
                    //                                            .imageEngine(new GlideEngine())  // for glide-V3
                    .imageEngine(Glide4Engine())    // for glide-V4
                    .setOnSelectedListener(object : OnSelectedListener {
                        override fun onSelected(
                                uriList: List<Uri>, pathList: List<String>) {
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
                    .forResult(REQUEST_CODE_CHOOSE)
        }
    }
}
