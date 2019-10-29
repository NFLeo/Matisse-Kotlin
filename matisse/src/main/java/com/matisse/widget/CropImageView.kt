package com.matisse.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.view.ViewCompat
import androidx.appcompat.widget.AppCompatImageView
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
import kotlin.math.*

class CropImageView : AppCompatImageView {

    //---------------- focus frame attributes start ------------//
    enum class Style {
        RECTANGLE, CIRCLE
    }

    companion object {
        private const val MAX_SCALE = 4F
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val ROTATE = 3
        private const val ZOOM_OR_ROTATE = 4
        private const val SAVE_SUCCESS = 1001
        private const val SAVE_ERROR = 1002

        var kHandler = InnerHandler()
        var kListener: OnBitmapSaveCompleteListener? = null
    }

    private val styles = arrayOf(Style.RECTANGLE, Style.CIRCLE)

    private var maskColor = 0XAF000000     // dark
    private var borderColor = 0XAA808080   // focusFrame border color
    private var borderWidth = 1            // focusFrame border width
    private var focusWidth = 250           // focusFrame width
    private var focusHeight = 250          // focusFrame height
    private var defaultStyleIndex = 0      // default style
    private var style = styles[defaultStyleIndex]
    private val borderPaint = Paint()
    private val focusPath = Path()
    private val focusRect = RectF()
    //---------------- focus frame attributes end ------------//

    private var imageWidth = 0
    private var imageHeight = 0
    private var rotatedImageWidth = 0
    private var rotatedImageHeight = 0
    private var cMatrix = Matrix()          // matrix when image is changing
    private var savedMatrix = Matrix()      // matrix when image is stated
    private var pA = PointF()               // pointF of first finger
    private var pB = PointF()               // pointF of second finger
    private var midPoint = PointF()         // middle pointF of two fingers
    private var doubleClickPos = PointF()   // pointF of double click
    private var focusMidPoint = PointF()    // middle pointF of focus frame view

