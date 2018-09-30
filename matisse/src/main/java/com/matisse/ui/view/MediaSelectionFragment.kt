package com.matisse.ui.view

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.matisse.R
import com.matisse.R.id.recyclerview
import com.matisse.entity.Item
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumMediaCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.utils.UIUtils
import com.matisse.widget.MediaGridInset
import kotlinx.android.synthetic.main.fragment_media_selection.*

class MediaSelectionFragment : Fragment(), AlbumCallbacks, AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener {

    private val mAlbumMediaCollection = AlbumMediaCollection()
    private lateinit var mAdapter: AlbumMediaAdapter
    private lateinit var mAlbum: Album
    private lateinit var mSelectionProvider: SelectionProvider
    private lateinit var mCheckStateListener: AlbumMediaAdapter.CheckStateListener
    private lateinit var mOnMediaClickListener: AlbumMediaAdapter.OnMediaClickListener

    companion object {

        fun newInstance(album: Album): MediaSelectionFragment {
            val fragment = MediaSelectionFragment()
            val args = Bundle()
            args.putParcelable(ConstValue.EXTRA_ALBUM, album)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is SelectionProvider) {
            mSelectionProvider = context
        } else {
            throw IllegalStateException("Context must implement SelectionProvider.")
        }

        if (context is AlbumMediaAdapter.CheckStateListener) {
            mCheckStateListener = context
        }

        if (context is AlbumMediaAdapter.OnMediaClickListener) {
            mOnMediaClickListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_media_selection, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mAlbum = arguments?.getParcelable(ConstValue.EXTRA_ALBUM)!!
        mAdapter = AlbumMediaAdapter(context!!, mSelectionProvider.provideSelectedItemCollection(), recyclerview)
        mAdapter.mCheckStateListener = this
        mAdapter.mOnMediaClickListener = this
        recyclerview.setHasFixedSize(true)

        val selectionSpec = SelectionSpec.getInstance()
        val spanCount = if (selectionSpec.gridExpectedSize > 0) {
            UIUtils.spanCount(context!!, selectionSpec.gridExpectedSize)
        } else {
            selectionSpec.spanCount
        }

        recyclerview.layoutManager = GridLayoutManager(context!!, spanCount)
        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
        recyclerview.addItemDecoration(MediaGridInset(spanCount, spacing, false))
        recyclerview.adapter = mAdapter
        mAlbumMediaCollection.onCreate(activity!!, this)
        mAlbumMediaCollection.load(mAlbum, selectionSpec.capture)
    }

    fun refreshMediaGrid() {
        mAdapter.notifyDataSetChanged()
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        mOnMediaClickListener.onMediaClick(mAlbum, item, adapterPosition)
    }

    override fun onUpdate() {
        mCheckStateListener.onUpdate()
    }

    override fun onAlbumStart() {
    }

    override fun onAlbumLoad(cursor: Cursor) {
        mAdapter.swapCursor(cursor)
    }

    override fun onAlbumReset() {
        mAdapter.swapCursor(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAlbumMediaCollection.onDestory()
    }

    interface SelectionProvider {
        fun provideSelectedItemCollection(): SelectedItemCollection
    }
}