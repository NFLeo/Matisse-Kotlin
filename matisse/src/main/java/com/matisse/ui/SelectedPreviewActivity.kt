package com.matisse.ui

import android.app.Activity
import android.os.Bundle
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.view.BasePreviewActivity

/**
 * Created by liubo on 2018/9/11.
 */
class SelectedPreviewActivity : BasePreviewActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        var bundle = intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE)
        var selected = bundle.getParcelableArrayList<Item>(SelectedItemCollection.STATE_SELECTION)
        mAdapter?.addAll(selected)
        mAdapter?.notifyDataSetChanged()
        mCheckView?.apply {
            if (mSpec!!.countable) {
                setCheckedNum(1)
            } else {
                setChecked(true)
            }
        }
        mPreviousPos = 0
        updateSize(selected[0])
    }
}