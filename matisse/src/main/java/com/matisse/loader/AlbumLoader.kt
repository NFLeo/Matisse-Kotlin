package com.matisse.loader

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.provider.MediaStore
import android.support.v4.content.CursorLoader
import com.matisse.internal.entity.Album
import com.matisse.internal.entity.SelectionSpec

/**
 * Describe : Load all albums(group by bucket_id) into a single cursor
 * Created by Leo on 2018/8/29 on 14:28.
 */
class AlbumLoader(context: Context, selection: String, selectionArgs: Array<out String>) : CursorLoader(context, QUERY_URI, PROJECTION, selection, selectionArgs, BUCKET_ORDER_BY) {

    companion object {
        val COLUMN_COUNT = "count"
        private val QUERY_URI = MediaStore.Files.getContentUri("external")
        const val BUCKET_ID = "bucket_id"
        const val BUCKET_DISPLAY_NAME = "bucket_display_name"
        private const val BUCKET_ORDER_BY = "datetaken DESC"

        val COLUMNS = arrayOf(MediaStore.Files.FileColumns._ID, BUCKET_ID,
                BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATA, COLUMN_COUNT)

        val PROJECTION = arrayOf(MediaStore.Files.FileColumns._ID, BUCKET_ID,
                BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATA,
                "COUNT(*) AS $COLUMN_COUNT")

        private val SELECTION = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? " +
                "OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?) " +
                "AND " + MediaStore.MediaColumns.SIZE + ">0) GROUP BY (" + BUCKET_ID

        private val SELECTION_ARGS = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

        private const val SELECTION_FOR_SINGLE_MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " +
                MediaStore.MediaColumns.SIZE + ">0) GROUP BY (" + BUCKET_ID

        private fun getSelectionArgsForSingleMediaType(mediaType: Int) = arrayOf(mediaType.toString())

        fun newInstance(context: Context): CursorLoader {
            var selection: String = SELECTION_FOR_SINGLE_MEDIA_TYPE
            val selectionArgs: Array<String>

            when {
                SelectionSpec.getInstance().onlyShowImages() ->
                    selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                SelectionSpec.getInstance().onlyShowVideos() ->
                    selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                else -> {
                    selection = SELECTION
                    selectionArgs = SELECTION_ARGS
                }
            }

            return AlbumLoader(context, selection, selectionArgs)
        }
    }

    override fun loadInBackground(): Cursor? {
        val albums = super.loadInBackground()
        val allAlbum = MatrixCursor(COLUMNS)
        var totalCount = 0
        var allAlbumCoverPath = ""

        if (albums != null) {
            while (albums.moveToNext()) {
                totalCount += albums.getInt(albums.getColumnIndex(COLUMN_COUNT))
            }

            if (albums.moveToFirst()) {
                allAlbumCoverPath = albums.getString(albums.getColumnIndex(MediaStore.MediaColumns.DATA))
            }
        }

        allAlbum.addRow(arrayOf(Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL,
                allAlbumCoverPath, totalCount.toString()))
        return MergeCursor(arrayOf(allAlbum, albums))
    }

    override fun onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}