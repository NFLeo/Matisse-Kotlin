package com.matisse.ui.view

import android.database.Cursor
import android.os.Bundle
import com.matisse.entity.Item
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.entity.ConstValue.EXTRA_ALBUM
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumMediaCollection
import com.matisse.ui.adapter.PreviewPagerAdapter

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
        var album = intent.getParcelableExtra<Album>(EXTRA_ALBUM)
        mCollection.load(album)
        var item = intent.getParcelableExtra<Item>(ConstValue.EXTRA_ITEM)
        mCheckView?.apply {
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
        var items = ArrayList<Item>()
        while (cursor.moveToNext()) {
            items.add(Item.valueOf(cursor))
        }

        if (items.isEmpty()) {
            return
        }
        var adapter = mPager?.adapter as PreviewPagerAdapter
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
        if (!mIsAlreadySetPosition) {
            mIsAlreadySetPosition = true
            var selected = intent.getParcelableExtra<Item>(ConstValue.EXTRA_ITEM)
            var selectedIndex = items.indexOf(selected)
            mPager?.setCurrentItem(selectedIndex, false)
            mPreviousPos = selectedIndex
        }
    }

    override fun onAlbumReset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAlbumStart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}