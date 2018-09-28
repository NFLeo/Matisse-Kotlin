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
    private var mParentView: View? = null
    private lateinit var recyclerView: RecyclerView
    var mAdapter: FolderMediaAdapter? = null
    var callback: BottomSheetCallback? = null
    private var mCurrentPosition: Int = 0

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
        mCurrentPosition = arguments?.getInt(ConstValue.FOLDER_CHECK_POSITION, 0) ?: 0
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup): View {
        if (mParentView == null) {
            mParentView = inflater.inflate(R.layout.dialog_bottom_sheet_folder, container, false)
            setDefaultHeight(UIUtils.getScreenHeight(context!!) / 2)
            initView()
        } else {
            if (mParentView?.parent != null) {
                val parent = mParentView?.parent as ViewGroup
                parent.removeView(view)
            }
        }
        return mParentView!!
    }

    private fun initView() {
        recyclerView = mParentView?.findViewById(R.id.recyclerview)!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        mAdapter = FolderMediaAdapter(context, mCurrentPosition)
        recyclerView.adapter = mAdapter

        callback?.initData(mAdapter!!)

        mAdapter?.mItemClickListener = object : FolderMediaAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                dismiss()
                callback?.onItemClick(mAdapter?.getCursor()!!, position)
            }
        }
    }

    interface BottomSheetCallback {
        fun initData(adapter: FolderMediaAdapter)
        fun onItemClick(cursor: Cursor, position: Int)
    }
}