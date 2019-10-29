package com.matisse.entity

import android.net.Uri

data class SelecteResultData(
    var mediaId: String, var mediaUri: Uri? = null,
    var mediaPath: String? = "", var compressedPath: String? = ""
)