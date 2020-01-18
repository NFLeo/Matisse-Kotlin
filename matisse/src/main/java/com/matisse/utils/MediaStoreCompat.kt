package com.matisse.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.matisse.entity.CaptureStrategy
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

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
            currentPhotoUri =
                if (Platform.beforeAndroidTen()) createImageFile() else createImageFileForQ()
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

    //todo Leo
//    private fun createImageFile(): File? {
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)
//        val storageDir: File
//        if (captureStrategy?.isPublic == true) {
//            storageDir = if (Platform.beforeAndroidTen())
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//            else kContext.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
//
//            if (!storageDir.exists()) storageDir.mkdirs()
//        } else {
//            storageDir = kContext.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
//        }
//
//        // TODO 2019/10/28 Leo 暂时不做
////        if (captureStrategy?.isPublic == true && captureStrategy?.directory != null) {
////            storageDir = File(storageDir, captureStrategy?.directory)
////            if (!storageDir.exists()) storageDir.mkdirs()
////        }
//
//        // Avoid joining path components manually
//        val tempFile = File(storageDir, imageFileName)
//
//        // Handle the situation that user's external storage is not ready
//        if (Environment.MEDIA_MOUNTED != EnvironmentCompat.getStorageState(tempFile)) return null
//
//        return tempFile
//    }

    private fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) storageDir.mkdirs()
        val tempFile = File(storageDir, imageFileName)
        currentPhotoPath = tempFile.absolutePath
        return FileProvider.getUriForFile(
            kContext.get()!!, captureStrategy?.authority!!, tempFile
        )
    }

    @SuppressLint("InlinedApi")
    private fun createImageFileForQ(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)

        val resolver = kContext.get()?.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        currentPhotoPath = PathUtils.getPath(kContext.get(), uri)
        return uri
    }

    fun getCurrentPhotoUri() = currentPhotoUri

    fun getCurrentPhotoPath() = currentPhotoPath
}