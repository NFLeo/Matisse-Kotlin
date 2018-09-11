package com.matisse.ui.view

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.matisse.R
import com.matisse.R.id.container
import com.matisse.entity.ConstValue
import com.matisse.entity.Item
import com.matisse.internal.entity.Album
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.utils.MediaStoreCompat
import com.matisse.utils.PathUtils
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.UIUtils
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.include_view_bottom.*

class MatisseActivity : AppCompatActivity(), MediaSelectionFragment.SelectionProvider, AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener, View.OnClickListener {
    companion object {

        val EXTRA_RESULT_SELECTION = "extra_result_selection"
        val EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path"
        val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
        private val REQUEST_CODE_PREVIEW = 23
        private val REQUEST_CODE_CAPTURE = 24
        val CHECK_STATE = "checkState"

    }

    private var mMediaStoreCompat: MediaStoreCompat? = null
    private var mSpec: SelectionSpec? = null
    private var mOriginalEnable: Boolean = false
    private val mAlbumCollection = AlbumCollection()
    private val mSelectedCollection = SelectedItemCollection(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matisse)

        mSpec = SelectionSpec.getInstance()

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

    override fun onUpdate() {
        updateBottomToolbar()
//
        if (mSpec!!.onSelectedListener != null) {
            mSpec!!.onSelectedListener?.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
        }
    }

    private fun updateBottomToolbar() {

        val selectedCount = mSelectedCollection.count()
        if (selectedCount == 0) {
            button_preview.isEnabled = false
            button_apply.isEnabled = false
            button_apply.text = getString(R.string.button_sure_default)
        } else if (selectedCount == 1 && mSpec!!.singleSelectionModeEnabled()) {
            button_preview.isEnabled = true
            button_apply.setText(R.string.button_sure_default)
            button_apply.isEnabled = true
        } else {
            button_preview.isEnabled = true
            button_apply.isEnabled = true
            button_apply.text = getString(R.string.button_sure, selectedCount)
        }


        if (mSpec!!.originalable) {
            originalLayout.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            originalLayout.visibility = View.INVISIBLE
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return

        if (requestCode == REQUEST_CODE_PREVIEW) {
            val resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE)
            val selected = resultBundle.getParcelableArrayList<Item>(SelectedItemCollection.STATE_SELECTION)
            mOriginalEnable = data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false)
            val collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED)
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                val result = Intent()
                val selectedUris = ArrayList<Uri>()
                val selectedPaths = ArrayList<String>()
                if (selected != null) {
                    for (item in selected) {
                        selectedUris.add(item.getContentUri())
                        selectedPaths.add(PathUtils.getPath(this, item.getContentUri())!!)
                    }
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                mSelectedCollection.overwrite(selected, collectionType)
                val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                        MediaSelectionFragment::class.java!!.getSimpleName())
                if (mediaSelectionFragment is MediaSelectionFragment) {
                    mediaSelectionFragment.refreshMediaGrid()
                }
                updateBottomToolbar()
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            val contentUri = mMediaStoreCompat!!.getCurrentPhotoUri()
            val path = mMediaStoreCompat!!.getCurrentPhotoPath()
            val selected = ArrayList<Uri>()
            selected.add(contentUri!!)
            val selectedPath = ArrayList<String>()
            selectedPath.add(path!!)
            val result = Intent()
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected)
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath)
            setResult(Activity.RESULT_OK, result)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                this@MatisseActivity.revokeUriPermission(contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            finish()
        }
    }

    private fun updateOriginalState() {
        original!!.setChecked(mOriginalEnable)
        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager,
                        IncapableDialog::class.java!!.getName())
                original!!.setChecked(false)
                mOriginalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = mSelectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = mSelectedCollection.asList()[i]

            if (item.isImage()) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > mSpec!!.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    override fun provideSelectedItemCollection() = mSelectedCollection

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {

        val intentCrop = Intent(this, ImageCropActivity::class.java)
        intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, PathUtils.getPath(this, item.getContentUri()))
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