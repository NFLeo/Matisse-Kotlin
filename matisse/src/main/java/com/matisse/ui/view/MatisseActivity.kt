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

class MatisseActivity : AppCompatActivity() {
    private val mAlbumCollection = AlbumCollection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matisse)

        mAlbumCollection.onCreate(this, albumCallbacks)
        mAlbumCollection.onRestoreInstanceState(savedInstanceState!!)
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

        }
    }
}