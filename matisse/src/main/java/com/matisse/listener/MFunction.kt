package com.matisse.listener

import android.view.View

interface MFunction<T> {
    /**
     * Perform this method on the given parameters
     */
    fun accept(params: T, view: View?)
}