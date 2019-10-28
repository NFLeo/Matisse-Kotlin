package com.matisse.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.matisse.loader.AlbumLoader
import java.lang.ref.WeakReference

class AlbumCollection : LoaderManager.LoaderCallbacks<Cursor> {

    private var context: WeakReference<Context>? = null
    private var loaderManager: LoaderManager? = null
    private var callbacks: AlbumCallbacks? = null
    private var currentSelection = 0
    private var loadFinished = false

    companion object {
        const val LOADER_ID = 1
        const val STATE_CURRENT_SELECTION = "state_current_selection"
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = context?.get()
        loadFinished = false
        return AlbumLoader.newInstance(context!!)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (context?.get() == null || data == null) return

        if (!loadFinished) {
            loadFinished = true
            callbacks?.onAlbumLoad(data)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (context?.get() == null) return

        callbacks?.onAlbumReset()
    }

    fun onCreate(activity: FragmentActivity, callbacks: AlbumCallbacks) {
        context = WeakReference(activity)
        loaderManager = LoaderManager.getInstance(activity)
        this.callbacks = callbacks
    }

    fun onRestoreInstanceState(saveInstanceState: Bundle) {
        currentSelection = saveInstanceState.getInt(STATE_CURRENT_SELECTION)
    }

    fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(STATE_CURRENT_SELECTION, currentSelection)
    }

    fun onDestroy() {
        loaderManager?.destroyLoader(LOADER_ID)
        if (callbacks != null) callbacks = null
    }

    @Synchronized
    fun loadAlbums() {
        loadFinished = false
        loaderManager?.initLoader(LOADER_ID, null, this)
    }

    fun getCurrentSelection() = currentSelection

    fun setStateCurrentSelection(currentSelection: Int) {
        this.currentSelection = currentSelection
    }
}