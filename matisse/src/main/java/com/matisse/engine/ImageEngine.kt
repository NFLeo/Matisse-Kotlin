package com.matisse.engine

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView

/**
 * Describe : Image loader interface. There are predefined
 * Created by Leo on 2018/9/6 on 17:01.
 */
interface ImageEngine {

    /**
     * Load thumbnail of a static image resource
     *
     * @param context context
     * @param resize Desired size of the origin image
     * @param placeholder Placeholder drawable when image is not loaded yet
     * @param imageView ImageView widget
     * @param uri Uri of the loaded image
     */
    fun loadThumbnail(
        context: Context, resize: Int, placeholder: Drawable?,
        imageView: ImageView, uri: Uri?
    )

    /**
     * Load thumbnail of a static image resource
     *
     * @param context context
     * @param resize Desired size of the origin image
     * @param placeholder Placeholder drawable when image is not loaded yet
     * @param imageView ImageView widget
     * @param uri Uri of the loaded image
     */
    fun loadGifThumbnail(
        context: Context, resize: Int, placeholder: Drawable?,
        imageView: ImageView, uri: Uri?
    )

    /**
     * Load a gif image resource
     *
     * @param context context
     * @param imageView ImageView widget
     * @param uri Uri of the loaded image
     */
    fun loadImage(context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?)

    /**
     * Load a gif image resource
     *
     * @param context context
     * @param resizeX Desired x-size of the origin image
     * @param resizeY Desired y-size of the origin image
     * @param imageView ImageView widget
     * @param uri Uri of the loaded image
     */
    fun loadGifImage(context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?)

    fun cleanMemory(context: Context)

    fun pause(context: Context)

    fun resume(context: Context)

    // 在application的onCreate中初始化
    fun init(context: Context)
}