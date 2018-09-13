package com.matisse.widget

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class MediaGridInset : RecyclerView.ItemDecoration {
    private var mSpanCount: Int = 0
    private var mSpacing: Int = 0
    private var mIncludeEdge: Boolean = false

    constructor(mSpanCount: Int, mSpacing: Int, mIncludeEdge: Boolean) : super() {
        this.mSpanCount = mSpanCount
        this.mSpacing = mSpacing
        this.mIncludeEdge = mIncludeEdge
    }

    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        val position = parent?.getChildAdapterPosition(view) ?: 0
        val column = position % mSpanCount

        outRect?.apply {
            if (mIncludeEdge) {
                left = mSpacing - column * mSpacing / mSpanCount
                right = (column + 1) * mSpacing / mSpanCount

                if (position < mSpanCount) {
                    top = mSpacing
                }

                bottom = mSpacing
            } else {
                left = column * mSpacing / mSpanCount
                right = mSpacing - (column + 1) * mSpacing / mSpanCount

                if (position >= mSpanCount) {
                    top = mSpacing
                }
            }
        }
    }
}