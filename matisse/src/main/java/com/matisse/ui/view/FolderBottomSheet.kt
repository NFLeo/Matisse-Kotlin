package com.matisse.ui.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matisse.R
import com.matisse.entity.Album
import com.matisse.entity.ConstValue
import com.matisse.ui.adapter.FolderItemMediaAdapter
import com.matisse.utils.UIUtils

class FolderBottomSheet : BottomSheetDialogFragment() {

    private var kParentView: View? = null
    private lateinit var recyclerView: RecyclerView
    var adapter: FolderItemMediaAdapter? = null
    var callback: BottomSheetCallback? = null
    private var currentPosition = 0

    companion object {
        fun instance(context: Context, currentPos: Int, tag: String): FolderBottomSheet {
            val bottomSheet = FolderBottomSheet()
            val bundle = Bundle()
            bundle.putInt(ConstValue.FOLDER_CHECK_POSITION, currentPos)
            bottomSheet.arguments = bundle
            bottomSheet.show((context as FragmentActivity).supportFragmentManager, tag)
            return bottomSheet
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPosition = arguments?.getInt(ConstValue.FOLDER_CHECK_POSITION, 0) ?: 0
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup): View {
        if (kParentView == null) {
            kParentView = inflater.inflate(R.layout.dialog_bottom_sheet_folder, container, false)
            setDefaultHeight(UIUtils.getScreenHeight(context!!) / 2)
            initView()
        } else {
            if (kParentView?.parent != null) {
                val parent = kParentView?.parent as ViewGroup
                parent.removeView(view)
            }
        }
        return kParentView!!
    }

    private fun initView() {
        recyclerView = kParentView?.findViewById(R.id.recyclerview)!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        setRecyclerViewHeight()
        adapter = FolderItemMediaAdapter(context!!, currentPosition).apply {
            recyclerView.adapter = this
            callback?.initData(this)

            itemClickListener = object : FolderItemMediaAdapter.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    dismiss()
                    callback?.onItemClick(albumList[position], position)
                }
            }
        }
    }

    private fun setRecyclerViewHeight() {
        recyclerView.layoutParams.height = UIUtils.getScreenHeight(context!!) / 2
    }

    interface BottomSheetCallback {
        fun initData(adapter: FolderItemMediaAdapter)
        /**
         * 点击回调
         * @param album 当前选中的相册
         * @param position 当前选中的位置
         */
        fun onItemClick(album: Album, position: Int)
    }
}