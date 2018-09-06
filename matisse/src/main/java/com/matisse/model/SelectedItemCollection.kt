package com.matisse.model

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import com.matisse.R
import com.matisse.entity.IncapableCause
import com.matisse.entity.Item
import com.matisse.internal.entity.SelectionSpec
import com.matisse.widget.CheckView
import com.matisse.utils.PathUtils
import com.matisse.utils.PhotoMetadataUtils
import java.util.LinkedHashSet

class SelectedItemCollection {

    private var mContext: Context
    private lateinit var mItems: LinkedHashSet<Item>
    private var mCollectionType = COLLECTION_UNDEFINED

    companion object {
        val STATE_SELECTION = "state_selection"
        val STATE_COLLECTION_TYPE = "state_collection_type"
        /**
         * Empty collection
         */
        val COLLECTION_UNDEFINED = 0x00
        /**
         * Collection only with images
         */
        val COLLECTION_IMAGE = 0x01
        /**
         * Collection only with videos
         */
        val COLLECTION_VIDEO = 0x01 shl 1
        /**
         * Collection with images and videos.
         */
        val COLLECTION_MIXED = COLLECTION_IMAGE or COLLECTION_VIDEO
    }

    constructor(context: Context) {
        mContext = context
    }

    fun onCreate(bundle: Bundle?) {
        if (bundle == null) {
            mItems = linkedSetOf()
        } else {
            val saved = bundle.getParcelableArrayList<Item>(STATE_SELECTION)
            mItems = LinkedHashSet(saved!!)
            mCollectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED)
        }
    }

    fun setDefaultSelection(uris: List<Item>) {
        mItems.addAll(uris)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems))
        outState.putInt(STATE_COLLECTION_TYPE, mCollectionType)
    }

    fun getDataWithBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems))
        bundle.putInt(STATE_COLLECTION_TYPE, mCollectionType)
        return bundle
    }

    fun add(item: Item): Boolean {
        if (typeConflict(item)) {
            throw IllegalArgumentException("Can't select images and videos at the same time.")
        }
        val added = mItems.add(item)
        if (added) {
            when (mCollectionType) {
                COLLECTION_UNDEFINED -> {
                    if (item.isImage()) {
                        mCollectionType = COLLECTION_IMAGE
                    } else if (item.isVideo()) {
                        mCollectionType = COLLECTION_VIDEO
                    }
                }

                COLLECTION_IMAGE, COLLECTION_VIDEO -> {
                    if (item.isVideo() || item.isImage()) {
                        mCollectionType = COLLECTION_MIXED
                    }
                }
            }
        }
        return added
    }

    fun remove(item: Item): Boolean {
        val removed = mItems.remove(item)
        if (removed) {
            if (mItems.size == 0) {
                mCollectionType = COLLECTION_UNDEFINED
            } else {
                if (mCollectionType == COLLECTION_MIXED) {
                    refineCollectionType()
                }
            }
        }
        return removed
    }

    fun overwrite(items: java.util.ArrayList<Item>, collectionType: Int) {
        mCollectionType = if (items.size == 0) {
            COLLECTION_UNDEFINED
        } else {
            collectionType
        }
        mItems.clear()
        mItems.addAll(items)
    }

    fun asList(): List<Item> {
        return ArrayList(mItems)
    }

    fun asListOfUri(): List<Uri> {
        val uris = ArrayList<Uri>()
        for (item in mItems) {
            uris.add(item.getContentUri()!!)
        }
        return uris
    }

    fun asListOfString(): List<String> {
        val paths = ArrayList<String>()
        mItems.forEach {
            val path = PathUtils.getPath(mContext, it.getContentUri()!!)
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
                mContext.getString(
                        R.string.error_over_count,
                        maxSelectable)
            } catch (e: Resources.NotFoundException) {
                mContext.getString(
                        R.string.error_over_count,
                        maxSelectable)
            } catch (e: NoClassDefFoundError) {
                mContext.getString(
                        R.string.error_over_count,
                        maxSelectable)
            }

            return IncapableCause(cause)
        } else if (typeConflict(item)) {
            return IncapableCause(mContext.getString(R.string.error_type_conflict))
        }

        return PhotoMetadataUtils.isAcceptable(mContext, item)
    }

    fun maxSelectableReached(): Boolean {
        return mItems.size == currentMaxSelectable()
    }

    // depends
    private fun currentMaxSelectable(): Int {
        val spec = SelectionSpec.getInstance()
        return when {
            spec.maxSelectable > 0 -> spec.maxSelectable
            mCollectionType == COLLECTION_IMAGE -> spec.maxImageSelectable
            mCollectionType == COLLECTION_VIDEO -> spec.maxVideoSelectable
            else -> spec.maxSelectable
        }
    }

    fun getCollectionType() = mCollectionType

    fun isEmpty() = mItems.isEmpty()

    fun isSelected(item: Item) = mItems.contains(item)

    fun count() = mItems.size

    fun checkedNumOf(item: Item): Int {
        val index = ArrayList(mItems).indexOf(item)
        return if (index == -1) CheckView.UNCHECKED else index + 1
    }

    private fun refineCollectionType() {
        var hasImage = false
        var hasVideo = false

        mItems.forEach {
            if (it.isImage() && !hasImage) hasImage = true
            if (it.isVideo() && !hasVideo) hasVideo = true
        }

        if (hasImage && hasVideo) {
            mCollectionType = COLLECTION_MIXED
        } else if (hasImage) {
            mCollectionType = COLLECTION_IMAGE
        } else if (hasVideo) {
            mCollectionType = COLLECTION_VIDEO
        }
    }

    /**
     * Determine whether there will be conflict media types. A user can only select images and videos at the same time
     * while [SelectionSpec.mediaTypeExclusive] is set to false.
     */
    fun typeConflict(item: Item): Boolean {
        return SelectionSpec.getInstance().mediaTypeExclusive && (item.isImage()
                && (mCollectionType == COLLECTION_VIDEO || mCollectionType == COLLECTION_MIXED) || item.isVideo()
                && (mCollectionType == COLLECTION_IMAGE || mCollectionType == COLLECTION_MIXED))
    }
}