package com.matisse.ui.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.PreviewPagerAdapter
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.Platform
import com.matisse.widget.CheckRadioView
import com.matisse.widget.CheckView
import com.matisse.widget.IncapableDialog

/**
 * Created by liubo on 2018/9/6.
 */
open class BasePreviewActivity : AppCompatActivity(),
        View.OnClickListener, ViewPager.OnPageChangeListener {
    companion object {
        const val EXTRA_DEFAULT_BUNDLE = "extra_default_bundle"
        const val EXTRA_RESULT_BUNDLE = "extra_result_bundle"
        const val EXTRA_RESULT_APPLY = "extra_result_apply"
        const val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
        const val CHECK_STATE = "checkState"
    }

    val mSelectedCollection = SelectedItemCollection(this)
    var mSpec: SelectionSpec? = null
    var mPager: ViewPager? = null

    var mAdapter: PreviewPagerAdapter? = null
    var mCheckView: CheckView? = null
    var mButtonBack: Button? = null
    var mButtonApply: Button? = null
    var mSize: TextView? = null

    var mPreviousPos = -1
    var mOriginalLayout: LinearLayout? = null
    var mOriginal: CheckRadioView? = null
    var mOriginalEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(SelectionSpec.getInstance().themeId)
        super.onCreate(savedInstanceState)
        if (SelectionSpec.getInstance().hasInited) {
            setContentView(R.layout.activity_media_preview)
            if (Platform.hasKitKat19()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
            mSpec = SelectionSpec.getInstance()
            if (mSpec!!.needOrientationRestriction()) {
                requestedOrientation = mSpec!!.orientation
            }

            mOriginalEnable = if (savedInstanceState == null) {
                mSelectedCollection.onCreate(intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE))
                intent.getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false)
            } else {
                mSelectedCollection.onCreate(savedInstanceState)
                savedInstanceState.getBoolean(CHECK_STATE)
            }

            mButtonBack = findViewById(R.id.button_back)
            mButtonApply = findViewById(R.id.button_apply)
            mSize = findViewById(R.id.size)
            mButtonBack?.setOnClickListener(this)
            mButtonApply?.setOnClickListener(this)

            mPager = findViewById(R.id.pager)
            mPager?.addOnPageChangeListener(this)
            mAdapter = PreviewPagerAdapter(supportFragmentManager, null)
            mPager?.adapter = mAdapter
            mCheckView = findViewById(R.id.check_view)
            mCheckView?.setCountable(mSpec!!.countable)

            mCheckView?.setOnClickListener {
                val item = mAdapter?.getMediaItem(mPager!!.currentItem)
                if (mSelectedCollection.isSelected(item!!)) {
                    mSelectedCollection.remove(item)
                    if (mSpec!!.countable) {
                        mCheckView?.setCheckedNum(CheckView.UNCHECKED)
                    } else {
                        mCheckView?.setChecked(false)
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item)
                        if (mSpec!!.countable) {
                            mCheckView?.setCheckedNum(mSelectedCollection.checkedNumOf(item))
                        } else {
                            mCheckView?.setChecked(true)
                        }
                    }
                }

                updateApplyButton()

                mSpec?.onSelectedListener?.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
            }

            mOriginalLayout = findViewById(R.id.originalLayout)
            mOriginal = findViewById(R.id.original)
            mOriginalLayout?.setOnClickListener {
                var count = countOverMaxSize()
                if (count > 0) {
                    var incapableDialog = IncapableDialog.newInstance("",
                            getString(R.string.error_over_original_count, count, mSpec!!.originalMaxSize))
                    incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.simpleName)
                    return@setOnClickListener
                }

                mOriginalEnable = !mOriginalEnable
                mOriginal?.setChecked(mOriginalEnable)

                if (!mOriginalEnable) {
                    mOriginal?.setColor(Color.WHITE)
                }

                mSpec?.onCheckedListener?.onCheck(mOriginalEnable)
            }

            updateApplyButton()
            return
        }
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        var selectedCount = mSelectedCollection.count()
        for (i in 0..selectedCount) {
            var item: Item = mSelectedCollection.asList()[i]
            if (item.isImage()) {
                var size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > mSpec!!.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mSelectedCollection.onSaveInstanceState(outState)
        outState?.putBoolean("checkState", mOriginalEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        sendBackResult(false)
        super.onBackPressed()
    }

    private fun sendBackResult(apply: Boolean) {
        var intent = Intent()
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle())
        intent.putExtra(EXTRA_RESULT_APPLY, apply)
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun updateApplyButton() {
        var selectedCount = mSelectedCollection.count()

        mButtonApply?.apply {
            when (selectedCount) {
                0 -> {
                    text = getString(R.string.button_sure_default)
                    isEnabled = false
                }
                1 -> {
                    if (mSpec!!.singleSelectionModeEnabled()) {
                        text = getString(R.string.button_sure_default)
                        isEnabled = true
                    }
                }
                else -> {
                    isEnabled = true
                    text = getString(R.string.button_sure, selectedCount)
                }
            }
        }

        if (mSpec!!.originalable) {
            mOriginalLayout?.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            mOriginalLayout?.visibility = View.GONE
        }
    }

    private fun updateOriginalState() {
        mOriginal?.setChecked(mOriginalEnable)
        if (!mOriginalEnable) mOriginal?.setColor(Color.WHITE)
        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                var incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                mOriginal?.setChecked(false)
                mOriginal?.setColor(Color.WHITE)
                mOriginalEnable = false
            }

        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        var adapter = mPager?.adapter as PreviewPagerAdapter

        mCheckView?.apply {
            if (mPreviousPos != -1 && mPreviousPos != position) {
                (adapter.instantiateItem(mPager!!, mPreviousPos) as PreviewItemFragment).resetView()
                var item = adapter.getMediaItem(position)
                if (mSpec!!.countable) {
                    var checkedNum = mSelectedCollection.checkedNumOf(item)
                    setCheckedNum(checkedNum)
                    if (checkedNum > 0) {
                        setEnable(true)
                    } else {
                        setEnable(!mSelectedCollection.maxSelectableReached())
                    }
                } else {
                    var checked = mSelectedCollection.isSelected(item)
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
            mSize?.apply {
                if (isGif()) {
                    visibility = View.VISIBLE
                    text = "${PhotoMetadataUtils.getSizeInMB(size)} M"
                } else {
                    visibility = View.GONE
                }
            }

            mOriginalLayout?.apply {
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
            mButtonBack -> onBackPressed()
            mButtonApply -> {
                sendBackResult(true)
                finish()
            }
        }
    }


    private fun assertAddSelection(item: Item): Boolean {
        var cause = mSelectedCollection.isAcceptable(item)
        IncapableCause.handleCause(this, cause)
        return cause == null
    }
}