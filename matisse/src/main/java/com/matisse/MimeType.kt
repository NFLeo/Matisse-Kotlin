package com.matisse

import com.matisse.MimeTypeManager.Companion.arraySetOf

/**
 * Describe : MIME Type enumeration to restrict selectable media on the selection activity.
 * Matisse only supports images and videos.
 * Created by Leo on 2018/8/29 on 14:55.
 */
enum class MimeType {

    // ============== images ==============
    JPEG {
        override fun initEnum() = MimeTypeManager("image/jpeg", MimeTypeManager.arraySetOf("jpg", "jpeg"))
    },

    PNG {
        override fun initEnum() = MimeTypeManager("image/png", arraySetOf("png"))
    },

    GIF {
        override fun initEnum() = MimeTypeManager("image/gif", arraySetOf("gif"))
    },

    BMP {
        override fun initEnum() = MimeTypeManager("image/x-ms-bmp", arraySetOf("bmp"))
    },

    WEBP {
        override fun initEnum() = MimeTypeManager("image/webp", arraySetOf("webp"))
    },

    // ============== videos ==============
    MPEG {
        override fun initEnum() = MimeTypeManager("video/mpeg", arraySetOf("mpeg", "mpg"))
    },

    MP4 {
        override fun initEnum() = MimeTypeManager("video/mp4", arraySetOf("mp4", "m4v"))
    },

    QUICKTIME {
        override fun initEnum() = MimeTypeManager("video/quicktime", arraySetOf("mov"))
    },

    THREEGPP {
        override fun initEnum() = MimeTypeManager("video/3gpp", arraySetOf("3gp", "3gpp"))
    },

    THREEGPP2 {
        override fun initEnum() = MimeTypeManager("video/3gpp2", arraySetOf("3g2", "3gpp2"))
    },

    MKV {
        override fun initEnum() = MimeTypeManager("video/x-matroska", arraySetOf("mkv"))
    },

    WEBM {
        override fun initEnum() = MimeTypeManager("video/webm", arraySetOf("webm"))
    },

    TS {
        override fun initEnum() = MimeTypeManager("video/mp2ts", arraySetOf("ts"))
    },

    AVI {
        override fun initEnum() = MimeTypeManager("video/avi", arraySetOf("avi"))
    };


    abstract fun initEnum(): MimeTypeManager
}