    private var mode = NONE            // init gesture mode
    private var doubleClickTime = 0L        // next double click time
    private var rotation = 0.0              // angle of finger rotation(is`t integer multiple of 90)
    private var oldDist = 1F                // first distance of two fingers
    private var sumRotationLevel = 0        // angle of rotation (is integer multiple of 90)
    private var maxScale = MAX_SCALE // get max scale from different images
    private var isInitSize = false          // is init by onSizeChanged
    private var saving = false              // is saving

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initAttributeSet(context!!, attrs!!)
    }

    private fun initAttributeSet(context: Context, attrs: AttributeSet) {
        focusWidth = getDimension(focusWidth.toFloat()).toInt()
        focusHeight = getDimension(focusHeight.toFloat()).toInt()
        borderWidth = getDimension(borderWidth.toFloat()).toInt()

        val a = context.obtainStyledAttributes(attrs, R.styleable.CropImageView)
        maskColor = a.getColor(R.styleable.CropImageView_cropMaskColor, maskColor.toInt()).toLong()
        borderColor =
            a.getColor(R.styleable.CropImageView_cropBorderColor, borderColor.toInt()).toLong()
        borderWidth =
            a.getDimensionPixelSize(R.styleable.CropImageView_cropBorderWidth, borderWidth)
        focusWidth = a.getDimensionPixelSize(R.styleable.CropImageView_cropFocusWidth, focusWidth)
        focusHeight =
            a.getDimensionPixelSize(R.styleable.CropImageView_cropFocusHeight, focusHeight)
        defaultStyleIndex = a.getInteger(R.styleable.CropImageView_cropStyle, defaultStyleIndex)
        style = styles[defaultStyleIndex]
        a.recycle()

        // set image as matrix scale type
        scaleType = ScaleType.MATRIX
    }

    private fun getDimension(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

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
        if (!isInitSize || d == null) return

        mode = NONE
        cMatrix = imageMatrix
        imageWidth = d.intrinsicWidth
        rotatedImageWidth = d.intrinsicWidth
        imageHeight = d.intrinsicHeight
        rotatedImageHeight = d.intrinsicHeight

        // get center and boundary of the focus view
        val viewWidth = width
        val viewHeight = height
        val midPointX = viewWidth / 2f
        val midPointY = viewHeight / 2f
        focusMidPoint = PointF(midPointX, midPointY)

        if (style == Style.CIRCLE) {
            val focusSize = min(focusWidth, focusHeight)
            focusWidth = focusSize
            focusHeight = focusSize
        }

        focusRect.left = focusMidPoint.x - focusWidth / 2
        focusRect.right = focusMidPoint.x + focusWidth / 2
        focusRect.top = focusMidPoint.y - focusHeight / 2
        focusRect.bottom = focusMidPoint.y + focusHeight / 2

        // the min slide of image must larger than the min slide of focus view
        val fitFocusScale = getScale(imageWidth, imageHeight, focusWidth, focusHeight, true)
        maxScale = fitFocusScale * MAX_SCALE

        // there is one slide at least of image to fit the parent
        val fitViewScale = getScale(imageWidth, imageHeight, viewWidth, viewHeight, false)

        // make sure the final scale rate fit focus view priority
        val scale = max(fitViewScale, fitFocusScale)
        // scale with the center of image center Point
        cMatrix.setScale(scale, scale, imageWidth / 2f, imageHeight / 2f)
        val mImageMatrixValues = FloatArray(9)
        // get mImageMatrixValues after scaled
        cMatrix.getValues(mImageMatrixValues)
        val transX =
            focusMidPoint.x - (mImageMatrixValues[2] + imageWidth * mImageMatrixValues[0] / 2)
        val transY =
            focusMidPoint.y - (mImageMatrixValues[5] + imageHeight * mImageMatrixValues[4] / 2)
        cMatrix.postTranslate(transX, transY)
        imageMatrix = cMatrix
        invalidate()
    }

    // calculate boundary scale rate
    private fun getScale(
        bitmapWidth: Int, bitmapHeight: Int, minWidth: Int, minHeight: Int, isMinScale: Boolean
    ): Float {
        val scale: Float
        val scaleX = minWidth.toFloat() / bitmapWidth
        val scaleY = minHeight.toFloat() / bitmapHeight
        scale = if (isMinScale) max(scaleX, scaleY) else min(scaleX, scaleY)

        return scale
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            if (Style.RECTANGLE == style) {
                focusPath.addRect(focusRect, Path.Direction.CCW)
            } else if (Style.CIRCLE == style) {
                val radius = min(
                    (focusRect.right - focusRect.left) / 2, (focusRect.bottom - focusRect.top) / 2
                )
                focusPath.addCircle(focusMidPoint.x, focusMidPoint.y, radius, Path.Direction.CCW)
            }

            save()
            clipRect(0, 0, width, height)
            if (Platform.hasKitO26()) {
                clipOutPath(focusPath)
            } else {
                clipPath(focusPath, Region.Op.DIFFERENCE)
            }
            drawColor(maskColor.toInt())
            restore()
        }

        borderPaint.apply {
            color = borderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = borderWidth.toFloat()
            isAntiAlias = true
            canvas?.drawPath(focusPath, borderPaint)
            focusPath.reset()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (saving || null == drawable) return super.onTouchEvent(event)

        when (event?.action!! and MotionEvent.ACTION_MASK) {
            // one point press down
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(cMatrix)
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
                    savedMatrix.set(cMatrix)
                    if (oldDist > 10f) mode = ZOOM_OR_ROTATE
                }
            }

            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> handleActionUp()
        }

        // to fix some device can't drag
        ViewCompat.postInvalidateOnAnimation(this)
        return true
    }

    private fun handleActionMove(event: MotionEvent) {
        if (mode == ZOOM_OR_ROTATE) {
            val pC = PointF(
                event.getX(1) - event.getX(0) + pA.x,
                event.getY(1) - event.getY(0) + pA.y
            )
            val a = spacing(pB.x, pB.y, pC.x, pC.y).toDouble()
            val b = spacing(pA.x, pA.y, pC.x, pC.y).toDouble()
            val c = spacing(pA.x, pA.y, pB.x, pB.y).toDouble()
            if (a >= 10) {
                val cosB = (a * a + c * c - b * b) / (2.0 * a * c)
                val angleB = acos(cosB)
                val pid4 = Math.PI / 4
                //旋转时，默认角度在 45 - 135 度之间
                mode = if (angleB > pid4 && angleB < 3 * pid4) ROTATE else ZOOM
            }
        }
        if (mode == DRAG) {
            cMatrix.set(savedMatrix)
            cMatrix.postTranslate(event.x - pA.x, event.y - pA.y)
            fixTranslation()
            imageMatrix = cMatrix
        } else if (mode == ZOOM) {
            val newDist = spacing(
                event.getX(0), event.getY(0),
                event.getX(1), event.getY(1)
            )
            if (newDist > 10f) {
                cMatrix.set(savedMatrix)
                // 这里之所以用 maxPostScale 矫正一下，主要是防止缩放到最大时，继续缩放图片会产生位移
                val tScale = min(newDist / oldDist, maxPostScale())
                if (tScale != 0f) {
                    cMatrix.postScale(tScale, tScale, midPoint.x, midPoint.y)
                    fixScale()
                    fixTranslation()
                    imageMatrix = cMatrix
                }
            }
        } else if (mode == ROTATE) {
            val pC = PointF(
                event.getX(1) - event.getX(0) + pA.x,
                event.getY(1) - event.getY(0) + pA.y
            )
            val a = spacing(pB.x, pB.y, pC.x, pC.y).toDouble()
            val b = spacing(pA.x, pA.y, pC.x, pC.y).toDouble()
            val c = spacing(pA.x, pA.y, pB.x, pB.y).toDouble()
            if (b > 10) {
                val cosA = (b * b + c * c - a * a) / (2.0 * b * c)
                var angleA = acos(cosA)
                val ta = (pB.y - pA.y).toDouble()
                val tb = (pA.x - pB.x).toDouble()
                val tc = (pB.x * pA.y - pA.x * pB.y).toDouble()
                val td = ta * pC.x + tb * pC.y + tc
                if (td > 0) angleA = 2 * Math.PI - angleA

                rotation = angleA
                cMatrix.set(savedMatrix)
                cMatrix.postRotate((rotation * 180 / Math.PI).toFloat(), midPoint.x, midPoint.y)
                imageMatrix = cMatrix
            }
        }
    }

    private fun handleActionUp() {
        if (mode == DRAG) {
            if (spacing(pA, pB) < 50) {
                var now = System.currentTimeMillis()
                if (now - doubleClickTime < 500 && spacing(pA, doubleClickPos) < 50) {
                    doubleClick(pA.x, pA.y)
                    now = 0
                }
                doubleClickPos.set(pA)
                doubleClickTime = now
            }
        } else if (mode == ROTATE) {
            var rotateLevel = floor((rotation + Math.PI / 4) / (Math.PI / 2)).toInt()
            if (rotateLevel == 4) rotateLevel = 0
            cMatrix.set(savedMatrix)
            cMatrix.postRotate((90 * rotateLevel).toFloat(), midPoint.x, midPoint.y)
            if (rotateLevel == 1 || rotateLevel == 3) {
                val tmp = rotatedImageWidth
                rotatedImageWidth = rotatedImageHeight
                rotatedImageHeight = tmp
            }
            fixScale()
            fixTranslation()
            imageMatrix = cMatrix
            sumRotationLevel += rotateLevel
        }
        mode = NONE
    }

    // get distance between two points
    private fun spacing(pA: PointF, pB: PointF) = spacing(pA.x, pA.y, pB.x, pB.y)

    private fun spacing(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    // fix translation of image
    private fun fixTranslation() {
        val imageRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
        // get the area of image scale after
        cMatrix.mapRect(imageRect)

        var deltaX = 0f
        var deltaY = 0f
        if (imageRect.left > focusRect.left) {
            deltaX = -imageRect.left + focusRect.left
        } else if (imageRect.right < focusRect.right) {
            deltaX = -imageRect.right + focusRect.right
        }
        if (imageRect.top > focusRect.top) {
            deltaY = -imageRect.top + focusRect.top
        } else if (imageRect.bottom < focusRect.bottom) {
            deltaY = -imageRect.bottom + focusRect.bottom
        }
        cMatrix.postTranslate(deltaX, deltaY)
    }

    // fix scale rate of image
    private fun fixScale() {
        val imageMatrixValues = FloatArray(9)
        cMatrix.getValues(imageMatrixValues)
        val currentScale = abs(imageMatrixValues[0]) + abs(imageMatrixValues[1])
        val minScale =
            getScale(rotatedImageWidth, rotatedImageHeight, focusWidth, focusHeight, true)
        maxScale = minScale * MAX_SCALE

        if (currentScale < minScale) {
            val scale = minScale / currentScale
            cMatrix.postScale(scale, scale)
        } else if (currentScale > maxScale) {
            val scale = maxScale / currentScale
            cMatrix.postScale(scale, scale)
        }
    }

    // get the max scale rate
    private fun maxPostScale(): Float {
        val imageMatrixValues = FloatArray(9)
        cMatrix.getValues(imageMatrixValues)
        val curScale = abs(imageMatrixValues[0]) + abs(imageMatrixValues[1])
        return maxScale / curScale
    }

    private fun doubleClick(x: Float, y: Float) {
        val p = FloatArray(9)
        cMatrix.getValues(p)
        val curScale = abs(p[0]) + abs(p[1])
        val minScale =
            getScale(rotatedImageWidth, rotatedImageHeight, focusWidth, focusHeight, true)
        if (curScale < maxScale) {
            val toScale = min(curScale + minScale, maxScale) / curScale
            cMatrix.postScale(toScale, toScale, x, y)
        } else {
            val toScale = minScale / curScale
            cMatrix.postScale(toScale, toScale, x, y)
            fixTranslation()
        }

        imageMatrix = cMatrix
    }

    class InnerHandler : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message?) {

            if (kListener == null) return

            val saveFile = msg?.obj as File
            when (msg.what) {
                SAVE_SUCCESS -> kListener?.onBitmapSaveSuccess(saveFile)
                SAVE_ERROR -> kListener?.onBitmapSaveError(saveFile)
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
    private fun getCropBitmap(
        expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean
    ): Bitmap? {
        if (expectWidth <= 0 || exceptHeight < 0 || drawable == null) return null
        var srcBitmap = (drawable as BitmapDrawable).bitmap
        srcBitmap = rotate(srcBitmap, sumRotationLevel * 90f)
        return makeCropBitmap(
            srcBitmap, focusRect, getImageMatrixRect(),
            expectWidth, exceptHeight, isSaveRectangle
        )
    }

    /**
     * rotate target bitmap
     * @param bitmap target bitmap
     * @param degree rotate angle
     */
    fun rotate(bitmap: Bitmap, degree: Float): Bitmap {
        if (degree != 0f) {
            val matrix = Matrix()
            matrix.setRotate(degree, bitmap.width / 2f, bitmap.height / 2f)

            try {
                val rotateBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width,
                    bitmap.height, matrix, true
                )

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
        cMatrix.mapRect(rectF)
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
    private fun makeCropBitmap(
        mBitmap: Bitmap?, focusRect: RectF, imageMatrixRect: RectF?,
        expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean
    ): Bitmap? {
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
                val sizeWidth = formatBitmapSize(isSaveRectangle, expectWidth, exceptHeight, true)
                val sizeHeight = formatBitmapSize(isSaveRectangle, expectWidth, exceptHeight, false)

                bitmap = Bitmap.createScaledBitmap(bitmap!!, sizeWidth, sizeHeight, true)
                if (style == Style.CIRCLE && !isSaveRectangle) {
                    val length = min(expectWidth, exceptHeight)
                    val radius = length / 2
                    val circleBitmap = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(circleBitmap)
                    val bitmapShader =
                        BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
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

    private fun formatBitmapSize(
        isSaveRectangle: Boolean, expectWidth: Int, exceptHeight: Int, widthOrHeight: Boolean
    ) = if (isSaveRectangle) {
        min(expectWidth, exceptHeight)
    } else {
        if (widthOrHeight) expectWidth else exceptHeight
    }

    /**
     * save bitmap as expect size
     * @param folder          expect folder
     * @param expectWidth     expect crop width
     * @param exceptHeight    expect crop height
     * @param isSaveRectangle is save image as rectangle
     * @return bitmap after crop
     */
    fun saveBitmapToFile(
        folder: File, expectWidth: Int, exceptHeight: Int, isSaveRectangle: Boolean
    ) {
        if (saving) return
        saving = true

        // picture in the album maybe the file is not exist
        val croppedImage = getCropBitmap(expectWidth, exceptHeight, isSaveRectangle) ?: return

        var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        var saveFile = createFile(folder, "IMG_", ".jpg")
        if (style == Style.CIRCLE && !isSaveRectangle) {
            outputFormat = Bitmap.CompressFormat.PNG
            saveFile = createFile(folder, "IMG_", ".png")
        }
        val finalOutputFormat = outputFormat
        val finalSaveFile = saveFile
        object : Thread() {
            override fun run() {
                saveOutput(croppedImage, finalOutputFormat, finalSaveFile)
            }
        }.start()
    }

    private fun createFile(folder: File, prefix: String, suffix: String): File {
        if (!folder.exists() || !folder.isDirectory) folder.mkdirs()
        try {
            val noMedia = File(folder, ".nomedia")  //在当前文件夹底下创建一个 .nomedia 文件
            if (!noMedia.exists()) noMedia.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
        val filename = prefix + dateFormat.format(Date(System.currentTimeMillis())) + suffix
        return File(folder, filename)
    }

    @SuppressLint("WrongThread")
    private fun saveOutput(
        croppedImage: Bitmap, outputFormat: Bitmap.CompressFormat, saveFile: File
    ) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(Uri.fromFile(saveFile))
            if (outputStream != null) croppedImage.compress(outputFormat, 90, outputStream)
            Message.obtain(kHandler, SAVE_SUCCESS, saveFile).sendToTarget()
        } catch (ex: IOException) {
            ex.printStackTrace()
            Message.obtain(kHandler, SAVE_ERROR, saveFile).sendToTarget()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        saving = false
        croppedImage.recycle()
    }

    fun setOnBitmapSaveCompleteListener(listener: OnBitmapSaveCompleteListener?) {
        kListener = listener
    }

    fun getFocusWidth() = focusWidth

    fun setFocusWidth(width: Int) {
        focusWidth = width
        initImageAndFocusView()
    }

    fun getFocusHeight() = focusHeight

    fun setFocusHeight(height: Int) {
        focusHeight = height
        initImageAndFocusView()
    }

    fun getMaskColor() = maskColor

    fun setMaskColor(color: Int) {
        maskColor = color.toLong()
        invalidate()
    }

    fun getFocusColor() = borderColor

    fun setBorderColor(color: Int) {
        borderColor = color.toLong()
        invalidate()
    }

    fun getBorderWidth() = borderWidth.toFloat()

    fun setBorderWidth(width: Int) {
        borderWidth = width
        invalidate()
    }

    fun setFocusStyle(style: Style) {
        this.style = style
        invalidate()
    }

    fun getFocusStyle() = style
}