package com.matisse.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import com.matisse.R
import com.matisse.utils.Platform
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class CropImageView: AppCompatImageView {

    //---------------- focus frame attributes start ------------//
    enum class Style {
        RECTANGLE, CIRCLE
    }

    private val styles = arrayOf(Style.RECTANGLE, Style.CIRCLE)

    private var mMaskColor          = 0XAF000000   // dark
    private var mBorderColor        = 0XAA808080   // focusFrame border color
    private var mBorderWidth        = 1            // focusFrame border width
    private var mFocusWidth         = 250          // focusFrame width
    private var mFocusHeight        = 250          // focusFrame height
    private var mDefaultStyleIndex  = 0            // default style
    private var mStyle              = styles[mDefaultStyleIndex]
    private val mBorderPaint        = Paint()
    private val mFocusPath          = Path()
    private val mFocusRect          = RectF()
    //---------------- focus frame attributes end ------------//

    companion object {
        val MAX_SCALE           = 4F
        val NONE                = 0
        val DRAG                = 1
        val ZOOM                = 2
        val ROTATE              = 3
        val ZOOM_OR_ROTATE      = 4
        val SAVE_SUCCESS        = 1001
        val SAVE_ERROR          = 1002

        var mHandler            = InnerHandler()
        var mListener:OnBitmapSaveCompleteListener? = null
    }

    private var mImageWidth         = 0
    private var mImageHeight        = 0
    private var mRotatedImageWidth  = 0
    private var mRotatedImageHeight = 0
    private var mMatrix             = Matrix()      // matrix when image is changing
    private var savedMatrix         = Matrix()      // matrix when image is stated
    private var pA                  = PointF()      // pointF of first finger
    private var pB                  = PointF()      // pointF of second finger
    private var midPoint            = PointF()      // middle pointF of two fingers
    private var doubleClickPos      = PointF()      // pointF of double click
    private var mFocusMidPoint      = PointF()      // middle pointF of focus frame view

    private var mode                = NONE          // init gesture mode
    private var doubleClickTime     = 0L            // next double click time
    private var mRatation            = 0F            // angle of finger rotation (is not integer multiple of 90)
    private var oldDist             = 1F            // first distance of two fingers
    private var sumRotationLevel    = 0             // angle of rotation (is integer multiple of 90)
    private var mMaxScale           = MAX_SCALE     // get max scale from different images
    private var isInitSize          = false         // is init by onSizeChanged
    private var mSaving             = false         // is saving

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        initAttributeSet(context!!, attrs!!)
    }

    private fun initAttributeSet(context: Context, attrs: AttributeSet) {
        mFocusWidth = getDimension(mFocusWidth.toFloat()).toInt()
        mFocusHeight = getDimension(mFocusHeight.toFloat()).toInt()
        mBorderWidth = getDimension(mBorderWidth.toFloat()).toInt()

        val a = context.obtainStyledAttributes(attrs, R.styleable.CropImageView)
        mMaskColor = a.getColor(R.styleable.CropImageView_cropMaskColor, mMaskColor.toInt()).toLong()
        mBorderColor = a.getColor(R.styleable.CropImageView_cropBorderColor, mBorderColor.toInt()).toLong()
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CropImageView_cropBorderWidth, mBorderWidth)
        mFocusWidth = a.getDimensionPixelSize(R.styleable.CropImageView_cropFocusWidth, mFocusWidth)
        mFocusHeight = a.getDimensionPixelSize(R.styleable.CropImageView_cropFocusHeight, mFocusHeight)
        mDefaultStyleIndex = a.getInteger(R.styleable.CropImageView_cropStyle, mDefaultStyleIndex)
        mStyle = styles[mDefaultStyleIndex]
        a.recycle()

        // set image as matrix scale type
        scaleType = ScaleType.MATRIX
    }

    private fun getDimension(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        initImageAndFocusView()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initImageAndFocusView()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        initImageAndFocusView()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initImageAndFocusView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        isInitSize = true
        initImageAndFocusView()
    }

    private fun initImageAndFocusView() {
        val d = drawable
        if (!isInitSize || d == null) {
            return
        }

        mode = NONE
        mMatrix = imageMatrix
        mImageWidth = d.intrinsicWidth
        mRotatedImageWidth = d.intrinsicWidth
        mImageHeight = d.intrinsicHeight
        mRotatedImageHeight = d.intrinsicHeight

        // get center and boundary of the focus view
        val viewWidth = width
        val viewHeight = height
        val midPointX = viewWidth / 2f
        val midPointY = viewHeight / 2f
        mFocusMidPoint = PointF(midPointX, midPointY)

        if (mStyle == Style.CIRCLE) {
            val focusSize = Math.min(mFocusWidth, mFocusHeight)
            mFocusWidth = focusSize
            mFocusHeight = focusSize
        }

        mFocusRect.left = mFocusMidPoint.x - mFocusWidth / 2
        mFocusRect.right = mFocusMidPoint.x + mFocusWidth / 2
        mFocusRect.top = mFocusMidPoint.y - mFocusHeight / 2
        mFocusRect.bottom = mFocusMidPoint.y + mFocusHeight / 2

        // the min slide of image must larger than the min slide of focus view
        val fitFocusScale = getScale(mImageWidth, mImageHeight, mFocusWidth, mFocusHeight, true)
        mMaxScale = fitFocusScale * MAX_SCALE

        // there is one slide at least of image to fit the parent
        val fitViewScale = getScale(mImageWidth, mImageHeight, viewWidth, viewHeight, false)

        // make sure the final scale rate fit focus view priority
        val scale = Math.max(fitViewScale, fitFocusScale).toFloat()
        // scale with the center of image center Point
        mMatrix.setScale(scale, scale, mImageWidth / 2f, mImageHeight / 2f)
        val mImageMatrixValues = FloatArray(9)
        // get mImageMatrixValues after scaled
        mMatrix.getValues(mImageMatrixValues)
        val transX = mFocusMidPoint.x - (mImageMatrixValues[2] + mImageWidth * mImageMatrixValues[0] / 2)
        val transY = mFocusMidPoint.y - (mImageMatrixValues[5] + mImageHeight * mImageMatrixValues[4] / 2)
        mMatrix.postTranslate(transX, transY)
        imageMatrix = mMatrix
        invalidate()
    }

    // calculate boundary scale rate
    private fun getScale(bitmapWidth: Int, bitmapHeight: Int, minWidth: Int, minHeight: Int, isMinScale: Boolean): Int {
        val scaleX = minWidth / bitmapWidth
        val scaleY = minHeight / bitmapHeight
        return if (isMinScale) Math.max(scaleX, scaleY)
        else Math.min(scaleX, scaleY)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            when (mStyle) {
                Style.CIRCLE -> {
                    val radius = Math.min((mFocusRect.right - mFocusRect.left) / 2,
                            (mFocusRect.bottom - mFocusRect.top) / 2)
                    mFocusPath.addCircle(mFocusMidPoint.x, mFocusMidPoint.y, radius, Path.Direction.CCW)
                }

                Style.RECTANGLE -> {
                    mFocusPath.addRect(mFocusRect, Path.Direction.CCW)
                }
            }

            save()
            clipRect(0, 0, width, height)
            if (Platform.hasKitO26()) {
                clipOutPath(mFocusPath)
            } else {
                clipPath(mFocusPath, Region.Op.DIFFERENCE)
            }
            drawColor(mMaskColor.toInt())
            restore()
        }

        mBorderPaint.apply {
            color = mBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = mBorderWidth.toFloat()
            isAntiAlias = true
            canvas?.drawPath(mFocusPath, mBorderPaint)
            mFocusPath.reset()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mSaving || null == drawable) {
            return super.onTouchEvent(event)
        }

        when (event?.action!! and MotionEvent.ACTION_MASK) {
            // one point press down
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(mMatrix)
                pA.set(event.x, event.y)
                pB.set(event.x, event.y)
                mode = DRAG
            }

            // next point press down
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.actionIndex <= 1) {
                    pA.set(event.getX(0), event.getY(0))
                    pB.set(event.getX(1), event.getY(1))
                    midPoint.set((pA.x + pB.x) / 2, (pA.y + pB.y) / 2)
                    oldDist = spacing(pA, pB)
                    savedMatrix.set(mMatrix)
                    if (oldDist > 10f) mode = ZOOM_OR_ROTATE
                }
            }

            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handleActionUp()
            }
        }

        // to fix some device can't drag
        ViewCompat.postInvalidateOnAnimation(this)
        return true
    }

    private fun handleActionMove(event: MotionEvent) {
        if (mode == ZOOM_OR_ROTATE) {
            val pC = PointF(event.getX(1) - event.getX(0) + pA.x,
                    event.getY(1) - event.getY(0) + pA.y)
            val a = spacing(pB.x.toDouble(), pB.y, pC.x, pC.y).toDouble()
            val b = spacing(pA.x.toDouble(), pA.y, pC.x, pC.y).toDouble()
            val c = spacing(pA.x.toDouble(), pA.y, pB.x, pB.y).toDouble()

            if (a >= 10) {
                val cosB = (a * a + c * c - b * b) / (2.0 * a * c)
                val angleB = Math.acos(cosB)
                val pid4 = Math.PI / 4
                //旋转时，默认角度在 45 - 135 度之间
                mode = if (angleB > pid4 && angleB < 3 * pid4)
                    ROTATE else ZOOM
            }
        }

        when (mode) {

            DRAG -> {
                mMatrix.set(savedMatrix)
                mMatrix.postTranslate(event.x - pA.x, event.y - pA.y)
                fixTranslation()
                imageMatrix = mMatrix
            }

            ZOOM -> {
                val newDist = spacing(event.getX(0).toDouble(), event.getY(0),
                        event.getX(1), event.getY(1))

                if (newDist > 10f) {
                    mMatrix.set(savedMatrix)
                    val fScale = Math.min(newDist / oldDist, maxPostScale())
                    if (fScale != 0f) {
                        mMatrix.postScale(fScale, fScale, midPoint.x, midPoint.y)
                        fixScale()
                        fixTranslation()
                        imageMatrix = mMatrix
                    }
                }
            }

            ROTATE -> {
                val pC = PointF(event.getX(1) - event.getX(0) + pA.x, event.getY(1) - event.getY(0) + pA.y)
                val a = spacing(pB.x.toDouble(), pB.y, pC.x, pC.y).toDouble()
                val b = spacing(pA.x.toDouble(), pA.y, pC.x, pC.y).toDouble()
                val c = spacing(pA.x.toDouble(), pA.y, pB.x, pB.y).toDouble()
                if (b > 10) {
                    val cosA = (b * b + c * c - a * a) / (2.0 * b * c)
                    var angleA = Math.acos(cosA)
                    val ta = (pB.y - pA.y).toDouble()
                    val tb = (pA.x - pB.x).toDouble()
                    val tc = (pB.x * pA.y - pA.x * pB.y).toDouble()
                    val td = ta * pC.x + tb * pC.y + tc
                    if (td > 0) {
                        angleA = 2 * Math.PI - angleA
                    }
                    mRatation = angleA.toFloat()
                    matrix.set(savedMatrix)
                    matrix.postRotate((mRatation * 180 / Math.PI).toFloat(), midPoint.x, midPoint.y)
                    imageMatrix = matrix
                }
            }
        }
    }

    private fun handleActionUp() {
        when (mode) {
            DRAG -> {
                if (spacing(pA, pB) < 50) {
                    var now = System.currentTimeMillis()
                    if (now - doubleClickTime < 500 && spacing(pA, doubleClickPos) < 50) {
                        doubleClick(pA.x, pA.y)
                        now = 0
                    }

                    doubleClickPos.set(pA)
                    doubleClickTime = now
                }
            }

            ROTATE -> {
                var rotateLevel = Math.floor((rotation + Math.PI / 4) / (Math.PI / 2)).toInt()
                if (rotateLevel == 4) rotateLevel = 0
                matrix.set(savedMatrix)
                matrix.postRotate((90 * rotateLevel).toFloat(), midPoint.x, midPoint.y)
                if (rotateLevel == 1 || rotateLevel == 3) {
                    val tmp = mRotatedImageWidth
                    mRotatedImageWidth = mRotatedImageHeight
                    mRotatedImageHeight = tmp
                }
                fixScale()
                fixTranslation()
                imageMatrix = matrix
                sumRotationLevel += rotateLevel
            }
        }

        mode = NONE
    }

    // get distance between two points
    private fun spacing(pA: PointF, pB: PointF) = spacing(pA.x.toDouble(), pA.y, pB.x, pB.y)

    private fun spacing(x1: Double, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return Math.sqrt(x * x + y * y).toFloat()
    }

    // fix translation of image
    private fun fixTranslation() {
        val imageRect = RectF(0f, 0f, mImageWidth.toFloat(), mImageHeight.toFloat())
        // get the area of image scale after
        mMatrix.mapRect(imageRect)

        val deltaX = when {
            imageRect.left > mFocusRect.left -> -imageRect.left + mFocusRect.right
            imageRect.right < mFocusRect.right -> -imageRect.right + mFocusRect.right
            else -> 0f
        }

        val deltaY = when {
            imageRect.top > mFocusRect.top -> -imageRect.top + mFocusRect.top
            imageRect.bottom < mFocusRect.bottom -> -imageRect.bottom + mFocusRect.bottom
            else -> 0f
        }

        mMatrix.postTranslate(deltaX, deltaY)
    }

    // fix scale rate of image
    private fun fixScale() {
        val imageMatrixValues = FloatArray(9)
        mMatrix.getValues(imageMatrixValues)
        val currentScale = Math.abs(imageMatrixValues[0] + Math.abs(imageMatrixValues[1]))
        val minScale = getScale(mRotatedImageWidth, mRotatedImageHeight, mFocusWidth, mFocusHeight, true)
        mMaxScale = minScale * MAX_SCALE

        if (currentScale < minScale) {
            val scale = minScale / currentScale
            mMatrix.postScale(scale, scale)
        } else if (currentScale > mMaxScale) {
            val scale = mMaxScale / currentScale
            mMatrix.postScale(scale, scale)
        }
    }

    // get the max scale rate
    private fun maxPostScale():Float {
        val imageMatrixValues = FloatArray(9)
        mMatrix.getValues(imageMatrixValues)
        val curScale = Math.abs(imageMatrixValues[0]) + Math.abs(imageMatrixValues[1])
        return mMaxScale / curScale
    }

    private fun doubleClick(x: Float, y: Float) {
        val p = FloatArray(9)
        mMatrix.getValues(p)
        val curScale = Math.abs(p[0]) + Math.abs(p[1])
        val minScale = getScale(mRotatedImageWidth, mRotatedImageHeight, mFocusWidth, mFocusHeight, true)
        if (curScale < mMaxScale) {
            val toScale = Math.min(curScale + minScale, mMaxScale) / curScale
            mMatrix.postScale(toScale, toScale, x, y)
        } else {
            val toScale = minScale / curScale
            mMatrix.postScale(toScale, toScale, x, y)
            fixTranslation()
        }

        imageMatrix = mMatrix
    }

    class InnerHandler : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message?) {

            if (mListener == null) return

            val saveFile = msg?.obj as File
            when (msg.what) {
                SAVE_SUCCESS -> mListener!!.onBitmapSaveSuccess(saveFile)
                SAVE_ERROR   -> mListener!!.onBitmapSaveError(saveFile)
            }
        }
    }

    interface OnBitmapSaveCompleteListener {
        fun onBitmapSaveSuccess(file: File)
        fun onBitmapSaveError(file: File)
    }

    /**
     * crop bitmap as expect size
     * @param expectWidth     expect crop width
     * @param exceptHeight    expect crop height
     * @param isSaveRectangle is save image as rectangle
     * @return bitmap after crop
     */
    fun getCropBitmap(expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean):Bitmap? {
        if (expectWidth <= 0 || exceptHeight < 0) return null
        var srcBitmap = (drawable as BitmapDrawable).bitmap
        srcBitmap = rotate(srcBitmap, sumRotationLevel * 90f)
        return makeCropBitmap(srcBitmap, mFocusRect, getImageMatrixRect(), expectWidth, exceptHeight, isSaveRectangle);
    }

    /**
     * rotate target bitmap
     * @param bitmap target bitmap
     * @param degree rotate angle
     */
    fun rotate(bitmap: Bitmap, degree: Float):Bitmap {
        if (degree != 0f) {
            val matrix = Matrix()
            matrix.setRotate(degree, bitmap.width / 2f, bitmap.height / 2f)

            try {
                val rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width,
                        bitmap.height, matrix, true)

                if (bitmap != rotateBitmap) {
                    return rotateBitmap
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
        }

        return bitmap
    }

    // get the area where image is can be seen
    private fun getImageMatrixRect(): RectF {
        val rectF = RectF()
        rectF.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        mMatrix.mapRect(rectF)
        return rectF
    }

    /**
     * @param mBitmap         target bitmap
     * @param focusRect       crop focus area
     * @param imageMatrixRect the area of the part of image which is in focus area
     * @param expectWidth     expect width image will be stretched when image height is not enough
     * @param exceptHeight    expect height image will be stretched when image height is not enough
     * @param isSaveRectangle is save image as rectangle
     * @return bitmap after crop
     */
    private fun makeCropBitmap(mBitmap: Bitmap?, focusRect: RectF, imageMatrixRect: RectF?, expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean): Bitmap? {
        if (imageMatrixRect == null || mBitmap == null) return null

        var bitmap = mBitmap
        val scale = imageMatrixRect.width() / bitmap.width
        var left = ((focusRect.left - imageMatrixRect.left) / scale).toInt()
        var top = ((focusRect.top - imageMatrixRect.top) / scale).toInt()
        var width = (focusRect.width() / scale).toInt()
        var height = (focusRect.height() / scale).toInt()

        if (left < 0) left = 0
        if (top < 0) top = 0
        if (left + width > bitmap.width) width = bitmap.width - left
        if (top + height > bitmap.height) height = bitmap.height - top

        try {
            bitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            if (expectWidth != width || exceptHeight != height) {
                bitmap = Bitmap.createScaledBitmap(bitmap, expectWidth, exceptHeight, true)
                if (mStyle == CropImageView.Style.CIRCLE && !isSaveRectangle) {
                    val length = Math.min(expectWidth, exceptHeight)
                    val radius = length / 2
                    val circleBitmap = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(circleBitmap)
                    val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    val paint = Paint()
                    paint.shader = bitmapShader
                    canvas.drawCircle(expectWidth / 2f, exceptHeight / 2f, radius.toFloat(), paint)
                    bitmap = circleBitmap
                }
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        return bitmap
    }

    /**
     * save bitmap as expect size
     * @param folder          expect folder
     * @param expectWidth     expect crop width
     * @param exceptHeight    expect crop height
     * @param isSaveRectangle is save image as rectangle
     * @return bitmap after crop
     */
    fun saveBitmapToFile(folder: File, expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean) {
        if (mSaving) return
        mSaving = true
        val croppedImage = getCropBitmap(expectWidth, exceptHeight, isSaveRectangle)
        var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        var saveFile = createFile(folder, "IMG_", ".jpg")
        if (mStyle == CropImageView.Style.CIRCLE && !isSaveRectangle) {
            outputFormat = Bitmap.CompressFormat.PNG
            saveFile = createFile(folder, "IMG_", ".png")
        }
        val finalOutputFormat = outputFormat
        val finalSaveFile = saveFile
        object : Thread() {
            override fun run() {
                saveOutput(croppedImage!!, finalOutputFormat, finalSaveFile)
            }
        }.start()
    }

    private fun createFile(folder: File, prefix: String, suffix: String): File {
        if (!folder.exists() || !folder.isDirectory) folder.mkdirs()
        try {
            val nomedia = File(folder, ".nomedia")  //在当前文件夹底下创建一个 .nomedia 文件
            if (!nomedia.exists()) nomedia.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
        val filename = prefix + dateFormat.format(Date(System.currentTimeMillis())) + suffix
        return File(folder, filename)
    }

    private fun saveOutput(croppedImage: Bitmap, outputFormat: Bitmap.CompressFormat, saveFile: File) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(Uri.fromFile(saveFile))
            if (outputStream != null) croppedImage.compress(outputFormat, 90, outputStream)
            Message.obtain(mHandler, SAVE_SUCCESS, saveFile).sendToTarget()
        } catch (ex: IOException) {
            ex.printStackTrace()
            Message.obtain(mHandler, SAVE_ERROR, saveFile).sendToTarget()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        mSaving = false
        croppedImage.recycle()
    }

    fun setOnBitmapSaveCompleteListener(listener: OnBitmapSaveCompleteListener?) {
        mListener = listener
    }

    fun getFocusWidth() = mFocusWidth

    fun setFocusWidth(width: Int) {
        mFocusWidth = width
        initImageAndFocusView()
    }

    fun getFocusHeight() = mFocusHeight

    fun setFocusHeight(height: Int) {
        mFocusHeight = height
        initImageAndFocusView()
    }

    fun getMaskColor() = mMaskColor

    fun setMaskColor(color: Int) {
        mMaskColor = color.toLong()
        invalidate()
    }

    fun getFocusColor() = mBorderColor

    fun setBorderColor(color: Int) {
        mBorderColor = color.toLong()
        invalidate()
    }

    fun getBorderWidth() = mBorderWidth.toFloat()

    fun setBorderWidth(width: Int) {
        mBorderWidth = width
        invalidate()
    }

    fun setFocusStyle(style: Style) {
        this.mStyle = style
        invalidate()
    }

    fun getFocusStyle() = mStyle
}