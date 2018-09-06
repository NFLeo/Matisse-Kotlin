package com.matisse.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import com.matisse.R

/**
 * Created by liubo on 2018/9/4.
 */
class CheckView(context: Context?) : View(context) {
    companion object {
        const val UNCHECKED: Int = Integer.MIN_VALUE
        private const val STROKE_WIDTH: Float = 3.0f
        private const val SHADOW_WIDTH = 6.0f
        private const val SIZE = 48
        private const val STROKE_RADIUS = 11.5f
        private const val BG_RADIUS = 11.0f
        private const val CONTENT_SIZE = 16
    }

    private var mCountable: Boolean = false
    private var mChecked: Boolean = false
    private var mCheckedNum = 0
    private var mStrokePaint: Paint? = null
    private var mBackgroundPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mShadowPaint: Paint? = null
    private var mCheckDrawable: Drawable? = null
    private var mDensity: Float? = 0f
    private var mCheckRect: Rect? = null
    private var mEnable = true

    init {
        initParams(context)
    }

    private fun initParams(context: Context?) {

        mDensity = context?.resources?.displayMetrics?.density

        mStrokePaint = Paint()
        mStrokePaint?.isAntiAlias = true
        mStrokePaint?.style = Paint.Style.STROKE
        mStrokePaint?.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        mStrokePaint?.strokeWidth = STROKE_WIDTH * mDensity!!

        val ta: TypedArray = context?.theme?.obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_borderColor))!!
        var defaultColor = ResourcesCompat.getColor(context.resources, R.color.zhihu_item_checkCircle_borderColor, context.theme)
        var color = ta.getColor(0, defaultColor)
        ta.recycle()
        mStrokePaint?.color = color

        mCheckDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_check_white_18dp, context.theme)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        var sizeSpec = MeasureSpec.makeMeasureSpec((mDensity?.times(SIZE))!!.toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(sizeSpec, sizeSpec)
    }

    fun setEnable(enable: Boolean) {
        if (mEnable != enable) {
            mEnable = enable
            invalidate()
        }
    }

    fun setCountable(boolean: Boolean) {
        if (mCountable != boolean) {
            mCountable = boolean
            invalidate()
        }
    }

    fun setChecked(boolean: Boolean) {
        if (mCountable) {
            throw IllegalStateException("CheckView is countable, call setCheckedNum() instead.")
        }

        mChecked = boolean
        invalidate()
    }

    fun setCheckedNum(num: Int) {
        if (!mCountable) {
            throw IllegalStateException("CheckView is not countable, call setChecked() instead.")
        }
        if (num != UNCHECKED && num < 0) {
            throw  IllegalStateException("the num can't be negative")
        }
        mCheckedNum = num
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        initShadowPaint()
        canvas?.drawCircle(mDensity!!.times(SIZE) / 2, mDensity!!.times(SIZE) / 2, mDensity!!.times(STROKE_RADIUS + STROKE_WIDTH / 2 + SHADOW_WIDTH), mShadowPaint)


        canvas?.drawCircle(mDensity!!.times(SIZE) / 2, mDensity!!.times(SIZE) / 2, mDensity!!.times(STROKE_RADIUS), mStrokePaint)

        if (mCountable) {
            if (mCheckedNum != UNCHECKED) {
                initBackgroundPaint()
                canvas?.drawCircle(mDensity!!.times(SIZE) / 2, mDensity!!.times(SIZE) / 2,
                        mDensity!!.times(BG_RADIUS), mBackgroundPaint)
                initTextPaint()
                var text = mCheckedNum.toString()
                var baseX = (canvas!!.width - mTextPaint!!.measureText(text)) / 2
                var baseY = (canvas.height - mTextPaint!!.descent() - mTextPaint!!.ascent()) / 2
                canvas.drawText(text, baseX, baseY, mTextPaint)
            }
        } else {
            if (mChecked) {
                initBackgroundPaint()
                canvas!!.drawCircle(mDensity!! * SIZE / 2, mDensity!! * SIZE / 2,
                        BG_RADIUS * mDensity!!, mBackgroundPaint)
                mCheckDrawable!!.bounds = getCheckRect()
                mCheckDrawable!!.draw(canvas)
            }
        }
        alpha = if (mEnable) 1.0f else 0.5f
    }

    private fun getCheckRect(): Rect? {
        if (mCheckRect == null) {
            var rectPadding = (mDensity!! * SIZE / 2 - CONTENT_SIZE * mDensity!! / 2).toInt()
            mCheckRect = Rect(rectPadding, rectPadding, (SIZE * mDensity!! - rectPadding).toInt(), (SIZE * mDensity!! - rectPadding).toInt())
        }
        return mCheckRect
    }

    private fun initTextPaint() {
        if (mTextPaint == null) {
            mTextPaint = Paint()
            mTextPaint!!.isAntiAlias = true
            var ta: TypedArray = context!!.theme!!.obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_numColor))
            var defaultColor = ResourcesCompat.getColor(context!!.resources, R.color.zhihu_item_checkCircle_numColor, context!!.theme)
            var color = ta.getColor(0, defaultColor)
            mTextPaint!!.color = color

        }
    }

    private fun initBackgroundPaint() {
        if (mBackgroundPaint == null) {
            mBackgroundPaint = Paint()
            mBackgroundPaint!!.isAntiAlias = true
            mBackgroundPaint!!.style = Paint.Style.FILL
            var ta: TypedArray = context!!.theme!!.obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_backgroundColor))
            var defaultColor = ResourcesCompat.getColor(context!!.resources, R.color.zhihu_item_checkCircle_backgroundColor, context!!.theme)
            var color = ta.getColor(0, defaultColor)
            ta.recycle()
            mBackgroundPaint!!.color = color
        }
    }

    private fun initShadowPaint() {
        if (mShadowPaint == null) {
            mShadowPaint = Paint()
            mShadowPaint!!.isAntiAlias = true
            var outerRadius: Float = STROKE_RADIUS + STROKE_WIDTH / 2
            var innerRadius = outerRadius - STROKE_WIDTH
            var gradientRadius = outerRadius + SHADOW_WIDTH
            var stop0 = (innerRadius - STROKE_WIDTH) / gradientRadius
            var stop1 = innerRadius / gradientRadius
            var stop2 = outerRadius / gradientRadius
            var stop3 = 1f
            mShadowPaint!!.shader = (RadialGradient(mDensity!!.times(SIZE) / 2, mDensity!!.times(SIZE) / 2, mDensity!!.times(gradientRadius),
                    intArrayOf(Color.parseColor("#00000000"), Color.parseColor("#0D000000"), Color.parseColor("#0D000000"), Color.parseColor("#00000000")),
                    floatArrayOf(stop0, stop1, stop2, stop3),
                    Shader.TileMode.CLAMP))
        }
    }
}