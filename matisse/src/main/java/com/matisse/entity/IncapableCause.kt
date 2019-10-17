package com.matisse.entity

import android.content.Context
import android.support.annotation.IntDef
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.matisse.internal.entity.SelectionSpec
import com.matisse.listener.Consumer
import com.matisse.widget.IncapableDialog

class IncapableCause {

    companion object {
        const val TOAST = 0x0001
        const val DIALOG = 0x0002
        const val NONE = 0x0003

        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause == null) return

            cause.consumer?.accept(cause.message ?: "")

            when (cause.form) {
                DIALOG -> {
                    val incapableDialog =
                        IncapableDialog.newInstance(cause.title, cause.message)
                    incapableDialog.show(
                        (context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name
                    )
                }
                TOAST -> Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
                NONE -> {
                }
                else -> Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TOAST, DIALOG, NONE)
    annotation class Form

    var form = TOAST
    var title: String? = null
    var message: String? = null
    var consumer: Consumer<String>? = null

    constructor(message: String) : this(TOAST, message)
    constructor(title: String, message: String) : this(TOAST, title, message)
    constructor(@Form form: Int, message: String) : this(form, "", message)

    constructor(@Form form: Int, title: String, message: String) {
        this.form = form
        this.title = title
        this.message = message

        this.consumer = SelectionSpec.getInstance().noticeConsumer
    }
}