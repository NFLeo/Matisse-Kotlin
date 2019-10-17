package com.matisse.ui.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.gyf.barlibrary.ImmersionBar
import com.matisse.R
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.Platform

abstract class BaseActivity : AppCompatActivity() {

    var spec: SelectionSpec? = null
    var instanceState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        spec = SelectionSpec.getInstance()
        setTheme(spec?.themeId ?: R.style.Matisse_Default)
        super.onCreate(savedInstanceState)
        if (safeCancelActivity()) return
        setContentView(getResourceLayoutId())
        configActivity()
        configSaveInstanceState(savedInstanceState)
        setViewData()
        initListener()
    }

    private fun safeCancelActivity(): Boolean {
        if (spec?.hasInited == false) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }

        return false
    }

    /**
     * 处理状态栏(状态栏颜色、状态栏字体颜色、是否隐藏等操作)
     *
     * 空实现，供外部重写
     */
    open fun configActivity() {
    }

    abstract fun getResourceLayoutId(): Int

    private fun configSaveInstanceState(savedInstanceState: Bundle?) {
        instanceState = savedInstanceState
    }

    abstract fun setViewData()

    abstract fun initListener()

    override fun onDestroy() {
        super.onDestroy()
        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).destroy()
        }
    }
}