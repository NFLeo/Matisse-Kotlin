package com.matisse.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v4.os.EnvironmentCompat
import com.matisse.entity.CaptureStrategy
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreCompat {

    private var mContext: WeakReference<Activity>
    private var mFragment: WeakReference<Fragment>?
    private var mCaptureStrategy: CaptureStrategy? = null
    private var mCurrentPhotoUri: Uri? = null
    private var mCurrentPhotoPath: String? = null

    companion object {
        fun hasCameraFeature(context: Context): Boolean {
            val pm = context.applicationContext.packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }

    constructor(activity: Activity) : this(activity, null)

    constructor(activity: Activity, fragment: Fragment?) {
        mContext = WeakReference(activity)
        mFragment = if (fragment == null) {
            null
        } else {
            WeakReference(fragment)
        }
    }

    fun setCaptureStrategy(strategy: CaptureStrategy) {
        mCaptureStrategy = strategy
    }

    fun dispatchCaptureIntent(context: Context, requestCode: Int) {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(context.packageManager) != null) {
            var photoFile: File? = null

            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (photoFile != null) {
                mCurrentPhotoPath = photoFile.absolutePath
                mCurrentPhotoUri = FileProvider.getUriForFile(mContext.get()!!, mCaptureStrategy?.authority!!, photoFile)

                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri)
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val resInfoList = context.packageManager
                            .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        context.grantUriPermission(packageName, mCurrentPhotoUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }

                if (mFragment != null) {
                    mFragment?.get()!!.startActivityForResult(captureIntent, requestCode)
                } else {
                    mContext.get()!!.startActivityForResult(captureIntent, requestCode)
                }
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)
        val storageDir: File
        if (mCaptureStrategy?.isPublic!!) {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

            if (!storageDir.exists()) storageDir.mkdirs()
        } else {
            storageDir = mContext.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        }

        // Avoid joining path components manually
        val tempFile = File(storageDir, imageFileName)

        // Handle the situation that user's external storage is not ready
        if (Environment.MEDIA_MOUNTED != EnvironmentCompat.getStorageState(tempFile)) {
            return null
        }

        return tempFile
    }

    fun getCurrentPhotoUri() = mCurrentPhotoUri

    fun getCurrentPhotoPath() = mCurrentPhotoPath
}