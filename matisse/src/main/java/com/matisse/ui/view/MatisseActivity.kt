package com.matisse.ui.view

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.matisse.R
import com.matisse.R.id.container
import com.matisse.entity.ConstValue
import com.matisse.entity.ConstValue.EXTRA_RESULT_SELECTION_PATH
import com.matisse.entity.Item
import com.matisse.internal.entity.Album
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.utils.PathUtils
import com.matisse.utils.UIUtils
import kotlinx.android.synthetic.main.include_view_bottom.*

class MatisseActivity : AppCompatActivity(), MediaSelectionFragment.SelectionProvider, AlbumMediaAdapter.OnMediaClickListener, View.OnClickListener {

    private val mAlbumCollection = AlbumCollection()
    private val mSelectedCollection = SelectedItemCollection(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matisse)

        mSelectedCollection.onCreate(savedInstanceState)

        mAlbumCollection.onCreate(this, albumCallbacks)
        if (savedInstanceState != null) {
            mAlbumCollection.onRestoreInstanceState(savedInstanceState)
        }

        mAlbumCollection.loadAlbums()

        button_apply.setOnClickListener(this)
    }

    private var albumCallbacks = object : AlbumCallbacks {
        override fun onAlbumStart() {
        }

        override fun onAlbumLoad(cursor: Cursor) {
            Handler(Looper.getMainLooper()).post {
                if (cursor.moveToFirst()) {
                    val album = Album.valueOf(cursor)
                    onAlbumSelected(album)
                }
            }
        }

        override fun onAlbumReset() {
        }
    }

    private fun onAlbumSelected(album: Album) {
        if (!album.isEmpty()) {
            UIUtils.setViewVisible(true, findViewById(container))
            val fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager.beginTransaction()
                    .replace(container, fragment, MediaSelectionFragment::class.java.simpleName)
                    .commitAllowingStateLoss()
        }
    }

    override fun provideSelectedItemCollection() = mSelectedCollection

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {

        val intentCrop = Intent(this, ImageCropActivity::class.java)
        intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, PathUtils.getPath(this, item.getContentUri()!!))
        startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
    }

    override fun onClick(v: View?) {
        val selectedUris = mSelectedCollection.asListOfUri()
        val selectedPaths = mSelectedCollection.asListOfString()

        if (mSelectedCollection.asList()[0].isImage()) {
            val intentCrop = Intent(this, ImageCropActivity::class.java)
            intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths[0])
            startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
        }
    }
}