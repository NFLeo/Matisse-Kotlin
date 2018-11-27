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
import com.gyf.barlibrary.ImmersionBar
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.entity.ConstValue.EXTRA_RESULT_SELECTION
import com.matisse.entity.ConstValue.EXTRA_RESULT_SELECTION_PATH
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.ui.adapter.FolderMediaAdapter
import com.matisse.utils.*
import com.matisse.widget.IncapableDialog
import kotlinx.android.synthetic.main.activity_matisse.*
import kotlinx.android.synthetic.main.include_view_bottom.*
import kotlinx.android.synthetic.main.include_view_navigation.*
import java.util.*

class MatisseActivity : AppCompatActivity(), MediaSelectionFragment.SelectionProvider,
        AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
        AlbumMediaAdapter.OnPhotoCapture, View.OnClickListener {

    private var mMediaStoreCompat: MediaStoreCompat? = null
    private var mSpec: SelectionSpec? = null
    private var mOriginalEnable: Boolean = false
    private val mAlbumCollection = AlbumCollection()
    private val mSelectedCollection = SelectedItemCollection(this)

    private var mCursor: Cursor? = null
    private var bottomSheet: FolderBottomSheet? = null
    private var mLastFolderCheckedPosition: Int = 0
    private lateinit var allAlbum: Album

    private var mImmersionBar:ImmersionBar? = null

    private var albumCallbacks = object : AlbumCallbacks {
        override fun onAlbumStart() {
        }

        override fun onAlbumLoad(cursor: Cursor) {
            mCursor = cursor

            Handler(Looper.getMainLooper()).post {
                if (cursor.moveToFirst()) {
                    allAlbum = Album.valueOf(cursor)
                    onAlbumSelected(allAlbum)
                }
            }
        }

        override fun onAlbumReset() {
            if (bottomSheet != null && bottomSheet?.mAdapter != null) {
                mCursor = null
                bottomSheet?.mAdapter?.swapCursor(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mSpec = SelectionSpec.getInstance()
        setTheme(mSpec?.themeId!!)
        super.onCreate(savedInstanceState)

        if (!mSpec?.hasInited!!) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContentView(R.layout.activity_matisse)

        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            mImmersionBar = ImmersionBar.with(this)
            mImmersionBar?.titleBar(toolbar)?.statusBarDarkFont(mSpec?.isDarkStatus == true)?.init()
        }
        initConfigs(savedInstanceState)
        initListener()
    }

    private fun initConfigs(savedInstanceState: Bundle?) {
        if (mSpec?.needOrientationRestriction()!!) {
            requestedOrientation = mSpec?.orientation!!
        }

        if (mSpec?.capture!!) {
            mMediaStoreCompat = MediaStoreCompat(this)
            if (mSpec?.captureStrategy == null)
                throw RuntimeException("Don't forget to set CaptureStrategy.")
            mMediaStoreCompat?.setCaptureStrategy(mSpec?.captureStrategy!!)
        }

        mSelectedCollection.onCreate(savedInstanceState)
        mAlbumCollection.onCreate(this, albumCallbacks)
        if (savedInstanceState != null) {
            mAlbumCollection.onRestoreInstanceState(savedInstanceState)
        }
        mAlbumCollection.loadAlbums()
        updateBottomToolbar()
    }

    private fun initListener() {
        button_apply.setText(R.string.album_name_all)
        button_apply.setOnClickListener(this)
        button_preview.setOnClickListener(this)
        original_layout.setOnClickListener(this)
        button_complete.setOnClickListener(this)
        button_back.setOnClickListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mSelectedCollection.onSaveInstanceState(outState!!)
        mAlbumCollection.onSaveInstanceState(outState)
        outState.putBoolean(ConstValue.CHECK_STATE, mOriginalEnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mImmersionBar?.destroy()
        mAlbumCollection.onDestory()
        mSpec?.onCheckedListener = null
        mSpec?.onSelectedListener = null
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onUpdate() {
        updateBottomToolbar()
        if (mSpec!!.onSelectedListener != null) {
            mSpec!!.onSelectedListener?.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
        }
    }

    override fun capture() {
        if (mMediaStoreCompat != null) {
            mMediaStoreCompat?.dispatchCaptureIntent(this, ConstValue.REQUEST_CODE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return

        var cropPath: String? = null
        if (data != null) {
            cropPath = data.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE)
        }

        when (requestCode) {
            ConstValue.REQUEST_CODE_PREVIEW -> {

                if (!cropPath.isNullOrEmpty()) {
                    // 裁剪带回数据，则认为图片经过裁剪流程
                    returnCropData(cropPath!!)
                    return
                }

                val resultBundle = data?.getBundleExtra(ConstValue.EXTRA_RESULT_BUNDLE)
                val selected = resultBundle?.getParcelableArrayList<Item>(ConstValue.STATE_SELECTION)
                mOriginalEnable = data?.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false) ?: false
                val collectionType = resultBundle?.getInt(ConstValue.STATE_COLLECTION_TYPE,
                        SelectedItemCollection.COLLECTION_UNDEFINED)
                if (data?.getBooleanExtra(ConstValue.EXTRA_RESULT_APPLY, false) == true) {
                    val result = Intent()
                    val selectedUris = ArrayList<Uri>()
                    val selectedPaths = ArrayList<String>()
                    if (selected != null) {
                        for (item in selected) {
                            selectedUris.add(item.getContentUri())
                            selectedPaths.add(PathUtils.getPath(this, item.getContentUri())!!)
                        }
                    }
                    result.putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selectedUris)
                    result.putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                    result.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
                    setResult(Activity.RESULT_OK, result)
                    finish()
                } else {
                    mSelectedCollection.overwrite(selected!!, collectionType!!)
                    val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                            MediaSelectionFragment::class.java.simpleName)
                    if (mediaSelectionFragment is MediaSelectionFragment) {
                        mediaSelectionFragment.refreshMediaGrid()
                    }
                    updateBottomToolbar()
                }
            }

            ConstValue.REQUEST_CODE_CAPTURE -> {
                val contentUri = mMediaStoreCompat!!.getCurrentPhotoUri()
                val path = mMediaStoreCompat!!.getCurrentPhotoPath()
                val selected = ArrayList<Uri>()
                selected.add(contentUri!!)
                val selectedPath = ArrayList<String>()
                selectedPath.add(path!!)

                // Check is Crop first
                if (mSpec?.openCrop() == true) {
                    val intentCrop = Intent(this, ImageCropActivity::class.java)
                    intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPath[0])
                    startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
                    return
                }

                val result = Intent()
                result.putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selected)
                result.putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPath)
                setResult(Activity.RESULT_OK, result)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    this@MatisseActivity.revokeUriPermission(contentUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                finish()
            }
            ConstValue.REQUEST_CODE_CROP -> {
                returnCropData(cropPath!!)
            }
        }
    }

    private fun returnCropData(cropPath: String) {
        val result = Intent()
        val selectedUris = ArrayList<Uri>()
        val selectedPaths = ArrayList<String>()
        selectedPaths.add(cropPath)
        result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
        result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun provideSelectedItemCollection() = mSelectedCollection

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {

        val intent = Intent(this, AlbumPreviewActivity::class.java)
        intent.putExtra(ConstValue.EXTRA_ALBUM, album)
        intent.putExtra(ConstValue.EXTRA_ITEM, item)
        intent.putExtra(ConstValue.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle())
        intent.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
        startActivityForResult(intent, ConstValue.REQUEST_CODE_PREVIEW)
    }

    override fun onClick(v: View?) {
        when (v) {
            button_back -> {
                onBackPressed()
            }

            button_preview -> {
                SelectedPreviewActivity.instance(this@MatisseActivity, mSelectedCollection.getDataWithBundle(), mOriginalEnable)
            }

            button_complete -> {
                val selectedUris = mSelectedCollection.asListOfUri() as ArrayList<Uri>
                val selectedPaths = mSelectedCollection.asListOfString() as ArrayList<String>

                val item = if (mSelectedCollection.asList().isEmpty()) null else mSelectedCollection.asList()[0]

                if (mSpec?.openCrop() == true && mSpec?.isSupportCrop(item) == true) {
                    val intentCrop = Intent(this, ImageCropActivity::class.java)
                    intentCrop.putExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths[0])
                    startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
                    return
                }

                val result = Intent()
                result.putParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION, selectedUris)
                result.putStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                result.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
                setResult(Activity.RESULT_OK, result)
                finish()
            }

            original_layout -> {
                val count = countOverMaxSize()
                if (count > 0) {
                    val incapableDialog = IncapableDialog.newInstance("",
                            getString(R.string.error_over_original_count, count, mSpec?.originalMaxSize))
                    incapableDialog.show(supportFragmentManager,
                            IncapableDialog::class.java.name)
                    return
                }

                mOriginalEnable = !mOriginalEnable
                original.setChecked(mOriginalEnable)

                if (mSpec?.onCheckedListener != null) {
                    mSpec?.onCheckedListener!!.onCheck(mOriginalEnable)
                }
            }

            button_apply -> {
                if (allAlbum.isAll() && allAlbum.isEmpty()) {
                    return
                }

                bottomSheet = FolderBottomSheet.instance(this@MatisseActivity, mLastFolderCheckedPosition, "Folder")
                bottomSheet?.callback = object : FolderBottomSheet.BottomSheetCallback {
                    override fun onItemClick(cursor: Cursor, position: Int) {
                        mLastFolderCheckedPosition = position

                        mAlbumCollection.setStateCurrentSelection(position)
                        cursor.moveToPosition(position)
                        val album = Album.valueOf(cursor)

                        button_apply.text = album.getDisplayName(this@MatisseActivity)
                        if (album.isAll() && SelectionSpec.getInstance().capture) {
                            album.addCaptureCount()
                        }
                        onAlbumSelected(album)
                    }

                    override fun initData(adapter: FolderMediaAdapter) {
                        adapter.swapCursor(mCursor)
                    }
                }
            }
        }
    }

    private fun updateBottomToolbar() {

        val selectedCount = mSelectedCollection.count()
        if (selectedCount == 0) {
            button_preview.isEnabled = false
            button_complete.isEnabled = false
            button_complete.text = getString(R.string.button_complete)
        } else if (selectedCount == 1 && mSpec!!.singleSelectionModeEnabled()) {
            button_preview.isEnabled = true
            button_complete.setText(R.string.button_complete)
            button_complete.isEnabled = true
        } else {
            button_preview.isEnabled = true
            button_complete.isEnabled = true
            button_complete.text = getString(R.string.button_sure, selectedCount)
        }

        if (mSpec!!.originalable) {
            original_layout.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            original_layout.visibility = View.INVISIBLE
        }
    }

    private fun updateOriginalState() {
        original!!.setChecked(mOriginalEnable)
        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
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

    private fun onAlbumSelected(album: Album) {
        if (album.isAll() && album.isEmpty()) {
            UIUtils.setViewVisible(true, empty_view)
            UIUtils.setViewVisible(false, container)
        } else {
            UIUtils.setViewVisible(false, empty_view)
            UIUtils.setViewVisible(true, container)
            val fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager.beginTransaction()
                    .replace(container.id, fragment, MediaSelectionFragment::class.java.simpleName)
                    .commitAllowingStateLoss()
        }
    }
}