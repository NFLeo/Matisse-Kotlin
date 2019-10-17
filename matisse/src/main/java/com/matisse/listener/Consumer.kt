package com.matisse.listener

interface Consumer<T> {
    /**
     * Perform this method on the given parameters
     */
    fun accept(params: T)
}