package com.matisse.ui.view

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
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
 * Created by liubo on 2018/9/6.
 */
class PreviewItemFragment : Fragment() {
    companion object {
        private val ARGS_ITEM = "args_item"

        fun newInstance(item: Item): PreviewItemFragment {
            val fragment = PreviewItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_preview_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item: Item = arguments!!.getParcelable(ARGS_ITEM) ?: return

        var videoPlayButton: View = view.findViewById(R.id.video_play_button)
        if (item.isVideo()) {
            videoPlayButton.visibility = View.VISIBLE
            videoPlayButton.setOnClickListener {
                var intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(item.getContentUri(), "video/*")
                if (intent.resolveActivity(activity!!.packageManager) != null) startActivity(intent) else Toast.makeText(context, R.string.error_no_video_activity, Toast.LENGTH_SHORT).show()
            }
        } else {
            videoPlayButton.visibility = View.GONE
        }

        var image: ImageViewTouch = view.findViewById(R.id.image_view)
        image.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN
        var size: Point = PhotoMetadataUtils.getBitmapSize(item.getContentUri(), activity)
        if (item.isGif()) {
            SelectionSpec.getInstance().imageEngine.loadGifImage(context!!, size.x, size.y, image, item.getContentUri()!!)
        } else {
            SelectionSpec.getInstance().imageEngine.loadImage(context!!, size.x, size.y,image, item.getContentUri()!! )
        }
    }


    fun resetView() {
        if (view != null) {
            var image:ImageViewTouch = view!!.findViewById(R.id.image_view);
            image.resetMatrix()
        }
    }
}