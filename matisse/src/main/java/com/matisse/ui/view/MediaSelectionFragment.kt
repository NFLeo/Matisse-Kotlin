package com.matisse.ui.view

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.model.AlbumCallbacks
import com.matisse.model.AlbumMediaCollection
import com.matisse.model.SelectedItemCollection
import com.matisse.ui.adapter.AlbumMediaAdapter
import com.matisse.utils.UIUtils
import com.matisse.widget.MediaGridInset
import kotlinx.android.synthetic.main.fragment_media_selection.*

class MediaSelectionFragment : Fragment(), AlbumCallbacks, AlbumMediaAdapter.CheckStateListener,
    AlbumMediaAdapter.OnMediaClickListener {

    private val albumMediaCollection = AlbumMediaCollection()
    private lateinit var adapter: AlbumMediaAdapter
    private lateinit var album: Album
    private lateinit var selectionProvider: SelectionProvider
    private lateinit var checkStateListener: AlbumMediaAdapter.CheckStateListener
    private lateinit var onMediaClickListener: AlbumMediaAdapter.OnMediaClickListener

    companion object {

        fun newInstance(album: Album): MediaSelectionFragment {
            val fragment = MediaSelectionFragment()
            val args = Bundle()
            args.putParcelable(ConstValue.EXTRA_ALBUM, album)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SelectionProvider) {
            selectionProvider = context
        } else {
            throw IllegalStateException("Context must implement SelectionProvider.")
        }

        if (context is AlbumMediaAdapter.CheckStateListener) checkStateListener = context

        if (context is AlbumMediaAdapter.OnMediaClickListener) onMediaClickListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_media_selection, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        album = arguments?.getParcelable(ConstValue.EXTRA_ALBUM)!!
        adapter = AlbumMediaAdapter(
            context!!, selectionProvider.provideSelectedItemCollection(), recyclerview
        )
        adapter.checkStateListener = this
        adapter.onMediaClickListener = this
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
        recyclerview.adapter = adapter
        albumMediaCollection.onCreate(activity!!, this)
        albumMediaCollection.load(album, selectionSpec.capture)
    }

    fun refreshMediaGrid() {
        adapter.notifyDataSetChanged()
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        onMediaClickListener.onMediaClick(this.album, item, adapterPosition)
    }

    override fun onSelectUpdate() {
        checkStateListener.onSelectUpdate()
    }

    override fun onAlbumStart() {
        // do nothing
    }

    override fun onAlbumLoad(cursor: Cursor) {
        adapter.swapCursor(cursor)
    }

    override fun onAlbumReset() {
        adapter.swapCursor(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        albumMediaCollection.onDestroy()
    }

    interface SelectionProvider {
        fun provideSelectedItemCollection(): SelectedItemCollection
    }
}