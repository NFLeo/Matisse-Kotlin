package com.matisse.widget

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.View
import com.matisse.R

/**
 * Created by liubo on 2018/9/7.
 */
class CheckRadioView : AppCompatImageView {

    private var mDrawable: Drawable? = null

    private var mSelectedColor = -1
    private var mUnSelectedColor = -1

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        initParams()
    }

    private fun initParams() {
        mSelectedColor = ResourcesCompat.getColor(resources, R.color.zhihu_item_checkCircle_backgroundColor,
                context.theme)
        mUnSelectedColor = ResourcesCompat.getColor(resources, R.color.zhihu_check_original_radio_disable, context.theme)

    }

    fun setChecked(enable: Boolean) {
        if (enable) {
            setImageResource(R.drawable.ic_preview_radio_on)
            mDrawable = drawable
            mDrawable!!.setColorFilter(mSelectedColor, PorterDuff.Mode.SRC_IN)
        } else {
            setImageResource(R.drawable.ic_preview_radio_off)
            mDrawable = drawable
            mDrawable!!.setColorFilter(mUnSelectedColor, PorterDuff.Mode.SRC_IN)
        }
    }

    fun setColor(c: Int) {
        if (mDrawable == null) {
            mDrawable = drawable
        }

        mDrawable!!.setColorFilter(c, PorterDuff.Mode.SRC_IN)
    }
}