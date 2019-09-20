package com.matisse.entity

import android.content.Context
import android.support.annotation.IntDef
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.matisse.widget.IncapableDialog

class IncapableCause {

    companion object {
        const val TOAST = 0x00
        const val DIALOG = 0x01
        const val NONE = 0x02

        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause == null)
                return

            when (cause.mForm) {
                NONE -> {
                }
                DIALOG -> {
                    val incapableDialog =
                        IncapableDialog.newInstance(cause.mTitle!!, cause.mMessage!!)
                    incapableDialog.show(
                        (context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name
                    )
                }
                TOAST -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TOAST, DIALOG, NONE)
    annotation class Form

    var mForm = TOAST
    var mTitle: String? = null
    var mMessage: String? = null

    constructor(message: String) : this(TOAST, message) {
        mMessage = message
    }

    constructor(title: String, message: String) : this(TOAST, title, message)

    constructor(@Form form: Int, message: String) : this(form, "", message)

    constructor(@Form form: Int, title: String, message: String) {
        mForm = form
        mTitle = title
        mMessage = message
    }
}