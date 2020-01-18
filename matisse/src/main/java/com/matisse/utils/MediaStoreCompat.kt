package com.matisse.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import com.matisse.entity.CaptureStrategy
import java.lang.ref.WeakReference

class MediaStoreCompat {

    private var kContext: WeakReference<Activity>
    private var kFragment: WeakReference<Fragment>?
    private var captureStrategy: CaptureStrategy? = null
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    companion object {
        fun hasCameraFeature(context: Context): Boolean {
            val pm = context.applicationContext.packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }

    constructor(activity: Activity) : this(activity, null)
    constructor(activity: Activity, fragment: Fragment?) {
        kContext = WeakReference(activity)
        kFragment = if (fragment == null) null else WeakReference(fragment)
    }

    fun setCaptureStrategy(strategy: CaptureStrategy) {
        captureStrategy = strategy
    }

    fun dispatchCaptureIntent(context: Context, requestCode: Int) {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (captureIntent.resolveActivity(context.packageManager) != null) {
            // 创建uri
            createCurrentPhotoUri()

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val resInfoList = context.packageManager
                    .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        packageName, currentPhotoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            if (kFragment != null) {
                kFragment?.get()?.startActivityForResult(captureIntent, requestCode)
            } else {
                kContext.get()?.startActivityForResult(captureIntent, requestCode)
            }

        }
    }

    private fun createCurrentPhotoUri() {
        currentPhotoUri = if (Platform.beforeAndroidTen())
            createImageFile(
                kContext.get()!!, captureStrategy?.authority ?: ""
            ) { currentPhotoPath = it }
        else
            createImageFileForQ(kContext.get()!!) {
                currentPhotoPath = getPath(kContext.get(), it)
            }
    }

    fun getCurrentPhotoUri() = currentPhotoUri

    fun getCurrentPhotoPath() = currentPhotoPath
}