package com.matisse.entity

import android.content.Context
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.fragment.app.FragmentActivity
import com.matisse.internal.entity.SelectionSpec
import com.matisse.listener.NoticeConsumer
import com.matisse.widget.IncapableDialog

class IncapableCause {

    companion object {
        const val TOAST = 0x0001
        const val DIALOG = 0x0002
        const val NONE = 0x0003

        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause?.noticeConsumer != null) {
                cause.noticeConsumer?.accept(
                    context, cause.form, cause.title ?: "", cause.message ?: ""
                )
                return
            }

            when (cause?.form) {
                DIALOG -> {
                    val incapableDialog = IncapableDialog.newInstance(cause.title, cause.message)
                    incapableDialog.show(
                        (context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name
                    )
                }

                TOAST -> {
                    Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TOAST, DIALOG, NONE)
    annotation class Form

    var form = TOAST
    var title: String? = null
    var message: String? = null
    var noticeConsumer: NoticeConsumer? = null

    constructor(message: String) : this(TOAST, message)
    constructor(@Form form: Int, message: String) : this(form, "", message)
    constructor(@Form form: Int, title: String, message: String) {
        this.form = form
        this.title = title
        this.message = message

        this.noticeConsumer = SelectionSpec.getInstance().noticeConsumer
    }
}