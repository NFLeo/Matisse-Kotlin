package com.matisse.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.support.v4.app.FragmentActivity
import android.util.DisplayMetrics
import android.util.TypedValue
import android.util.TypedValue.applyDimension
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.widget.IncapableDialog

object UIUtils {

    fun handleCause(context: Context, cause: IncapableCause?) {
        if (cause == null)
            return

        when (cause.mForm) {
            IncapableCause.NONE -> {
                // do nothing.
            }
            IncapableCause.DIALOG -> {
                // Show description with dialog
                val incapableDialog = IncapableDialog.newInstance(cause.mTitle, cause.mMessage)
                incapableDialog.show((context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name)
            }
        // default is TOAST
            IncapableCause.TOAST -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun spanCount(context: Context, gridExpectedSize: Int): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val expected = screenWidth / gridExpectedSize
        var spanCount = Math.round(expected.toFloat())
        if (spanCount == 0) {
            spanCount = 1
        }

        return spanCount
    }

    fun setTextDrawable(context: Context, textView: TextView?, attr: Int) {
        if (textView == null) {
            return
        }

        val drawables = textView.compoundDrawables
        val ta = context.theme.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0)
        ta.recycle()

        for (i in drawables.indices) {
            val drawable = drawables[i]
            if (drawable != null) {
                val state = drawable.constantState ?: continue

                val newDrawable = state.newDrawable().mutate()
                newDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                newDrawable.bounds = drawable.bounds
                drawables[i] = newDrawable
            }
        }

        textView.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    /**
     * 设置控件显示隐藏
     * 避免控件重复设置，统一提前添加判断
     *
     * @param isVisible true visible
     * @param view      targetview
     */
    fun setViewVisible(isVisible: Boolean, view: View?) {
        if (view == null) {
            return
        }
        val visibleFlag = if (isVisible) View.VISIBLE else View.GONE

        if (view.visibility != visibleFlag) {
            view.visibility = visibleFlag
        }
    }

    fun dp2px(context: Context, dipValue: Float): Float {
        val mDisplayMetrics = getDisplayMetrics(context)
        return applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, mDisplayMetrics)
    }

    /**
     * 获取屏幕尺寸与密度.
     * @param context the context
     * @return mDisplayMetrics
     */
    private fun getDisplayMetrics(context: Context?): DisplayMetrics {
        val mResources: Resources = if (context == null) {
            Resources.getSystem()
        } else {
            context.resources
        }
        return mResources.displayMetrics
    }

    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()// 创建了一张白纸
        windowManager.defaultDisplay.getMetrics(outMetrics)// 给白纸设置宽高
        return outMetrics.widthPixels
    }

    /**
     * 获取屏幕的高度px
     * @param context 上下文
     * @return 屏幕高px
     */
    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()// 创建了一张白纸
        windowManager.defaultDisplay.getMetrics(outMetrics)// 给白纸设置宽高
        return outMetrics.heightPixels
    }
}