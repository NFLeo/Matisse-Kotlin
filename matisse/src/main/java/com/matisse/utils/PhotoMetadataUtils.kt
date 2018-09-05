package com.matisse.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.matisse.MimeTypeManager
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec

/**
 * Created by Leo on 2018/8/29 on 15:24.
 */
object PhotoMetadataUtils {
    private val SCHEME_CONTENT = "content"

    fun getPath(resolver: ContentResolver, uri: Uri): String? {
        if (SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null

            try {
                cursor = resolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
                if (cursor == null || !cursor.moveToFirst()) {
                    return null
                }

                return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
            } finally {
                cursor?.close()
            }
        }

        return uri.path
    }

    fun isAcceptable(context: Context, item: Item): IncapableCause? {
        if (!isSelectableType(context, item)) {
            return IncapableCause(context.getString(R.string.error_file_type))
        }

        if (SelectionSpec.getInstance().filters != null) {
            SelectionSpec.getInstance().filters?.forEach {
                return it.filter(context, item)
            }
        }
        return null
    }

    private fun isSelectableType(context: Context?, item: Item): Boolean {
        if (context == null) {
            return false
        }

        val resolver = context.contentResolver
        for (type in SelectionSpec.getInstance().mimeTypeSet) {
            if (MimeTypeManager.checkType(resolver, item.getContentUri(), type.getValue())) {
                return true
            }
        }
        return false
    }
}