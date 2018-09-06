package com.matisse.entity

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView

data class PreBindInfo(var mResize: Int, var mPlaceholder: Drawable, var mCheckViewCountable: Boolean, var mViewHolder: RecyclerView.ViewHolder)