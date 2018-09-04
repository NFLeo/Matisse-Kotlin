package com.matisse.utils

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

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
                if (cursor != null) {
                    cursor.close()
                }
            }
        }

        return uri.path
    }
}