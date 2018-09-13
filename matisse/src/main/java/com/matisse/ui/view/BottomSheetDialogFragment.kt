package com.matisse.ui.view

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatDialogFragment
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import com.matisse.R

abstract class BottomSheetDialogFragment : AppCompatDialogFragment() {

    private lateinit var mBehavior: BottomSheetBehavior<*>
    private var coordinator: ViewGroup? = null
    private var bottomSheet: FrameLayout? = null
    private var contentView: View? = null
    private var mDefaultHeight = -1
    var mCancelable = true

    private var mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        coordinator = inflater.inflate(R.layout.dialog_bottom_sheet, container) as ViewGroup
        bottomSheet = coordinator?.findViewById(R.id.design_bottom_sheet)

        mBehavior = BottomSheetBehavior.from(bottomSheet)
        mBehavior.setBottomSheetCallback(mBottomSheetCallback)
        mBehavior.isHideable = mCancelable

        contentView = getContentView(inflater, coordinator!!)
        bottomSheet?.addView(contentView)

        if (mDefaultHeight != -1) {
            setDefaultHeight(mDefaultHeight)
        }

        // 设置 dialog 位于屏幕底部，并且设置出入动画
        setBottomLayout()
        setPeekHeight()
        initBackAction()

        return coordinator
    }

    fun setDefaultHeight(defaultHeight: Int) {
        mDefaultHeight = defaultHeight
        if (bottomSheet != null) {
            bottomSheet?.layoutParams?.width = -1
            bottomSheet?.layoutParams?.height = defaultHeight
        }
    }

    private fun setPeekHeight() {
        val dm = DisplayMetrics()
        //取得窗口属性
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        //窗口高度
        val screenHeight = dm.heightPixels
        mBehavior.peekHeight = screenHeight
    }

    private fun initBackAction() {
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                backAction()
            } else false
        }
    }

    override fun setCancelable(cancelable: Boolean) {
        super.setCancelable(cancelable)
        if (mCancelable != cancelable) {
            mCancelable = cancelable
            mBehavior.isHideable = cancelable
        }
    }

    private fun setBottomLayout() {
        val win = dialog.window
        if (win != null) {
            win.setBackgroundDrawableResource(R.drawable.transparent)
            win.decorView.setPadding(0, 0, 0, 0)
            val lp = win.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            win.attributes = lp
            // dialog 布局位于底部
            win.setGravity(Gravity.BOTTOM)
            // 设置进出场动画
            win.setWindowAnimations(R.style.Animation_Bottom)
        }
    }

//    override fun show(manager: FragmentManager, tag: String) {
//        val ft = manager.beginTransaction()
//        ft.add(this, tag)
//        ft.commitAllowingStateLoss()
//        // 解决 调用show后无法获取布局
//        manager.executePendingTransactions()
//    }

    abstract fun getContentView(inflater: LayoutInflater, container: ViewGroup): View

    open fun backAction() = false
}