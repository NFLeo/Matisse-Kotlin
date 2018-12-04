package com.matisse.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.matisse.R

/**
 * Created by liubo on 2018/9/4.
 */
class CheckView : View {

    companion object {
        const val UNCHECKED: Int = Integer.MIN_VALUE
        private const val STROKE_WIDTH: Float = 3.0f
        private const val SHADOW_WIDTH = 6.0f
        private const val SIZE = 40
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

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
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
        val defaultColor = ResourcesCompat.getColor(context.resources, R.color.item_checkCircle_borderColor, context.theme)
        val color = ta.getColor(0, defaultColor)
        ta.recycle()
        mStrokePaint?.color = color

        mCheckDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_check_white_18dp, context.theme)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val sizeSpec = MeasureSpec.makeMeasureSpec((mDensity!! * SIZE).toInt(), MeasureSpec.EXACTLY)
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
        super.onDraw(canvas)

        // draw outer and inner shadow
        initShadowPaint()
        canvas?.drawCircle(mDensity!! * SIZE / 2, mDensity!! * SIZE / 2, mDensity!!.times(STROKE_RADIUS + STROKE_WIDTH / 2 + SHADOW_WIDTH), mShadowPaint)

        // draw white stroke
        canvas?.drawCircle(mDensity!! * SIZE / 2, mDensity!! * SIZE / 2, mDensity!!.times(STROKE_RADIUS), mStrokePaint)

        // draw content
        if (mCountable) {
            if (mCheckedNum != UNCHECKED) {
                initBackgroundPaint()
                canvas?.drawCircle(mDensity!! * SIZE / 2, mDensity!! * SIZE / 2,
                        mDensity!!.times(BG_RADIUS), mBackgroundPaint)
                initTextPaint()
                val text = mCheckedNum.toString()
                val baseX = (canvas!!.width - mTextPaint!!.measureText(text)) / 2
                val baseY = (canvas.height - mTextPaint!!.descent() - mTextPaint!!.ascent()) / 2
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
            val rectPadding = (mDensity!! * SIZE / 2 - CONTENT_SIZE * mDensity!! / 2).toInt()
            mCheckRect = Rect(rectPadding, rectPadding, (SIZE * mDensity!! - rectPadding).toInt(), (SIZE * mDensity!! - rectPadding).toInt())
        }
        return mCheckRect
    }

    private fun initTextPaint() {
        if (mTextPaint == null) {
            mTextPaint = TextPaint()
            mTextPaint!!.isAntiAlias = true
            mTextPaint!!.color = Color.WHITE
            mTextPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            mTextPaint!!.textSize = 12.0f * mDensity!!
        }
    }

    private fun initBackgroundPaint() {
        if (mBackgroundPaint == null) {
            mBackgroundPaint = Paint()
            mBackgroundPaint!!.isAntiAlias = true
            mBackgroundPaint!!.style = Paint.Style.FILL
            val ta: TypedArray = context!!.theme!!.obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_backgroundColor))
            val defaultColor = ResourcesCompat.getColor(context!!.resources, R.color.item_checkCircle_backgroundColor, context!!.theme)
            val color = ta.getColor(0, defaultColor)
            ta.recycle()
            mBackgroundPaint!!.color = color
        }
    }

    private fun initShadowPaint() {
        if (mShadowPaint == null) {
            mShadowPaint = Paint()
            mShadowPaint!!.isAntiAlias = true
            val outerRadius: Float = STROKE_RADIUS + STROKE_WIDTH / 2
            val innerRadius = outerRadius - STROKE_WIDTH
            val gradientRadius = outerRadius + SHADOW_WIDTH
            val stop0 = (innerRadius - STROKE_WIDTH) / gradientRadius
            val stop1 = innerRadius / gradientRadius
            val stop2 = outerRadius / gradientRadius
            val stop3 = 1f

            val shadow = ContextCompat.getColor(context, R.color.shadow)
            val shadowHint = ContextCompat.getColor(context, R.color.shadow_hint)
            mShadowPaint!!.shader = (RadialGradient(mDensity!! * SIZE / 2, mDensity!! * SIZE / 2, mDensity!!.times(gradientRadius),
                    intArrayOf(shadowHint, shadow, shadow, shadowHint),
                    floatArrayOf(stop0, stop1, stop2, stop3),
                    Shader.TileMode.CLAMP))
        }
    }
}