package com.matisse.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.gyf.barlibrary.BarHide
import com.gyf.barlibrary.ImmersionBar
import com.matisse.R
import com.matisse.entity.ConstValue
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.PreviewPagerAdapter
import com.matisse.utils.PathUtils
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.Platform
import com.matisse.utils.UIUtils
import com.matisse.widget.CheckView
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_media_preview.*
import kotlinx.android.synthetic.main.include_view_bottom.*

/**
 * Created by liubo on 2018/9/6.
 */
open class BasePreviewActivity : AppCompatActivity(),
    View.OnClickListener, ViewPager.OnPageChangeListener {

    lateinit var selectedCollection: SelectedItemCollection
    var spec: SelectionSpec? = null
    var adapter: PreviewPagerAdapter? = null

    var previousPos = -1
    var originalEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        spec = SelectionSpec.getInstance()
        setTheme(spec!!.themeId)
        super.onCreate(savedInstanceState)

        if (!spec!!.hasInited) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_STATUS_BAR)?.init()
        }

        setContentView(R.layout.activity_media_preview)
        if (Platform.hasKitKat19()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        if (spec!!.needOrientationRestriction()) {
            requestedOrientation = spec!!.orientation
        }

        selectedCollection = SelectedItemCollection(this)
        originalEnable = if (savedInstanceState == null) {
            selectedCollection.onCreate(intent.getBundleExtra(ConstValue.EXTRA_DEFAULT_BUNDLE))
            intent.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false)
        } else {
            selectedCollection.onCreate(savedInstanceState)
            savedInstanceState.getBoolean(ConstValue.CHECK_STATE)
        }

        button_preview.setText(
            UIUtils.getAttrString(
                this@BasePreviewActivity, R.attr.Preview_Back_text, R.string.button_back
            )
        )

        button_preview.setOnClickListener(this)
        button_apply.setOnClickListener(this)

        pager?.addOnPageChangeListener(this)
        adapter = PreviewPagerAdapter(supportFragmentManager, null)
        pager?.adapter = adapter

        check_view.setCountable(spec!!.countable)
        check_view.setOnClickListener(this)
        original_layout.setOnClickListener(this)

        updateApplyButton()
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = selectedCollection.count()
        for (i in 0 until selectedCount) {
            val item: Item = selectedCollection.asList()[i]
            if (item.isImage()) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > spec!!.originalMaxSize) {
                    count++
                }
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
    }

    @SuppressLint("SetTextI18n")
    private fun updateApplyButton() {
        val selectedCount = selectedCollection.count()

        button_apply.apply {
            when (selectedCount) {
                0 -> {
                    text = getString(
                        UIUtils.getAttrString(
                            this@BasePreviewActivity,
                            R.attr.Preview_Confirm_text, R.string.button_sure_default
                        )
                    )
                    isEnabled = false
                }
                1 -> {
                    isEnabled = true

                    text = if (spec?.singleSelectionModeEnabled() == true) {
                        getString(R.string.button_sure_default)
                    } else {
                        "${getString(
                            UIUtils.getAttrString(
                                this@BasePreviewActivity,
                                R.attr.Preview_Confirm_text,
                                R.string.button_sure_default
                            )
                        )}($selectedCount)"
                    }
                }
                else -> {
                    isEnabled = true
                    text = "${getString(
                        UIUtils.getAttrString(
                            this@BasePreviewActivity,
                            R.attr.Preview_Confirm_text,
                            R.string.button_sure_default
                        )
                    )}($selectedCount)"
                }
            }
        }

        if (spec!!.originalable) {
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
                    "",
                    getString(R.string.error_over_original_size, spec!!.originalMaxSize)
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
                (adapter.instantiateItem(pager!!, previousPos) as PreviewItemFragment).resetView()
                val item = adapter.getMediaItem(position)
                if (spec!!.countable) {
                    val checkedNum = selectedCollection.checkedNumOf(item)
                    setCheckedNum(checkedNum)
                    if (checkedNum > 0) {
                        setEnable(true)
                    } else {
                        setEnable(!selectedCollection.maxSelectableReached())
                    }
                } else {
                    val checked = selectedCollection.isSelected(item)
                    setChecked(checked)
                    if (checked) setEnable(true) else setEnable(!selectedCollection.maxSelectableReached())
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
                    visibility = View.VISIBLE
                    text = String.format(
                        getString(R.string.picture_size), PhotoMetadataUtils.getSizeInMB(size)
                    )
                } else {
                    visibility = View.GONE
                }
            }

            original_layout?.apply {
                if (isVideo()) {
                    visibility = View.GONE
                } else if (spec!!.originalable) {
                    visibility = View.VISIBLE
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
                        finish()
                    }
                } else {
                    sendBackResult(true)
                    finish()
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
                    if (spec?.countable == true) {
                        check_view.setCheckedNum(CheckView.UNCHECKED)
                    } else {
                        check_view.setChecked(false)
                    }
                } else {
                    if (spec?.maxImageSelectable!! <= 1)
                        selectedCollection.removeAll()

                    if (assertAddSelection(item)) {
                        selectedCollection.add(item)
                        if (spec!!.countable) {
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

    override fun onDestroy() {
        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).destroy()
        }
        super.onDestroy()
    }
}