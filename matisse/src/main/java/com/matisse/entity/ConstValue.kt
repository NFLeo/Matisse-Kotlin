package com.matisse.entity

import com.matisse.ucrop.UCrop

object ConstValue {
    const val EXTRA_RESULT_SELECTION = "extra_result_selection"
    const val EXTRA_RESULT_SELECTION_ID = "extra_result_selection_id"
    const val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
    const val EXTRA_ALBUM = "extra_album"
    const val EXTRA_ITEM = "extra_item"

    const val CHECK_STATE = "checkState"

    const val FOLDER_CHECK_POSITION = "folder_check_position"

    const val EXTRA_DEFAULT_BUNDLE = "extra_default_bundle"
    const val EXTRA_RESULT_BUNDLE = "extra_result_bundle"
    const val EXTRA_RESULT_CROP_BACK_BUNDLE = UCrop.EXTRA_OUTPUT_URI
    const val EXTRA_RESULT_APPLY = "extra_result_apply"

    const val STATE_SELECTION = "state_selection"
    const val STATE_COLLECTION_TYPE = "state_collection_type"

    const val REQUEST_CODE_PREVIEW = 23
    const val REQUEST_CODE_CAPTURE = 24
    const val REQUEST_CODE_CROP = 69            // 对应UCrop中的key
    const val REQUEST_CODE_CROP_ERROR = 96      // 对应UCrop中的key
    const val REQUEST_CODE_CHOOSE = 26
}