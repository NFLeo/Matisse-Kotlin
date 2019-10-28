package com.leo.matisse

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.view.ViewCompat
import android.view.ViewGroup
import android.widget.ImageView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.DraweeHolder
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.matisse.engine.ImageEngine

/**
 * Describe :
 * Created by Leo on 2018/10/9 on 15:07.
 */
class FrescoEngine : ImageEngine {

    override fun loadThumbnail(
        context: Context, resize: Int, placeholder: Drawable?, imageView: ImageView, uri: Uri?
    ) {
        var hierarchy: GenericDraweeHierarchy? = null
        val hierarchyBuilder =
            GenericDraweeHierarchyBuilder.newInstance(imageView.context.resources)
        var draweeHolder: DraweeHolder<*>? =
            imageView.getTag(R.id.fresco_drawee) as DraweeHolder<*>?

        hierarchyBuilder.placeholderImage = placeholder
        hierarchyBuilder.failureImage = placeholder

        if (hierarchy == null) {
            hierarchy = hierarchyBuilder.build()
        }

        val controllerBuilder =
            Fresco.newDraweeControllerBuilder().setUri(uri).setAutoPlayAnimations(true)

        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
        imageRequestBuilder.resizeOptions = ResizeOptions(resize, resize)

        val request = imageRequestBuilder.build()
        controllerBuilder.imageRequest = request
        val controller: DraweeController

        if (draweeHolder == null) {
            draweeHolder = DraweeHolder.create(hierarchy, context)
            controller = controllerBuilder.build()
        } else {
            controller = controllerBuilder.setOldController(draweeHolder.controller).build()
        }

        draweeHolder?.controller = controller

        if (ViewCompat.isAttachedToWindow(imageView)) {
            draweeHolder?.onAttach()
        }

        imageView.setTag(R.id.fresco_drawee, draweeHolder)
        imageView.setImageDrawable(draweeHolder?.topLevelDrawable)
    }

    override fun loadGifThumbnail(
        context: Context, resize: Int, placeholder: Drawable?, imageView: ImageView, uri: Uri?
    ) {
    }

    override fun loadImage(
        context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?
    ) {
        var hierarchy: GenericDraweeHierarchy? = null
        val hierarchyBuilder =
            GenericDraweeHierarchyBuilder.newInstance(imageView.context.resources)
        var draweeHolder: DraweeHolder<*>? =
            imageView.getTag(R.id.fresco_drawee) as DraweeHolder<*>?

        if (hierarchy == null) {
            hierarchy = hierarchyBuilder.build()
        }

        val controllerBuilder =
            Fresco.newDraweeControllerBuilder().setUri(uri).setAutoPlayAnimations(true)

        var params: ViewGroup.LayoutParams? = imageView.layoutParams
        if (params == null) {
            params = ViewGroup.LayoutParams(resizeX, resizeY)
        }

        if (params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        imageView.layoutParams = params

        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
        val request = imageRequestBuilder.build()
        controllerBuilder.imageRequest = request
        val controller: DraweeController

        if (draweeHolder == null) {
            draweeHolder = DraweeHolder.create(hierarchy, context)
            controller = controllerBuilder.build()
        } else {
            controller = controllerBuilder.setOldController(draweeHolder.controller).build()
        }

        // 请求
        draweeHolder?.controller = controller

        if (ViewCompat.isAttachedToWindow(imageView)) {
            draweeHolder?.onAttach()
        }

        imageView.setTag(R.id.fresco_drawee, draweeHolder)
        imageView.setImageDrawable(draweeHolder?.topLevelDrawable)
    }

    override fun loadGifImage(
        context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri?
    ) {
    }

    override fun cleanMemory(context: Context) {
        Fresco.getImagePipeline().clearMemoryCaches()
    }

    override fun pause(context: Context) {
        Fresco.getImagePipeline().pause()
    }

    override fun resume(context: Context) {
        Fresco.getImagePipeline().resume()
    }

    override fun init(context: Context) {
        Fresco.initialize(context)
    }
}