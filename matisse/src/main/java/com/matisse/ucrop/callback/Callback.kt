@file:JvmName("Callback")
package com.matisse.ucrop.callback

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.matisse.ucrop.model.ExifInfo

interface BitmapCropCallback {

    fun onBitmapCropped(
        resultUri: Uri,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int
    )

    fun onCropFailure(t: Throwable)

}

interface BitmapLoadCallback {

    fun onBitmapLoaded(bitmap: Bitmap, exifInfo: ExifInfo, imageInputUri: Uri, imageOutputUri: Uri?)

    fun onFailure(bitmapWorkerException: Exception)

}


interface BitmapLoadShowCallback {

    fun onBitmapLoaded(bitmap: Bitmap)

    fun onFailure(bitmapWorkerException: Exception)

}

interface CropBoundsChangeListener {

    fun onCropAspectRatioChanged(cropRatio: Float)

}

interface OverlayViewChangeListener {

    fun onCropRectUpdated(cropRect: RectF)

}