package com.matisse.ui.view

import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.matisse.R
import com.matisse.R.id.container
import com.matisse.entity.Item
import com.matisse.internal.entity.Album
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.utils.UIUtils

class MatisseActivity : AppCompatActivity(), MediaSelectionFragment.SelectionProvider, AlbumMediaAdapter.OnMediaClickListener {

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
    }
}