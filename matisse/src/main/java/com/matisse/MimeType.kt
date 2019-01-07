package com.matisse

/**
 * Describe : MIME Type enumeration to restrict selectable media on the selection activity.
 * Matisse only supports images and videos.
 * Created by Leo on 2018/8/29 on 14:55.
 */
enum class MimeType {

    // ============== images ==============
    JPEG {
        override fun getValue() = MimeTypeManager.arraySetOf("jpg", "jpeg")
        override fun getKey() = "image/jpeg"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    PNG {
        override fun getValue() = MimeTypeManager.arraySetOf("png")
        override fun getKey() = "image/png"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    GIF {
        override fun getValue() = MimeTypeManager.arraySetOf("gif")
        override fun getKey() = "image/gif"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    BMP {
        override fun getValue() = MimeTypeManager.arraySetOf("bmp")
        override fun getKey() = "image/x-ms-bmp"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    WEBP {
        override fun getValue() = MimeTypeManager.arraySetOf("webp")
        override fun getKey() = "image/webp"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    // ============== videos ==============
    MPEG {
        override fun getValue() = MimeTypeManager.arraySetOf("mpg")
        override fun getKey() = "video/mpeg"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    MP4 {
        override fun getValue() = MimeTypeManager.arraySetOf("m4v")
        override fun getKey() = "video/mp4"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    QUICKTIME {
        override fun getValue() = MimeTypeManager.arraySetOf("mov")
        override fun getKey() = "video/quicktime"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    THREEGPP {
        override fun getValue() = MimeTypeManager.arraySetOf("3gpp")
        override fun getKey() = "video/3gpp"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    THREEGPP2 {
        override fun getValue() = MimeTypeManager.arraySetOf("3gpp2")
        override fun getKey() = "video/3gpp2"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    MKV {
        override fun getValue() = MimeTypeManager.arraySetOf("mkv")
        override fun getKey() = "video/x-matroska"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    WEBM {
        override fun getValue() = MimeTypeManager.arraySetOf("webm")
        override fun getKey() = "video/webm"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    TS {
        override fun getValue() = MimeTypeManager.arraySetOf("ts")
        override fun getKey() = "video/mp2ts"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    },

    AVI {
        override fun getValue() = MimeTypeManager.arraySetOf("avi")
        override fun getKey() = "video/avi"
        override fun initEnum() = MimeTypeManager(getKey(), getValue())
    };

    abstract fun initEnum(): MimeTypeManager
    abstract fun getValue(): Set<String>
    abstract fun getKey(): String
}