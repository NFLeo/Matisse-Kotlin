package com.matisse.ui.view

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.matisse.R
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.PhotoMetadataUtils
import it.sephiroth.android.library.imagezoom.ImageViewTouch
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase

/**
 * desc：预览界面真正载体</br>
 * time: 2018/9/6-9:40</br>
 * author：Leo </br>
 * since V 1.8.0 </br>
 */
class PreviewItemFragment : Fragment() {

    companion object {
        private const val ARGS_ITEM = "args_item"

        fun newInstance(item: Item): PreviewItemFragment {
            val fragment = PreviewItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_preview_item, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item: Item = arguments!!.getParcelable(ARGS_ITEM) ?: return

        val videoPlayButton: View = view.findViewById(R.id.video_play_button)
        if (item.isVideo()) {
            videoPlayButton.visibility = View.VISIBLE
            videoPlayButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(item.getContentUri(), "video/*")
                if (intent.resolveActivity(activity!!.packageManager) != null) startActivity(intent)
                else Toast.makeText(
                    context, R.string.error_no_video_activity, Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            videoPlayButton.visibility = View.GONE
        }

        val image: ImageViewTouch = view.findViewById(R.id.image_view)
        image.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN
        val size: Point = PhotoMetadataUtils.getBitmapSize(item.getContentUri(), activity)
        if (item.isGif()) {
            SelectionSpec.getInstance().imageEngine?.loadGifImage(
                context!!, size.x, size.y, image, item.getContentUri()
            )
        } else {
            SelectionSpec.getInstance().imageEngine?.loadImage(
                context!!, size.x, size.y, image, item.getContentUri()
            )
        }
    }

    fun resetView() {
        val image: ImageViewTouch? = view?.findViewById(R.id.image_view)
        image?.resetMatrix()
    }
}