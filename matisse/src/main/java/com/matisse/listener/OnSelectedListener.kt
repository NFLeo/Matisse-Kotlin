package com.matisse.listener

import android.net.Uri

/**
 * Created by liubo on 2018/9/10.
 */
interface OnSelectedListener {
    /**
     * @param uriList the selected item {@link Uri} list.
     * @param pathList the selected item file path list.
     */
    fun onSelected(uriList:List<Uri>, pathList:List<String>)
}