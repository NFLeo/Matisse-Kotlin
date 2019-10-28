package com.leo.matisse

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import com.matisse.engine.ImageEngine

/**
 * [ImageEngine] implementation using Glide.
 */
class Glide4Engine : ImageEngine {

    override fun cleanMemory(context: Context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Glide.get(context).clearMemory()
        }
    }

    override fun pause(context: Context) {
        Glide.with(context).pauseRequests()
    }

    override fun resume(context: Context) {
        Glide.with(context).resumeRequests()
    }

    override fun init(context: Context) {
    }

    override fun loadThumbnail(
        context: Context, resize: Int, placeholder: Drawable?, imageView: ImageView, uri: Uri?
    ) {
        Glide.with(context)
            .asBitmap() // some .jpeg files are actually gif
            .load(uri)
            .apply(
                RequestOptions()
                    .override(resize, resize).dontAnimate()
                    .placeholder(placeholder).centerCrop()
            )
            .into(imageView)
    }

    override fun loadGifThumbnail(
        context: Context, resize: Int, placeholder: Drawable?, imageView: ImageView, uri: Uri?
    ) {
        Glide.with(context)
            .asBitmap() // some .jpeg files are actually gif
            .load(uri)
            .apply(
                RequestOptions().override(resize, resize).placeholder(placeholder).centerCrop()
            )
            .into(imageView)
    }

    override fun loadImage(
        context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?
    ) {
        Glide.with(context)
            .load(uri)
            .apply(
                RequestOptions()
                    .override(resizeX, resizeY).priority(Priority.HIGH).fitCenter()
            )
            .into(imageView)
    }

    override fun loadGifImage(
        context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?
    ) {
        Glide.with(context)
            .asGif()
            .load(uri)
            .apply(
                RequestOptions().override(resizeX, resizeY).priority(Priority.HIGH).fitCenter()
            )
            .into(imageView)
    }
}
