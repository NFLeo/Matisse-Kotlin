package com.matisse.model

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import com.matisse.R
import com.matisse.entity.ConstValue.STATE_COLLECTION_TYPE
import com.matisse.entity.ConstValue.STATE_SELECTION
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.widget.CheckView
import com.matisse.utils.PathUtils
import com.matisse.utils.PhotoMetadataUtils
import java.util.LinkedHashSet

class SelectedItemCollection(context: Context) {

    private var context: Context = context
    private lateinit var items: LinkedHashSet<Item>
    private var collectionType = COLLECTION_UNDEFINED

    companion object {
        /**
         * Empty collection
         */
        const val COLLECTION_UNDEFINED = 0x00
        /**
         * Collection only with images
         */
        const val COLLECTION_IMAGE = 0x01
        /**
         * Collection only with videos
         */
        const val COLLECTION_VIDEO = 0x01 shl 1
        /**
         * Collection with images and videos.
         */
        const val COLLECTION_MIXED = COLLECTION_IMAGE or COLLECTION_VIDEO
    }

    fun onCreate(bundle: Bundle?) {
        if (bundle == null) {
            items = linkedSetOf()
        } else {
            val saved = bundle.getParcelableArrayList<Item>(STATE_SELECTION)
            items = LinkedHashSet(saved!!)
            collectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED)
        }
    }

    fun setDefaultSelection(uris: List<Item>) {
        items.addAll(uris)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SELECTION, ArrayList(items))
        outState.putInt(STATE_COLLECTION_TYPE, collectionType)
    }

    fun getDataWithBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelableArrayList(STATE_SELECTION, ArrayList(items))
        bundle.putInt(STATE_COLLECTION_TYPE, collectionType)
        return bundle
    }

    fun add(item: Item): Boolean {
        if (typeConflict(item)) {
            throw IllegalArgumentException("Can't select images and videos at the same time.")
        }
        val added = items.add(item)
        if (added) {
            when (collectionType) {
                COLLECTION_UNDEFINED -> {
                    if (item.isImage()) {
                        collectionType = COLLECTION_IMAGE
                    } else if (item.isVideo()) {
                        collectionType = COLLECTION_VIDEO
                    }
                }

                COLLECTION_IMAGE, COLLECTION_VIDEO -> {
                    if (item.isVideo() || item.isImage()) {
                        collectionType = COLLECTION_MIXED
                    }
                }
            }
        }
        return added
    }

    fun remove(item: Item): Boolean {
        val removed = items.remove(item)
        if (removed) {
            resetType()
        }
        return removed
    }

    fun removeAll() {
        items.clear()
        resetType()
    }

    private fun resetType() {
        if (items.size == 0) {
            collectionType = COLLECTION_UNDEFINED
        } else {
            if (collectionType == COLLECTION_MIXED) {
                refineCollectionType()
            }
        }
    }

    fun overwrite(items: java.util.ArrayList<Item>, collectionType: Int) {
        this.collectionType = if (items.size == 0) {
            COLLECTION_UNDEFINED
        } else {
            collectionType
        }
        this.items.clear()
        this.items.addAll(items)
    }

    fun asList() = ArrayList(items)

    fun asListOfUri(): List<Uri> {
        val uris = ArrayList<Uri>()
        for (item in items) {
            uris.add(item.getContentUri())
        }
        return uris
    }

    fun asListOfString(): List<String> {
        val paths = ArrayList<String>()
        items.forEach {
            val path = PathUtils.getPath(context, it.getContentUri())
            if (path != null) {
                paths.add(path)
            }
        }

        return paths
    }

    fun isAcceptable(item: Item): IncapableCause? {
        if (maxSelectableReached()) {
            val maxSelectable = currentMaxSelectable()

            val cause = try {
                context.getString(
                    R.string.error_over_count,
                    maxSelectable
                )
            } catch (e: Resources.NotFoundException) {
                context.getString(
                    R.string.error_over_count,
                    maxSelectable
                )
            } catch (e: NoClassDefFoundError) {
                context.getString(
                    R.string.error_over_count,
                    maxSelectable
                )
            }

            return IncapableCause(cause)
        } else if (typeConflict(item)) {
            return IncapableCause(context.getString(R.string.error_type_conflict))
        }

        return PhotoMetadataUtils.isAcceptable(context, item)
    }

    fun maxSelectableReached() = items.size == currentMaxSelectable()

    // depends
    private fun currentMaxSelectable(): Int {
        val spec = SelectionSpec.getInstance()
        return when {
            spec.maxSelectable > 0 -> spec.maxSelectable
            collectionType == COLLECTION_IMAGE -> spec.maxImageSelectable
            collectionType == COLLECTION_VIDEO -> spec.maxVideoSelectable
            else -> spec.maxSelectable
        }
    }

    fun getCollectionType() = collectionType

    fun isEmpty() = items.isEmpty()

    fun isSelected(item: Item?) = items.contains(item)

    fun count() = items.size

    fun checkedNumOf(item: Item?): Int {
        val index = ArrayList(items).indexOf(item)
        return if (index == -1) CheckView.UNCHECKED else index + 1
    }

    private fun refineCollectionType() {
        var hasImage = false
        var hasVideo = false

        items.forEach {
            if (it.isImage() && !hasImage) hasImage = true
            if (it.isVideo() && !hasVideo) hasVideo = true
        }

        if (hasImage && hasVideo) {
            collectionType = COLLECTION_MIXED
        } else if (hasImage) {
            collectionType = COLLECTION_IMAGE
        } else if (hasVideo) {
            collectionType = COLLECTION_VIDEO
        }
    }

    /**
     * Determine whether there will be conflict media types. A user can only select images and videos at the same time
     * while [SelectionSpec.mediaTypeExclusive] is set to false.
     */
    private fun typeConflict(item: Item) =
        SelectionSpec.getInstance().mediaTypeExclusive
                && (item.isImage() && (collectionType == COLLECTION_VIDEO || collectionType == COLLECTION_MIXED)
                || item.isVideo() && (collectionType == COLLECTION_IMAGE || collectionType == COLLECTION_MIXED))
}