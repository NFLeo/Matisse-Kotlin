package com.matisse.loader

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import com.matisse.MimeTypeManager
import com.matisse.entity.Album
import com.matisse.internal.entity.SelectionSpec
import com.matisse.utils.Platform.beforeAndroidTen
import java.util.*

/**
 * Describe : Load all albums(group by bucket_id) into a single cursor
 * Created by Leo on 2018/8/29 on 14:28.
 */
class AlbumLoader(context: Context, selection: String, selectionArgs: Array<out String>) :
    CursorLoader(
        context, QUERY_URI, if (beforeAndroidTen()) PROJECTION else PROJECTION_29,
        selection, selectionArgs, BUCKET_ORDER_BY
    ) {

    companion object {
        const val COLUMN_COUNT = "count"
        private val QUERY_URI = MediaStore.Files.getContentUri("external")
        const val BUCKET_ID = "bucket_id"
        const val BUCKET_DISPLAY_NAME = "bucket_display_name"
        private const val BUCKET_ORDER_BY = "datetaken DESC"

        const val COLUMN_URI = "uri"

        val COLUMNS = arrayOf(
            MediaStore.Files.FileColumns._ID,
            BUCKET_ID, BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            COLUMN_URI, COLUMN_COUNT
        )

        val PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID, BUCKET_ID,
            BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
            "COUNT(*) AS $COLUMN_COUNT"
        )

        private val PROJECTION_29 = arrayOf(
            MediaStore.Files.FileColumns._ID,
            BUCKET_ID, BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE
        )

        private const val SELECTION = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? " +
                "OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?) " +
                "AND " + MediaStore.MediaColumns.SIZE + ">0) GROUP BY (" + BUCKET_ID

        private const val SELECTION_29 = (
                "(" + MediaStore.Files.FileColumns.MEDIA_TYPE
                        + "=? OR " + MediaStore.Files.FileColumns.MEDIA_TYPE
                        + "=?) AND " + MediaStore.MediaColumns.SIZE + ">0")

        private val SELECTION_ARGS = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        private const val SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " +
                    MediaStore.MediaColumns.SIZE + ">0) GROUP BY (" + BUCKET_ID

        private const val SELECTION_FOR_SINGLE_MEDIA_TYPE_29 = (
                MediaStore.Files.FileColumns.MEDIA_TYPE
                        + "=? AND " + MediaStore.MediaColumns.SIZE + ">0")

        private fun getSelectionArgsForSingleMediaType(mediaType: Int) =
            arrayOf(mediaType.toString())

        fun newInstance(context: Context): CursorLoader {
            var selection = if (beforeAndroidTen())
                SELECTION_FOR_SINGLE_MEDIA_TYPE
            else
                SELECTION_FOR_SINGLE_MEDIA_TYPE_29
            val selectionArgs: Array<String>

            when {
                SelectionSpec.getInstance().onlyShowImages() ->
                    selectionArgs =
                        getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                SelectionSpec.getInstance().onlyShowVideos() ->
                    selectionArgs =
                        getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                else -> {
                    selection = if (beforeAndroidTen()) SELECTION else SELECTION_29
                    selectionArgs = SELECTION_ARGS
                }
            }

            return AlbumLoader(context, selection, selectionArgs)
        }
    }

    override fun loadInBackground(): Cursor? {
        val albums = super.loadInBackground()
        val allAlbum = MatrixCursor(COLUMNS)
        return if (beforeAndroidTen()) {
            loadBelowAndroidQ(albums, allAlbum)
        } else loadAboveAndroidQ(albums, allAlbum)
    }

    private fun loadBelowAndroidQ(
        albums: Cursor?, allAlbum: MatrixCursor
    ): MergeCursor {
        var totalCount = 0
        var allAlbumCoverUri: Uri? = null
        val otherAlbums = MatrixCursor(COLUMNS)
        albums?.apply {
            while (moveToNext()) {
                val fileId = getLong(getColumnIndex(MediaStore.Files.FileColumns._ID))
                val bucketId = getLong(getColumnIndex(BUCKET_ID))
                val bucketDisplayName = getString(getColumnIndex(BUCKET_DISPLAY_NAME))
                val mimeType = getString(getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                val uri = getUri(albums)
                val count = getInt(getColumnIndex(COLUMN_COUNT))

                otherAlbums.addRow(
                    arrayOf(
                        fileId, bucketId, bucketDisplayName,
                        mimeType, uri.toString(), count.toString()
                    )
                )
                totalCount += count
            }
            if (albums.moveToFirst()) {
                allAlbumCoverUri = getUri(albums)
            }
        }

        allAlbumAddRow(allAlbumCoverUri, totalCount, allAlbum)
        return MergeCursor(arrayOf<Cursor>(allAlbum, otherAlbums))
    }

    private fun loadAboveAndroidQ(
        albums: Cursor?, allAlbum: MatrixCursor
    ): MergeCursor {
        var totalCount = 0
        var allAlbumCoverUri: Uri? = null
        val otherAlbums = MatrixCursor(COLUMNS)

        // Pseudo GROUP BY
        val countMap = hashMapOf<Long, Long>()
        albums?.apply {
            while (moveToNext()) {
                val bucketId = getLong(getColumnIndex(BUCKET_ID))

                var count: Long? = countMap[bucketId]
                if (count == null) {
                    count = 1L
                } else {
                    count++
                }
                countMap[bucketId] = count
            }

            if (moveToFirst()) {
                allAlbumCoverUri = getUri(this)

                val done = HashSet<Long>()

                do {
                    val bucketId = getLong(getColumnIndex(BUCKET_ID))

                    if (done.contains(bucketId)) continue

                    val fileId = getLong(getColumnIndex(MediaStore.Files.FileColumns._ID))
                    val bucketDisplayName = getString(getColumnIndex(BUCKET_DISPLAY_NAME))
                    val mimeType = getString(getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                    val uri = getUri(this)
                    val count = countMap[bucketId]

                    otherAlbums.addRow(
                        arrayOf<String>(
                            fileId.toString(), bucketId.toString(), bucketDisplayName,
                            mimeType, uri.toString(), count.toString()
                        )
                    )
                    done.add(bucketId)

                    totalCount += count?.toInt() ?: 0
                } while (albums.moveToNext())
            }
        }

        allAlbumAddRow(allAlbumCoverUri, totalCount, allAlbum)
        return MergeCursor(arrayOf<Cursor>(allAlbum, otherAlbums))
    }

    private fun allAlbumAddRow(
        allAlbumCoverUri: Uri?, totalCount: Int, allAlbum: MatrixCursor
    ) {
        val row: Array<String?> = arrayOfNulls(6)
        row[0] = Album.ALBUM_ID_ALL
        row[1] = Album.ALBUM_ID_ALL
        row[2] = Album.ALBUM_NAME_ALL
        row[3] = null
        row[4] = allAlbumCoverUri?.toString()
        row[5] = totalCount.toString()
        allAlbum.addRow(row)
    }

    private fun getUri(cursor: Cursor): Uri {
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
        val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
        val contentUri = when {
            MimeTypeManager.isImage(mimeType) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MimeTypeManager.isVideo(mimeType) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        return ContentUris.withAppendedId(contentUri, id)
    }

    override fun onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}