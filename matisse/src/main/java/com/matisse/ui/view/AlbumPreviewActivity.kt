package com.matisse.ui.view

import android.database.Cursor
import android.os.Bundle
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumMediaCollection
import com.matisse.ui.adapter.PreviewPagerAdapter
import kotlinx.android.synthetic.main.activity_media_preview.*

/**
 * Created by liubo on 2018/9/11.
 */
class AlbumPreviewActivity : BasePreviewActivity(), AlbumCallbacks {

    var mCollection: AlbumMediaCollection = AlbumMediaCollection()
    var mIsAlreadySetPosition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        mCollection.onCreate(this, this)
        val album = intent.getParcelableExtra<Album>(ConstValue.EXTRA_ALBUM)
        mCollection.load(album)
        val item = intent.getParcelableExtra<Item>(ConstValue.EXTRA_ITEM)
        check_view?.apply {
            if (mSpec?.countable!!) {
                setCheckedNum(mSelectedCollection.checkedNumOf(item))
            } else {
                setChecked(mSelectedCollection.isSelected(item))
            }
        }
        updateSize(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCollection.onDestory()
    }

    override fun onAlbumLoad(cursor: Cursor) {
        val items = ArrayList<Item>()
        while (cursor.moveToNext()) {
            items.add(Item.valueOf(cursor))
        }

        if (items.isEmpty()) {
            return
        }
        val adapter = pager?.adapter as PreviewPagerAdapter
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
        if (!mIsAlreadySetPosition) {
            mIsAlreadySetPosition = true
            val selected = intent.getParcelableExtra<Item>(ConstValue.EXTRA_ITEM)
            val selectedIndex = items.indexOf(selected)
            pager?.setCurrentItem(selectedIndex, false)
            mPreviousPos = selectedIndex
        }
    }

    override fun onAlbumReset() {
    }

    override fun onAlbumStart() {
    }
}