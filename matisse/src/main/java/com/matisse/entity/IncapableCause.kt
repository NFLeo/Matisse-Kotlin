package com.matisse.entity

import android.content.Context
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.matisse.widget.IncapableDialog

class IncapableCause {

    var mForm = FORM.TOAST
    var mTitle: String? = null
    var mMessage: String? = null

    companion object {
        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause == null) {
                return
            }

            when (cause.mForm) {
                FORM.NONE -> {
                }
                FORM.DIALOG -> {
                    var incapableDialog = IncapableDialog.newInstance(cause.mTitle, cause.mMessage)
                    incapableDialog.show((context as FragmentActivity).supportFragmentManager, IncapableDialog::class.simpleName)
                }
                FORM.TOAST -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()

            }
        }
    }

    enum class FORM {
        TOAST, DIALOG, NONE
    }

    constructor(message: String) : this(FORM.TOAST, "", message)

    constructor(title: String, message: String) : this(FORM.TOAST, title, message)

    constructor(form: FORM, message: String) : this(form, "", message)

    constructor(form: FORM, title: String, message: String) {
        mForm = form
        mTitle = title
        mMessage = message
    }

    /**
     * handleCause(Context context, IncapableCause cause)
     *
     * 为静态方法 见UIUtils.handleCause(Context, IncapableCause)
     **/
}