package com.matisse.ui.view

import android.content.Intent
import android.graphics.Point
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.matisse.R
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.photoview.PhotoView
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.widget.longimage.ImageSource
import com.matisse.widget.longimage.ImageViewState
import com.matisse.widget.longimage.SubsamplingScaleImageView
import it.sephiroth.android.library.imagezoom.ImageViewTouch

/**
 * desc: 预览界面真正载体 </br>
 * time: 2020-03-30-20:05 </br>
 * author: Leo </br>
 * since V 2.1 </br>
 */
class PicturePreviewItemFragment : Fragment() {

    companion object {
        private const val ARGS_ITEM = "args_item"

        fun newInstance(item: Item): PicturePreviewItemFragment {
            val fragment = PicturePreviewItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_picture_preview_item, container, false)

    override fun onViewCreated(contentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(contentView, savedInstanceState)
        val media: Item = arguments!!.getParcelable(ARGS_ITEM) ?: return

        val videoPlayButton: View = contentView.findViewById(R.id.video_play_button)
        if (media.isVideo()) {
            videoPlayButton.visibility = View.VISIBLE
            videoPlayButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(media.getContentUri(), "video/*")
                if (intent.resolveActivity(activity!!.packageManager) != null) startActivity(intent)
                else Toast.makeText(
                    context, R.string.error_no_video_activity, Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            videoPlayButton.visibility = View.GONE
        }

        // 常规图控件
        val imageView: PhotoView = contentView.findViewById(R.id.preview_image)
        // 长图控件
        val longImg: SubsamplingScaleImageView = contentView.findViewById(R.id.longImg)

        val size: Point = PhotoMetadataUtils.getBitmapSize(media.getContentUri(), activity)
        // TODO Leo 2020-03-30 长图判断不能这么写，后续完善
        val isLongImg = PhotoMetadataUtils.isLongImg(size)
        val isGifImg = media.isGif()

        imageView.visibility = if (isLongImg && !isGifImg) View.GONE else View.VISIBLE
        longImg.visibility = if (isLongImg && !isGifImg) View.VISIBLE else View.GONE

        if (isGifImg) {
            SelectionSpec.getInstance().imageEngine?.loadGifImage(
                context!!, size.x, size.y, imageView, media.getContentUri()
            )
        } else {
            if (isLongImg) {
                displayLongPic(media.getContentUri(), longImg)
            } else {
                SelectionSpec.getInstance().imageEngine?.loadImage(
                    context!!, size.x, size.y, imageView, media.getContentUri()
                )
            }
        }
    }

    fun resetView() {
        val image: ImageViewTouch? = view?.findViewById(R.id.image_view)
        image?.resetMatrix()
    }

    /**
     * 加载长图
     *
     * @param uri
     * @param longImg
     */
    private fun displayLongPic(uri: Uri, longImg: SubsamplingScaleImageView) {
        longImg.isQuickScaleEnabled = true
        longImg.isZoomEnabled = true
        longImg.isPanEnabled = true
        longImg.setDoubleTapZoomDuration(100)
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        longImg.setImage(ImageSource.uri(uri), ImageViewState(0f, PointF(0f, 0f), 0))
    }
}