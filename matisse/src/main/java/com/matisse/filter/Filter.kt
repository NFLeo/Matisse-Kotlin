package com.matisse.filter

import android.content.Context
import com.matisse.MimeTypeManager
import com.matisse.entity.Item

/**
 * Describe : Filter for choosing a {@link Item}. You can add multiple Filters through
 * {@link SelectionCreator #addFilter(Filter)}.
 * Created by Leo on 2018/9/4 on 16:12.
 */
abstract class Filter {
    companion object {

        // Convenient constant for a minimum value
        const val MIN = 0

        // Convenient constant for a maximum value
        const val MAX = Int.MAX_VALUE

        // Convenient constant for 1024
        const val K = 1024
    }

    // Against what mime types this filter applies
    open abstract fun constraintTypes(): Set<MimeTypeManager>

    /**
     * Invoked for filtering each item
     *
     * @return null if selectable, {@link IncapableCause} if not selectable.
     */
    abstract fun filter(context: Context, item: Item)

    // Whether an {@link Item} need filtering
    open fun needFiltering(context: Context, item: Item): Boolean {
        constraintTypes().forEach {
            if (it.checkType(context.contentResolver, item.getContentUri())) {
                return true
            }
        }

        return false
    }
}