package com.matisse.ui.view

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
import com.matisse.widget.CheckView
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_media_preview.*
import kotlinx.android.synthetic.main.include_view_bottom.*

/**
 * Created by liubo on 2018/9/6.
 */
open class BasePreviewActivity : AppCompatActivity(),
        View.OnClickListener, ViewPager.OnPageChangeListener {

    val mSelectedCollection = SelectedItemCollection(this)
    var mSpec: SelectionSpec? = null
    var mAdapter: PreviewPagerAdapter? = null

    var mPreviousPos = -1
    var originalEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        mSpec = SelectionSpec.getInstance()
        setTheme(mSpec!!.themeId)
        super.onCreate(savedInstanceState)

        if (!mSpec!!.hasInited) {
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
        if (mSpec!!.needOrientationRestriction()) {
            requestedOrientation = mSpec!!.orientation
        }

        originalEnable = if (savedInstanceState == null) {
            mSelectedCollection.onCreate(intent.getBundleExtra(ConstValue.EXTRA_DEFAULT_BUNDLE))
            intent.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false)
        } else {
            mSelectedCollection.onCreate(savedInstanceState)
            savedInstanceState.getBoolean(ConstValue.CHECK_STATE)
        }

        button_preview.text = getString(R.string.button_back)
        button_preview.setOnClickListener(this)
        button_apply.setOnClickListener(this)

        pager?.addOnPageChangeListener(this)
        mAdapter = PreviewPagerAdapter(supportFragmentManager, null)
        pager?.adapter = mAdapter

        check_view.setCountable(mSpec!!.countable)
        check_view.setOnClickListener(this)

        original_layout.setOnClickListener(this)

        updateApplyButton()
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = mSelectedCollection.count()
        for (i in 0 until selectedCount) {
            val item: Item = mSelectedCollection.asList()[i]
            if (item.isImage()) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > mSpec!!.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mSelectedCollection.onSaveInstanceState(outState)
        outState.putBoolean(ConstValue.CHECK_STATE, originalEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        sendBackResult(false)
        super.onBackPressed()
    }

    private fun sendBackResult(apply: Boolean) {
        val intent = Intent()
        intent.putExtra(ConstValue.EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle())
        intent.putExtra(ConstValue.EXTRA_RESULT_APPLY, apply)
        intent.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun updateApplyButton() {
        val selectedCount = mSelectedCollection.count()

        button_apply.apply {
            when (selectedCount) {
                0 -> {
                    text = getString(R.string.button_sure_default)
                    isEnabled = false
                }
                1 -> {
                    isEnabled = true

                    text = if (mSpec!!.singleSelectionModeEnabled()) {
                        getString(R.string.button_sure_default)
                    } else {
                        getString(R.string.button_sure, selectedCount)
                    }
                }
                else -> {
                    isEnabled = true
                    text = getString(R.string.button_sure, selectedCount)
                }
            }
        }

        if (mSpec!!.originalable) {
            original_layout?.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            original_layout?.visibility = View.GONE
        }
    }

    private fun updateOriginalState() {
        original?.setChecked(originalEnable)
        if (countOverMaxSize() > 0) {
            if (originalEnable) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
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
            if (mPreviousPos != -1 && mPreviousPos != position) {
                (adapter.instantiateItem(pager!!, mPreviousPos) as PreviewItemFragment).resetView()
                val item = adapter.getMediaItem(position)
                if (mSpec!!.countable) {
                    val checkedNum = mSelectedCollection.checkedNumOf(item)
                    setCheckedNum(checkedNum)
                    if (checkedNum > 0) {
                        setEnable(true)
                    } else {
                        setEnable(!mSelectedCollection.maxSelectableReached())
                    }
                } else {
                    val checked = mSelectedCollection.isSelected(item)
                    setChecked(checked)
                    if (checked) setEnable(true) else setEnable(!mSelectedCollection.maxSelectableReached())
                }
                updateSize(item)
            }
        }

        mPreviousPos = position
    }

    fun updateSize(item: Item?) {
        item?.apply {
            tv_size.apply {
                if (isGif()) {
                    visibility = View.VISIBLE
                    text = "${PhotoMetadataUtils.getSizeInMB(size)} M"
                } else {
                    visibility = View.GONE
                }
            }

            original_layout?.apply {
                if (isVideo()) {
                    visibility = View.GONE
                } else if (mSpec!!.originalable) {
                    visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            button_preview -> onBackPressed()
            button_apply -> {
                if (mSpec?.openCrop() == true) {
                    val item = mAdapter?.getMediaItem(pager.currentItem)

                    if (mSpec?.isSupportCrop(item) == true) {
                        val intentCrop = Intent(this, ImageCropActivity::class.java)
                        intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, PathUtils.getPath(this, item?.getContentUri()!!))
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
                    val incapableDialog = IncapableDialog.newInstance("",
                            getString(R.string.error_over_original_count, count, mSpec?.originalMaxSize))
                    incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                    return
                }

                originalEnable = !originalEnable
                original?.setChecked(originalEnable)

                if (mSpec?.onCheckedListener != null) {
                    mSpec?.onCheckedListener!!.onCheck(originalEnable)
                }
            }

            check_view -> {
                val item = mAdapter?.getMediaItem(pager!!.currentItem)
                if (mSelectedCollection.isSelected(item!!)) {
                    mSelectedCollection.remove(item)
                    if (mSpec!!.countable) {
                        check_view.setCheckedNum(CheckView.UNCHECKED)
                    } else {
                        check_view.setChecked(false)
                    }
                } else {
                    if (mSpec?.maxImageSelectable!! <= 1) {
                        mSelectedCollection.removeAll()
                    }

                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item)
                        if (mSpec!!.countable) {
                            check_view.setCheckedNum(mSelectedCollection.checkedNumOf(item))
                        } else {
                            check_view.setChecked(true)
                        }
                    }
                }

                updateApplyButton()

                mSpec?.onSelectedListener?.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return

        if (requestCode == ConstValue.REQUEST_CODE_CROP) {

            val resultPath = data?.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE)
            val result = Intent()
            result.putExtra(ConstValue.EXTRA_RESULT_BUNDLE, resultPath)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun assertAddSelection(item: Item): Boolean {
        val cause = mSelectedCollection.isAcceptable(item)
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