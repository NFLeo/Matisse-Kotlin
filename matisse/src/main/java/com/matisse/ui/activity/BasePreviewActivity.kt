package com.matisse.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import android.view.View
import android.view.WindowManager
import com.gyf.barlibrary.BarHide
import com.gyf.barlibrary.ImmersionBar
import com.matisse.R
import com.matisse.entity.ConstValue
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.PreviewPagerAdapter
import com.matisse.ui.view.PreviewItemFragment
import com.matisse.utils.PathUtils
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.Platform
import com.matisse.utils.UIUtils
import com.matisse.widget.CheckView
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_media_preview.*
import kotlinx.android.synthetic.main.include_view_bottom.*

/**
 * desc：BasePreviewActivity</br>
 * time: 2018/9/6-11:15</br>
 * author：liubo </br>
 * since V 1.0.0 </br>
 */
open class BasePreviewActivity : BaseActivity(), View.OnClickListener,
    ViewPager.OnPageChangeListener {

    lateinit var selectedCollection: SelectedItemCollection
    var adapter: PreviewPagerAdapter? = null
    var previousPos = -1
    var originalEnable = false


    override fun configActivity() {
        super.configActivity()
        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_STATUS_BAR)?.init()
        }

        if (Platform.hasKitKat19()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        selectedCollection = SelectedItemCollection(this)
        originalEnable = if (instanceState == null) {
            selectedCollection.onCreate(intent.getBundleExtra(ConstValue.EXTRA_DEFAULT_BUNDLE))
            intent.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false)
        } else {
            selectedCollection.onCreate(instanceState)
            instanceState!!.getBoolean(ConstValue.CHECK_STATE)
        }
    }

    override fun getResourceLayoutId() = R.layout.activity_media_preview

    override fun setViewData() {
        button_preview.setText(getAttrString(R.attr.Preview_Back_text, R.string.button_back))

        adapter = PreviewPagerAdapter(supportFragmentManager, null)
        pager?.adapter = adapter
        check_view.setCountable(spec?.isCountable() == true)
        updateApplyButton()
    }

    override fun initListener() {
        button_preview.setOnClickListener(this)
        button_apply.setOnClickListener(this)
        check_view.setOnClickListener(this)
        original_layout.setOnClickListener(this)
        pager?.addOnPageChangeListener(this)
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        selectedCollection.asList().forEach {
            if (it.isImage()) {
                val size = PhotoMetadataUtils.getSizeInMB(it.size)
                if (size > spec?.originalMaxSize ?: 0) count++
            }
        }

        return count
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedCollection.onSaveInstanceState(outState)
        outState.putBoolean(ConstValue.CHECK_STATE, originalEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        sendBackResult(false)
        super.onBackPressed()
    }

    private fun sendBackResult(apply: Boolean) {
        val intent = Intent()
        intent.putExtra(ConstValue.EXTRA_RESULT_BUNDLE, selectedCollection.getDataWithBundle())
        intent.putExtra(ConstValue.EXTRA_RESULT_APPLY, apply)
        intent.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        setResult(Activity.RESULT_OK, intent)

        if (apply) finish()
    }

    @SuppressLint("SetTextI18n")
    private fun updateApplyButton() {
        val selectedCount = selectedCollection.count()

        button_apply.apply {
            when (selectedCount) {
                0 -> {
                    text = getString(
                        getAttrString(R.attr.Preview_Confirm_text, R.string.button_sure_default)
                    )
                    isEnabled = false
                }
                1 -> {
                    isEnabled = true

                    text = if (spec?.singleSelectionModeEnabled() == true) {
                        getString(R.string.button_sure_default)
                    } else {
                        "${getString(
                            getAttrString(R.attr.Preview_Confirm_text, R.string.button_sure_default)
                        )}($selectedCount)"
                    }
                }
                else -> {
                    isEnabled = true
                    text = "${getString(
                        getAttrString(R.attr.Preview_Confirm_text, R.string.button_sure_default)
                    )}($selectedCount)"
                }
            }
        }

        if (spec?.originalable == true) {
            UIUtils.setViewVisible(true, original_layout)
            updateOriginalState()
        } else {
            UIUtils.setViewVisible(false, original_layout)
        }
    }

    private fun updateOriginalState() {
        original?.setChecked(originalEnable)
        if (countOverMaxSize() > 0) {
            if (originalEnable) {
                val incapableDialog = IncapableDialog.newInstance(
                    "", getString(R.string.error_over_original_size, spec!!.originalMaxSize)
                )
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                original?.setChecked(false)
                originalEnable = false
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        val adapter = pager?.adapter as PreviewPagerAdapter

        check_view.apply {
            if (previousPos != -1 && previousPos != position) {
                (adapter.instantiateItem(pager, previousPos) as PreviewItemFragment).resetView()
                val item = adapter.getMediaItem(position)
                if (spec?.isCountable() == true) {
                    val checkedNum = selectedCollection.checkedNumOf(item)
                    setCheckedNum(checkedNum)
                    if (checkedNum > 0) {
                        setEnable(true)
                    } else {
                        setEnable(!selectedCollection.maxSelectableReached(item))
                    }
                } else {
                    val checked = selectedCollection.isSelected(item)
                    setChecked(checked)
                    if (checked) setEnable(true) else setEnable(
                        !selectedCollection.maxSelectableReached(
                            item
                        )
                    )
                }
                updateSize(item)
            }
        }

        previousPos = position
    }

    fun updateSize(item: Item?) {
        item?.apply {
            tv_size.apply {
                if (isGif()) {
                    UIUtils.setViewVisible(true, this)
                    text = String.format(
                        getString(R.string.picture_size), PhotoMetadataUtils.getSizeInMB(size)
                    )
                } else {
                    UIUtils.setViewVisible(false, this)
                }
            }

            original_layout?.apply {
                if (isVideo()) {
                    UIUtils.setViewVisible(false, this)
                } else if (spec?.originalable == true) {
                    UIUtils.setViewVisible(true, this)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            button_preview -> onBackPressed()
            button_apply -> {
                if (spec?.openCrop() == true) {
                    val item = adapter?.getMediaItem(pager.currentItem)

                    if (spec?.isSupportCrop(item) == true) {
                        val intentCrop = Intent(this, ImageCropActivity::class.java)
                        intentCrop.putExtra(
                            ConstValue.EXTRA_RESULT_SELECTION_PATH,
                            PathUtils.getPath(this, item?.getContentUri()!!)
                        )
                        startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
                    } else {
                        sendBackResult(true)
                    }
                } else {
                    sendBackResult(true)
                }
            }

            original_layout -> {
                val count = countOverMaxSize()
                if (count > 0) {
                    val incapableDialog = IncapableDialog.newInstance(
                        "",
                        getString(R.string.error_over_original_count, count, spec?.originalMaxSize)
                    )
                    incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                    return
                }

                originalEnable = !originalEnable
                original?.setChecked(originalEnable)

                spec?.onCheckedListener?.onCheck(originalEnable)
            }

            check_view -> {
                val item = adapter?.getMediaItem(pager.currentItem)
                if (selectedCollection.isSelected(item)) {
                    selectedCollection.remove(item)
                    if (spec?.isCountable() == true) {
                        check_view.setCheckedNum(CheckView.UNCHECKED)
                    } else {
                        check_view.setChecked(false)
                    }
                } else {
//                    if (spec?.maxImageSelectable ?: 0 <= 1)
//                        selectedCollection.removeAll()

                    if (assertAddSelection(item)) {
                        selectedCollection.add(item)
                        if (spec?.isCountable() == true) {
                            check_view.setCheckedNum(selectedCollection.checkedNumOf(item))
                        } else {
                            check_view.setChecked(true)
                        }
                    }
                }

                updateApplyButton()

                spec?.onSelectedListener?.onSelected(
                    selectedCollection.asListOfUri(), selectedCollection.asListOfString()
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == ConstValue.REQUEST_CODE_CROP) {
            val resultPath = data?.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE)
            val result = Intent().putExtra(ConstValue.EXTRA_RESULT_BUNDLE, resultPath)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun assertAddSelection(item: Item?): Boolean {
        val cause = selectedCollection.isAcceptable(item)
        IncapableCause.handleCause(this, cause)
        return cause == null
    }
}