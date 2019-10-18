package com.matisse.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.matisse.entity.Album
import com.matisse.loader.AlbumMediaLoader
import java.lang.ref.WeakReference

class AlbumMediaCollection : LoaderManager.LoaderCallbacks<Cursor> {

    private var context: WeakReference<Context>? = null
    private var loaderManager: LoaderManager? = null
    private var callbacks: AlbumCallbacks? = null

    companion object {
        const val LOADER_ID = 2
        const val ARGS_ALBUM = "args_album"
        const val ARGS_ENABLE_CAPTURE = "args_enable_capture"
    }

    fun onCreate(context: FragmentActivity, callbacks: AlbumCallbacks) {
        this.context = WeakReference(context)
        loaderManager = LoaderManager.getInstance(context)
        this.callbacks = callbacks
    }

    fun onDestroy() {
        loaderManager?.destroyLoader(LOADER_ID)
        if (callbacks != null) callbacks = null
    }

    fun load(target: Album) {
        load(target, false)
    }

    fun load(target: Album, enableCapture: Boolean) {
        val args = Bundle()
        args.putParcelable(ARGS_ALBUM, target)
        args.putBoolean(ARGS_ENABLE_CAPTURE, enableCapture)
        loaderManager?.initLoader(LOADER_ID, args, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val content = context?.get()

        val album = args?.getParcelable<Album>(ARGS_ALBUM)
        return AlbumMediaLoader.newInstance(
            content!!, album!!, album.isAll()
                    && args.getBoolean(ARGS_ENABLE_CAPTURE, false)
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (context?.get() == null) return

        callbacks?.onAlbumLoad(data!!)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (context?.get() == null) return

        callbacks?.onAlbumReset()
    }
}