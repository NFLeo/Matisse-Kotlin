package com.matisse.ui.activity.matisse

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.model.AlbumCallbacks
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.activity.AlbumPreviewActivity
import com.matisse.ui.activity.BaseActivity
import com.matisse.ui.activity.SelectedPreviewActivity
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.ui.adapter.FolderItemMediaAdapter
import com.matisse.ui.view.FolderBottomSheet
import com.matisse.ui.view.MediaSelectionFragment
import com.matisse.utils.*
import kotlinx.android.synthetic.main.activity_matisse.*
import kotlinx.android.synthetic.main.include_view_bottom.*
import kotlinx.android.synthetic.main.include_view_navigation.*

/**
 * desc：入口</br>
 * time: 2019/9/11-14:17</br>
 * author：Leo </br>
 * since V 1.0.0 </br>
 */
class MatisseActivity : BaseActivity(),
    MediaSelectionFragment.SelectionProvider,
    AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
    AlbumMediaAdapter.OnPhotoCapture, View.OnClickListener {

    private var mediaStoreCompat: MediaStoreCompat? = null
    private var originalEnable = false
    private var allAlbum: Album? = null
    private lateinit var selectedCollection: SelectedItemCollection
    private lateinit var albumFolderSheetHelper: AlbumFolderSheetHelper
    private lateinit var albumLoadHelper: AlbumLoadHelper


    override fun configActivity() {
        super.configActivity()
        spec?.statusBarFuture?.accept(this, toolbar)

        if (spec?.capture == true) {
            mediaStoreCompat = MediaStoreCompat(this)
            if (spec?.captureStrategy == null)
                throw RuntimeException("Don't forget to set CaptureStrategy.")
            mediaStoreCompat?.setCaptureStrategy(spec?.captureStrategy!!)
        }
    }

    override fun getResourceLayoutId() = R.layout.activity_matisse

    override fun setViewData() {
        button_apply.setText(getAttrString(R.attr.Media_Album_text, R.string.album_name_all))
        selectedCollection = SelectedItemCollection(this).apply { onCreate(instanceState) }
        albumLoadHelper = AlbumLoadHelper(this, albumCallback)
        albumFolderSheetHelper = AlbumFolderSheetHelper(this, albumSheetCallback)
        updateBottomToolbar()
    }

    override fun initListener() {
        UIUtils.setOnClickListener(
            this, button_apply, button_preview,
            original_layout, button_complete, button_back
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedCollection.onSaveInstanceState(outState)
        albumLoadHelper.onSaveInstanceState(outState)
        outState.putBoolean(ConstValue.CHECK_STATE, originalEnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        albumLoadHelper.onDestroy()
        spec?.onCheckedListener = null
        spec?.onSelectedListener = null
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onSelectUpdate() {
        updateBottomToolbar()
        spec?.onSelectedListener?.onSelected(
            selectedCollection.asListOfUri(), selectedCollection.asListOfString()
        )
    }

    override fun capture() {
        mediaStoreCompat?.dispatchCaptureIntent(this, ConstValue.REQUEST_CODE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val cropPath = data?.getStringExtra(ConstValue.EXTRA_RESULT_BUNDLE) ?: ""

        when (requestCode) {
            ConstValue.REQUEST_CODE_PREVIEW -> {
                // 裁剪带回数据，则认为图片经过裁剪流程
                if (cropPath.isNotEmpty()) doActivityResultFromCrop(cropPath)
                else doActivityResultFromPreview(data)
            }
            ConstValue.REQUEST_CODE_CAPTURE -> doActivityResultFromCapture()
            ConstValue.REQUEST_CODE_CROP -> doActivityResultFromCrop(cropPath)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            button_back -> onBackPressed()
            button_preview -> {
                if (selectedCollection.count() == 0) {
                    UIUtils.handleCause(
                        activity, IncapableCause(getString(R.string.please_select_media_resource))
                    )
                    return
                }

                SelectedPreviewActivity.instance(
                    activity, selectedCollection.getDataWithBundle(), originalEnable
                )
            }
            button_complete -> {
                if (selectedCollection.count() == 0) {
                    UIUtils.handleCause(
                        activity, IncapableCause(getString(R.string.please_select_media_resource))
                    )
                    return
                }

                val item = selectedCollection.asList()[0]
                if (spec?.openCrop() == true && spec?.isSupportCrop(item) == true) {
                    gotoImageCrop(this, selectedCollection.asListOfString() as ArrayList<String>)
                    return
                }

                handleIntentFromPreview(activity, originalEnable, selectedCollection.items())
            }

            original_layout -> {
                val count = countOverMaxSize(selectedCollection)
                if (count <= 0) {
                    originalEnable = !originalEnable
                    original.setChecked(originalEnable)
                    spec?.onCheckedListener?.onCheck(originalEnable)
                    return
                }

                UIUtils.handleCause(
                    activity, IncapableCause(
                        IncapableCause.DIALOG, "",
                        getString(R.string.error_over_original_count, count, spec?.originalMaxSize)
                    )
                )
            }

            button_apply -> {
                if (allAlbum?.isAll() == true && allAlbum?.isEmpty() == true) {
                    UIUtils.handleCause(activity, IncapableCause(getString(R.string.empty_album)))
                    return
                }

                albumFolderSheetHelper.createFolderSheetDialog()
            }
        }
    }

    override fun provideSelectedItemCollection() = selectedCollection

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        val intent = Intent(this, AlbumPreviewActivity::class.java)
            .putExtra(ConstValue.EXTRA_ALBUM, album as Parcelable)
            .putExtra(ConstValue.EXTRA_ITEM, item)
            .putExtra(ConstValue.EXTRA_DEFAULT_BUNDLE, selectedCollection.getDataWithBundle())
            .putExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)

        startActivityForResult(intent, ConstValue.REQUEST_CODE_PREVIEW)
    }

    /**
     * 处理预览的[onActivityResult]
     */
    private fun doActivityResultFromPreview(data: Intent?) {
        data?.apply {

            originalEnable = getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false)
            val isApplyData = getBooleanExtra(ConstValue.EXTRA_RESULT_APPLY, false)
            handlePreviewIntent(activity, data, originalEnable, isApplyData, selectedCollection)

            if (!isApplyData) {
                val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                    MediaSelectionFragment::class.java.simpleName
                )
                if (mediaSelectionFragment is MediaSelectionFragment) {
                    mediaSelectionFragment.refreshMediaGrid()
                }
                updateBottomToolbar()
            }
        }
    }

    /**
     * 处理拍照的[onActivityResult]
     */
    private fun doActivityResultFromCapture() {
        val capturePathUri = mediaStoreCompat?.getCurrentPhotoUri() ?: return
        val capturePath = mediaStoreCompat?.getCurrentPhotoPath() ?: return
        val selectedPath = arrayListOf(capturePath)
        // 刷新系统相册
        MediaScannerConnection.scanFile(this, arrayOf(capturePath), null, null)
        // 重新获取相册数据
        albumLoadHelper.loadAlbumData()
        // 手动插入到相册列表
        albumFolderSheetHelper.insetAlbumToFolder(capturePathUri)

        albumFolderSheetHelper.getAlbumFolderList()?.apply {
            button_apply.post {
                onAlbumSelected(this[0])
            }
        }

        // Check is Crop first
        if (spec?.openCrop() == true) {
            gotoImageCrop(this, selectedPath)
        }
    }

    /**
     * 处理裁剪的[onActivityResult]
     */
    private fun doActivityResultFromCrop(cropPath: String?) {
        finishIntentFromCrop(activity, cropPath)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBottomToolbar() {
        val selectedCount = selectedCollection.count()
        if (selectedCount == 0) {
            button_complete.setText(getAttrString(R.attr.Media_Sure_text, R.string.button_sure))
        } else if (selectedCount == 1 && spec?.singleSelectionModeEnabled() == true) {
            button_complete.setText(getAttrString(R.attr.Media_Sure_text, R.string.button_sure))
        } else {
            button_complete.text =
                "${getString(
                    getAttrString(R.attr.Media_Sure_text, R.string.button_sure)
                )}($selectedCount)"
        }

        if (spec?.originalable == true) {
            UIUtils.setViewVisible(true, original_layout)
            updateOriginalState()
        } else {
            UIUtils.setViewVisible(false, original_layout)
        }
    }

    private fun updateOriginalState() {
        original.setChecked(originalEnable)
        if (countOverMaxSize(selectedCollection) > 0 || originalEnable) {
            UIUtils.handleCause(
                activity, IncapableCause(
                    IncapableCause.DIALOG, "",
                    getString(R.string.error_over_original_size, spec?.originalMaxSize)
                )
            )

            original.setChecked(false)
            originalEnable = false
        }
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

    private var albumCallback = object : AlbumCallbacks {
        override fun onAlbumStart() {
            // do nothing
        }

        override fun onAlbumLoad(cursor: Cursor) {
            albumFolderSheetHelper.setAlbumFolderCursor(cursor)

            Handler(Looper.getMainLooper()).post {
                if (cursor.moveToFirst()) {
                    allAlbum = Album.valueOf(cursor).apply { onAlbumSelected(this) }
                }
            }
        }

        override fun onAlbumReset() {
            albumFolderSheetHelper.clearFolderSheetDialog()
        }
    }

    private var albumSheetCallback = object : FolderBottomSheet.BottomSheetCallback {
        override fun initData(adapter: FolderItemMediaAdapter) {
            adapter.setListData(albumFolderSheetHelper.readAlbumFromCursor())
        }

        override fun onItemClick(album: Album, position: Int) {
            if (!albumFolderSheetHelper.setLastFolderCheckedPosition(position)) return
            albumLoadHelper.setStateCurrentSelection(position)

            button_apply.text = album.getDisplayName(activity)
            onAlbumSelected(album)
        }
    }
}
