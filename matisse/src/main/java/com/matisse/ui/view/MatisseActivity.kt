package com.matisse.ui.view

import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.matisse.R
import com.matisse.internal.entity.Album
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection
import com.matisse.utils.UIUtils
import kotlinx.android.synthetic.main.activity_matisse.*

class MatisseActivity : AppCompatActivity() {
    private val mAlbumCollection = AlbumCollection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matisse)

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
                val album = Album.valueOf(cursor)
                onAlbumSelected(album)
            }
        }

        override fun onAlbumReset() {
        }
    }

    private fun onAlbumSelected(album: Album) {
        if (!album.isEmpty()) {
            UIUtils.setViewVisible(true, container)
            val fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager.beginTransaction()
                    .replace(container.id, fragment, MediaSelectionFragment::class.java.simpleName)
                    .commitAllowingStateLoss()
        }
    }
}