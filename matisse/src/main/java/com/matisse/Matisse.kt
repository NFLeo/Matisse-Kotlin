/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.matisse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import com.matisse.ui.view.MatisseActivity
import java.lang.ref.WeakReference

/**
 * Entry for Matisse's media selection.
 */
class Matisse private constructor(activity: Activity?, fragment: Fragment? = null) {

    private val mContext: WeakReference<Activity>
    private val mFragment: WeakReference<Fragment>?

    internal val activity: Activity?
        get() = mContext.get()

    internal val fragment: Fragment?
        get() = mFragment?.get()

    private constructor(fragment: Fragment) : this(fragment.activity, fragment) {}

    init {
        mContext = WeakReference<Activity>(activity)
        mFragment = WeakReference<Fragment>(fragment)
    }

    /**
     * MIME types the selection constrains on.
     *
     *
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
     *
     *
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

    companion object {

        /**
         * Start Matisse from an Activity.
         *
         *
         * This Activity's [Activity.onActivityResult] will be called when user
         * finishes selecting.
         *
         * @param activity Activity instance.
         * @return Matisse instance.
         */
        fun from(activity: Activity): Matisse {
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
            return data.getParcelableArrayListExtra(MatisseActivity.EXTRA_RESULT_SELECTION)
        }

        /**
         * Obtain user selected media path list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media path list.
         */
        fun obtainPathResult(data: Intent): List<String> {
            return data.getStringArrayListExtra(MatisseActivity.EXTRA_RESULT_SELECTION_PATH)
        }

        /**
         * Obtain state whether user decide to use selected media in original
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return Whether use original photo
         */
        fun obtainOriginalState(data: Intent): Boolean {
            return data.getBooleanExtra(MatisseActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false)
        }
    }

}
