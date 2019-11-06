package com.matisse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.matisse.entity.ConstValue
import java.lang.ref.WeakReference

/**
 * Entry for Matisse's media selection.
 */
class Matisse(activity: Activity?, fragment: Fragment? = null) {

    companion object {

        /**
         * Start Matisse from an Activity.
         * This Activity's [Activity.onActivityResult] will be called when user
         * finishes selecting.
         *
         * @param activity Activity instance.
         * @return Matisse instance.
         */
        fun from(activity: Activity?): Matisse {
            return Matisse(activity)
        }

        /**
         * Start Matisse from a Fragment.
         *
         *
         * This Fragment's [Fragment.onActivityResult] will be called when user
         * finishes selecting.
         *
         * @param fragment Fragment instance.
         * @return Matisse instance.
         */
        fun from(fragment: Fragment): Matisse {
            return Matisse(fragment)
        }

        /**
         * Obtain user selected media' [Uri] list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media' [Uri] list.
         */
        fun obtainResult(data: Intent): List<Uri> {
            return data.getParcelableArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION)
        }

        /**
         * Obtain user selected compressed file path.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected compressed media path list.
         */
        fun obtainCompressResult(data: Intent): List<String>? {
            return data.getStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_COMPRESS)?.run {
                this
            }
        }

        /**
         * Obtain user selected media path list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media path list.
         */
        fun obtainPathResult(data: Intent): List<String>? {
            return data.getStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_PATH)?.run {
                this
            }
        }

        /**
         * Obtain user selected media path id list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media path id list.
         */
        fun obtainPathIdResult(data: Intent): List<String>? {
            return data.getStringArrayListExtra(ConstValue.EXTRA_RESULT_SELECTION_ID)?.run {
                this
            }
        }

        /**
         * Obtain state whether user decide to use selected media in original
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return Whether use original photo
         */
        fun obtainOriginalState(data: Intent) =
            data.getBooleanExtra(ConstValue.EXTRA_RESULT_ORIGINAL_ENABLE, false)
    }

    private val mContext = WeakReference(activity)
    private val mFragment: WeakReference<Fragment>?

    internal val activity: Activity?
        get() = mContext.get()

    internal val fragment: Fragment?
        get() = mFragment?.get()

    private constructor(fragment: Fragment) : this(fragment.activity, fragment)

    init {
        mFragment = WeakReference<Fragment>(fragment)
    }

    /**
     * MIME types the selection constrains on.
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes MIME types set user can choose from.
     * @return [SelectionCreator] to build select specifications.
     * @see MimeType
     *
     * @see SelectionCreator
     */
    fun choose(mimeTypes: Set<MimeType>): SelectionCreator {
        return this.choose(mimeTypes, true)
    }

    /**
     * MIME types the selection constrains on.
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes          MIME types set user can choose from.
     * @param mediaTypeExclusive Whether can choose images and videos at the same time during one single choosing
     * process. true corresponds to not being able to choose images and videos at the same
     * time, and false corresponds to being able to do this.
     * @return [SelectionCreator] to build select specifications.
     * @see MimeType
     *
     * @see SelectionCreator
     */
    fun choose(mimeTypes: Set<MimeType>, mediaTypeExclusive: Boolean): SelectionCreator {
        return SelectionCreator(this, mimeTypes, mediaTypeExclusive)
    }
}
