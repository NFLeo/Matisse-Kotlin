package com.matisse.entity

class CaptureStrategy {

    var isPublic: Boolean = false
    lateinit var authority: String

    fun CaptureStrategy(isPublic: Boolean, authority: String) {
        this.isPublic = isPublic
        this.authority = authority
    }
}