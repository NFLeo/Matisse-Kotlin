package com.matisse.ui.activity

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.matisse.R
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.UIUtils

abstract class BaseActivity : AppCompatActivity() {

    lateinit var activity: Activity
    var spec: SelectionSpec? = null
    var instanceState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        spec = SelectionSpec.getInstance()
        setTheme(spec?.themeId ?: R.style.Matisse_Default)
        super.onCreate(savedInstanceState)
        if (safeCancelActivity()) return
        activity = this
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
        if (spec?.needOrientationRestriction() == true) {
            requestedOrientation = spec?.orientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    abstract fun getResourceLayoutId(): Int

    private fun configSaveInstanceState(savedInstanceState: Bundle?) {
        instanceState = savedInstanceState
    }

    abstract fun setViewData()

    abstract fun initListener()

    /**
     * 获取主题配置中的属性值
     * @param attr 主题配置属性key
     * @param defaultRes 默认值
     */
    fun getAttrString(attr: Int, defaultRes: Int) = UIUtils.getAttrString(this, attr, defaultRes)
}