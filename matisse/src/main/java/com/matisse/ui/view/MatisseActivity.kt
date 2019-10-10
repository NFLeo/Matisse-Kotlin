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
import android.text.TextUtils
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

    private var mediaStoreCompat: MediaStoreCompat? = null
    private var spec: SelectionSpec? = null
    private var originalEnable = false
    private lateinit var albumCollection: AlbumCollection
    private lateinit var selectedCollection: SelectedItemCollection

    private var cursor: Cursor? = null
    private var bottomSheet: FolderBottomSheet? = null
    private var lastFolderCheckedPosition = 0
    private lateinit var allAlbum: Album


    override fun onCreate(savedInstanceState: Bundle?) {
        spec = SelectionSpec.getInstance()
        setTheme(spec?.themeId!!)
        super.onCreate(savedInstanceState)

        if (!spec?.hasInited!!) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContentView(R.layout.activity_matisse)

        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).titleBar(toolbar)
                ?.statusBarDarkFont(spec?.isDarkStatus == true)?.init()
        }
        initConfigs(savedInstanceState)
        initListener()
    }

    private fun initConfigs(savedInstanceState: Bundle?) {
        if (spec?.needOrientationRestriction() == true) {
            requestedOrientation = spec?.orientation!!
        }

        albumCollection = AlbumCollection()
        selectedCollection = SelectedItemCollection(this)

        if (spec?.capture == true) {
            mediaStoreCompat = MediaStoreCompat(this)
            if (spec?.captureStrategy == null)
                throw RuntimeException("Don't forget to set CaptureStrategy.")
            mediaStoreCompat?.setCaptureStrategy(spec?.captureStrategy!!)
        }

        selectedCollection.onCreate(savedInstanceState)
        albumCollection.onCreate(this, object : AlbumCallbacks {
            override fun onAlbumStart() {
                // do nothing
            }

            override fun onAlbumLoad(cursor: Cursor) {
                this@MatisseActivity.cursor = cursor

                Handler(Looper.getMainLooper()).post {
                    if (cursor.moveToFirst()) {
                        allAlbum = Album.valueOf(cursor)
                        onAlbumSelected(allAlbum)
                    }
                }
            }

            override fun onAlbumReset() {
                if (bottomSheet != null && bottomSheet?.adapter != null) {
                    cursor = null
                    bottomSheet?.adapter?.swapCursor(null)
                }
            }
        })
        if (savedInstanceState != null)
            albumCollection.onRestoreInstanceState(savedInstanceState)

        albumCollection.loadAlbums()
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
        selectedCollection.onSaveInstanceState(outState!!)
        albumCollection.onSaveInstanceState(outState)
        outState.putBoolean(ConstValue.CHECK_STATE, originalEnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Platform.isClassExists("com.gyf.barlibrary.ImmersionBar")) {
            ImmersionBar.with(this).destroy()
        }
        albumCollection.onDestroy()
        spec?.onCheckedListener = null
        spec?.onSelectedListener = null
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onUpdate() {
        updateBottomToolbar()
        if (spec!!.onSelectedListener != null) {
            spec!!.onSelectedListener?.onSelected(
                selectedCollection.asListOfUri(), selectedCollection.asListOfString()
            )
        }
    }

    override fun capture() {
        if (mediaStoreCompat != null) {
            mediaStoreCompat?.dispatchCaptureIntent(this, ConstValue.REQUEST_CODE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        var cropPath: String? = null
        if (data != null)
            cropPath = data.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE)

        when (requestCode) {
            ConstValue.REQUEST_CODE_PREVIEW -> {

                if (!cropPath.isNullOrEmpty()) {
                    // 裁剪带回数据，则认为图片经过裁剪流程
                    returnCropData(cropPath!!)
                    return
                }

                val resultBundle = data?.getBundleExtra(ConstValue.EXTRA_RESULT_BUNDLE)
                val selected =
                    resultBundle?.getParcelableArrayList<Item>(ConstValue.STATE_SELECTION)
                originalEnable =
                    data?.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false) ?: false
                val collectionType = resultBundle?.getInt(
                    ConstValue.STATE_COLLECTION_TYPE, SelectedItemCollection.COLLECTION_UNDEFINED
                )
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
                    result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                    result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                    result.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
                    setResult(Activity.RESULT_OK, result)
                    finish()
                } else {
                    selectedCollection.overwrite(selected!!, collectionType!!)
                    val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                        MediaSelectionFragment::class.java.simpleName
                    )
                    if (mediaSelectionFragment is MediaSelectionFragment) {
                        mediaSelectionFragment.refreshMediaGrid()
                    }
                    updateBottomToolbar()
                }
            }

            ConstValue.REQUEST_CODE_CAPTURE -> {
                val contentUri = mediaStoreCompat!!.getCurrentPhotoUri()
                val path = mediaStoreCompat!!.getCurrentPhotoPath()
                val selected = ArrayList<Uri>()
                selected.add(contentUri!!)
                val selectedPath = ArrayList<String>()
                selectedPath.add(path!!)

                // Check is Crop first
                if (spec?.openCrop() == true) {
                    val intentCrop = Intent(this, ImageCropActivity::class.java)
                    intentCrop.putExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath[0])
                    startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
                    return
                }

                val result = Intent()
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected)
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath)
                setResult(Activity.RESULT_OK, result)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    this@MatisseActivity.revokeUriPermission(
                        contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                finish()
            }
            ConstValue.REQUEST_CODE_CROP -> {
                returnCropData(cropPath)
            }
        }
    }

    private fun returnCropData(cropPath: String?) {
        val result = Intent()
        val selectedUris = ArrayList<Uri>()
        val selectedPaths = ArrayList<String>()
        selectedPaths.add(cropPath ?: "")
        result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
        result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun provideSelectedItemCollection() = selectedCollection

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        val intent = Intent(this, AlbumPreviewActivity::class.java)
        intent.putExtra(ConstValue.EXTRA_ALBUM, album)
        intent.putExtra(ConstValue.EXTRA_ITEM, item)
        intent.putExtra(ConstValue.EXTRA_DEFAULT_BUNDLE, selectedCollection.getDataWithBundle())
        intent.putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        startActivityForResult(intent, ConstValue.REQUEST_CODE_PREVIEW)
    }

    override fun onClick(v: View?) {
        when (v) {
            button_back -> onBackPressed()
            button_preview -> {
                SelectedPreviewActivity.instance(
                    this@MatisseActivity, selectedCollection.getDataWithBundle(), originalEnable
                )
            }
            button_complete -> {
                val selectedUris = selectedCollection.asListOfUri() as ArrayList<Uri>
                val selectedPaths = selectedCollection.asListOfString() as ArrayList<String>

                val item =
                    if (selectedCollection.asList().isEmpty()) null else selectedCollection.asList()[0]

                if (spec?.openCrop() == true && spec?.isSupportCrop(item) == true) {
                    val intentCrop = Intent(this, ImageCropActivity::class.java)
                    intentCrop.putExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths[0])
                    startActivityForResult(intentCrop, ConstValue.REQUEST_CODE_CROP)
                    return
                }

                val result =
                    Intent().putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                        .putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                        .putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
                setResult(Activity.RESULT_OK, result)
                finish()
            }

            original_layout -> {
                val count = countOverMaxSize()
                if (count > 0) {
                    val incapableDialog = IncapableDialog.newInstance(
                        "",
                        getString(R.string.error_over_original_count, count, spec?.originalMaxSize)
                    )
                    incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                    return
                }

                originalEnable = !originalEnable
                original.setChecked(originalEnable)

                if (spec?.onCheckedListener != null)
                    spec?.onCheckedListener!!.onCheck(originalEnable)
            }

            button_apply -> {
                if (allAlbum.isAll() && allAlbum.isEmpty()) return

                bottomSheet = FolderBottomSheet.instance(
                    this@MatisseActivity, lastFolderCheckedPosition, "Folder"
                )
                bottomSheet?.callback = object : FolderBottomSheet.BottomSheetCallback {
                    override fun onItemClick(cursor: Cursor, position: Int) {
                        lastFolderCheckedPosition = position

                        albumCollection.setStateCurrentSelection(position)
                        cursor.moveToPosition(position)
                        val album = Album.valueOf(cursor)

                        button_apply.text = album.getDisplayName(this@MatisseActivity)
                        if (album.isAll() && SelectionSpec.getInstance().capture) {
                            album.addCaptureCount()
                        }
                        onAlbumSelected(album)
                    }

                    override fun initData(adapter: FolderMediaAdapter) {
                        adapter.swapCursor(cursor)
                    }
                }
            }
        }
    }

    private fun updateBottomToolbar() {

        val selectedCount = selectedCollection.count()
        if (selectedCount == 0) {
            button_preview.isEnabled = false
            button_complete.isEnabled = false
            button_complete.text = getString(R.string.button_complete)
        } else if (selectedCount == 1 && spec!!.singleSelectionModeEnabled()) {
            button_preview.isEnabled = true
            button_complete.setText(R.string.button_complete)
            button_complete.isEnabled = true
        } else {
            button_preview.isEnabled = true
            button_complete.isEnabled = true
            button_complete.text = getString(R.string.button_sure, selectedCount)
        }

        if (spec!!.originalable) {
            original_layout.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            original_layout.visibility = View.INVISIBLE
        }
    }

    private fun updateOriginalState() {
        original!!.setChecked(originalEnable)
        if (countOverMaxSize() > 0) {
            if (originalEnable) {
                val incapableDialog = IncapableDialog.newInstance(
                    "",
                    getString(R.string.error_over_original_size, spec!!.originalMaxSize)
                )
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                original!!.setChecked(false)
                originalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = selectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = selectedCollection.asList()[i]

            if (item.isImage()) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > spec!!.originalMaxSize) {
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