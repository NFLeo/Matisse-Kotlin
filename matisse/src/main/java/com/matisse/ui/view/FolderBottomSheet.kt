package com.matisse.ui.view

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.matisse.R
import com.matisse.entity.ConstValue
import com.matisse.ui.adapter.FolderMediaAdapter
import com.matisse.utils.UIUtils

class FolderBottomSheet : BottomSheetDialogFragment() {

    private var kParentView: View? = null
    private lateinit var recyclerView: RecyclerView
    var adapter: FolderMediaAdapter? = null
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
        adapter = FolderMediaAdapter(context!!, currentPosition)
        recyclerView.adapter = adapter

        callback?.initData(adapter!!)

        adapter?.itemClickListener = object : FolderMediaAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                dismiss()
                callback?.onItemClick(adapter?.getCursor()!!, position)
            }
        }
    }

    interface BottomSheetCallback {
        fun initData(adapter: FolderMediaAdapter)
        fun onItemClick(cursor: Cursor, position: Int)
    }
}