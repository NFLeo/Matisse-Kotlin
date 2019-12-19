package com.matisse.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.matisse.R

class CheckRadioView : AppCompatImageView {

    private var mDrawable: Drawable? = null
    private lateinit var selectedColorFilter: PorterDuffColorFilter
    private lateinit var unSelectUdColorFilter: PorterDuffColorFilter


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val ta: TypedArray = context?.theme
            ?.obtainStyledAttributes(intArrayOf(R.attr.Item_checkRadio)) ?: return
        val selectedColor = ta.getColor(
            0, ResourcesCompat.getColor(
                resources, R.color.selector_base_text, context.theme
            )
        )
        val unSelectUdColor = ResourcesCompat.getColor(
            resources, R.color.check_original_radio_disable, context.theme
        )
        ta.recycle()

        selectedColorFilter = PorterDuffColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        unSelectUdColorFilter = PorterDuffColorFilter(unSelectUdColor, PorterDuff.Mode.SRC_IN)
        setChecked(false)
    }

    fun setChecked(enable: Boolean) {
        if (enable) {
            setImageResource(R.drawable.ic_preview_radio_on)
            mDrawable = drawable
            mDrawable?.colorFilter = selectedColorFilter
        } else {
            setImageResource(R.drawable.ic_preview_radio_off)
            mDrawable = drawable
            mDrawable?.colorFilter = unSelectUdColorFilter
        }
    }

    fun setColor(color: Int) {
        if (mDrawable == null) {
            mDrawable = drawable
        }
        mDrawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}
