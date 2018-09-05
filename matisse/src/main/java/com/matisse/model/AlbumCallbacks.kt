package com.matisse.model

import android.database.Cursor

interface AlbumCallbacks {
    fun onAlbumStart()
    fun onAlbumLoad(cursor: Cursor)
    fun onAlbumReset()
}