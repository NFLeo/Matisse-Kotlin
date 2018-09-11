package com.matisse.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.matisse.internal.entity.Album
import com.matisse.loader.AlbumMediaLoader
import java.lang.ref.WeakReference

class AlbumMediaCollection : LoaderManager.LoaderCallbacks<Cursor> {

    private var mContext: WeakReference<Context>? = null
    private var mLoaderManager: LoaderManager? = null
    private var mCallbacks: AlbumCallbacks? = null

    companion object {
        val LOADER_ID = 2
        val ARGS_ALBUM = "args_album"
        val ARGS_ENABLE_CAPTURE = "args_enable_capture"
    }

    fun onCreate(context: FragmentActivity, callbacks: AlbumCallbacks) {
        mContext = WeakReference(context)
        mLoaderManager = context.supportLoaderManager
        mCallbacks = callbacks
    }

    fun onDestory() {
        mLoaderManager?.destroyLoader(LOADER_ID)
        if (mCallbacks != null) {
            mCallbacks = null
        }
    }

    fun load(target: Album) {
        load(target, false)
    }

    fun load(target: Album, enableCapture: Boolean) {
        val args = Bundle()
        args.putParcelable(ARGS_ALBUM, target)
        args.putBoolean(ARGS_ENABLE_CAPTURE, enableCapture)
        mLoaderManager?.initLoader(LOADER_ID, args, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val content = mContext?.get()

        val album = args?.getParcelable<Album>(ARGS_ALBUM)
        return AlbumMediaLoader.newInstance(content!!, album!!, album.isAll()
                && args.getBoolean(ARGS_ENABLE_CAPTURE, false))
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (mContext?.get() == null) {
            return
        }

        mCallbacks?.onAlbumLoad(data!!)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (mContext?.get() == null) {
            return
        }

        mCallbacks?.onAlbumReset()
    }


    interface AlbumMediacallback {
        fun onAlbumMediaLoad(cursor: Cursor)

        fun onAlbumMediaReset()
    }

}