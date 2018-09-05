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
        override fun getValue() = MimeTypeManager.arraySetOf("jpg", "jpeg")
        override fun initEnum() = MimeTypeManager("image/jpeg", getValue())
    },

    PNG {
        override fun getValue() = MimeTypeManager.arraySetOf("png")
        override fun initEnum() = MimeTypeManager("image/png", getValue())
    },

    GIF {
        override fun getValue() = MimeTypeManager.arraySetOf("gif")
        override fun initEnum() = MimeTypeManager("image/gif", getValue())
    },

    BMP {
        override fun getValue() = MimeTypeManager.arraySetOf("bmp")
        override fun initEnum() = MimeTypeManager("image/x-ms-bmp", getValue())
    },

    WEBP {
        override fun getValue() = MimeTypeManager.arraySetOf("webp")
        override fun initEnum() = MimeTypeManager("image/webp", getValue())
    },

    // ============== videos ==============
    MPEG {
        override fun getValue() = MimeTypeManager.arraySetOf("mpg")
        override fun initEnum() = MimeTypeManager("video/mpeg", getValue())
    },

    MP4 {
        override fun getValue() = MimeTypeManager.arraySetOf("m4v")
        override fun initEnum() = MimeTypeManager("video/mp4", getValue())
    },

    QUICKTIME {
        override fun getValue() = MimeTypeManager.arraySetOf("mov")
        override fun initEnum() = MimeTypeManager("video/quicktime", getValue())
    },

    THREEGPP {
        override fun getValue() = MimeTypeManager.arraySetOf("3gpp")
        override fun initEnum() = MimeTypeManager("video/3gpp", getValue())
    },

    THREEGPP2 {
        override fun getValue() = MimeTypeManager.arraySetOf("3gpp2")
        override fun initEnum() = MimeTypeManager("video/3gpp2", getValue())
    },

    MKV {
        override fun getValue() = MimeTypeManager.arraySetOf("mkv")
        override fun initEnum() = MimeTypeManager("video/x-matroska", getValue())
    },

    WEBM {
        override fun getValue() = MimeTypeManager.arraySetOf("webm")
        override fun initEnum() = MimeTypeManager("video/webm", getValue())
    },

    TS {
        override fun getValue() = MimeTypeManager.arraySetOf("ts")
        override fun initEnum() = MimeTypeManager("video/mp2ts", getValue())
    },

    AVI {
        override fun getValue() = MimeTypeManager.arraySetOf("avi")
        override fun initEnum() = MimeTypeManager("video/avi", getValue())
    };

    abstract fun initEnum(): MimeTypeManager
    abstract fun getValue(): Set<String>
}