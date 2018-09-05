package com.matisse.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.matisse.loader.AlbumLoader
import java.lang.ref.WeakReference

class AlbumCollection : LoaderManager.LoaderCallbacks<Cursor> {

    private var mContext: WeakReference<Context>? = null
    private var mLoaderManager: LoaderManager? = null
    private var mCallbacks: AlbumCallbacks? = null
    private var mCurrentSelection: Int = 0
    private var mLoadFinished: Boolean = false

    companion object {
        val LOADER_ID = 1
        val STATE_CURRENT_SELECTION = "state_current_selection"
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = mContext?.get()
        mLoadFinished = false
        return AlbumLoader.newInstance(context!!)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (mContext?.get() == null) {
            return
        }

        if (!mLoadFinished) {
            mLoadFinished = true
            mCallbacks?.onAlbumLoad(data!!)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (mContext?.get() == null) {
            return
        }

        mCallbacks?.onAlbumReset()
    }

    fun onCreate(activity: FragmentActivity, callbacks: AlbumCallbacks) {
        mContext = WeakReference(activity)
        mLoaderManager = activity.supportLoaderManager
        mCallbacks = callbacks
    }

    fun onRestoreInstanceState(saveInstanceState: Bundle) {
        mCurrentSelection = saveInstanceState.getInt(STATE_CURRENT_SELECTION)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_SELECTION, mCurrentSelection)
    }

    fun onDestory() {
        mLoaderManager?.destroyLoader(LOADER_ID)
        if (mCallbacks != null) {
            mCallbacks = null
        }
    }

    fun loadAlbums() {
        mLoaderManager?.initLoader(LOADER_ID, null, this)
    }

    fun getCurrentSelection() = mCurrentSelection

    fun setStateCurrentSelection(currentSelection: Int) {
        mCurrentSelection = currentSelection
    }
}