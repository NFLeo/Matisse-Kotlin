package com.matisse.entity

import android.content.Context
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.fragment.app.FragmentActivity
import com.matisse.internal.entity.SelectionSpec
import com.matisse.widget.IncapableDialog

class IncapableCause {

    companion object {
        const val TOAST = 0x0001
        const val DIALOG = 0x0002
        const val LOADING = 0x0003
        const val NONE = 0x0004

        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause?.noticeEvent != null) {
                cause.noticeEvent?.invoke(
                    context, cause.form, cause.title ?: "", cause.message ?: ""
                )
                return
            }

            when (cause?.form) {
                DIALOG -> {
                    IncapableDialog.newInstance(cause.title, cause.message)
                        .show(
                            (context as FragmentActivity).supportFragmentManager,
                            IncapableDialog::class.java.name
                        )
                }

                TOAST -> {
                    Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
                }

                LOADING -> {
                    // TODO Leo 2019-12-24 complete loading
                }
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TOAST, DIALOG, LOADING, NONE)
    annotation class Form

    var form = TOAST
    var title: String? = null
    var message: String? = null
    var dismissLoading: Boolean? = null
    var noticeEvent: ((
        context: Context, noticeType: Int, title: String, msg: String
    ) -> Unit)? = null

    constructor(message: String) : this(TOAST, message)
    constructor(@Form form: Int, message: String) : this(form, "", message)
    constructor(@Form form: Int, title: String, message: String) : this(form, title, message, true)
    constructor(@Form form: Int, title: String, message: String, dismissLoading: Boolean) {
        this.form = form
        this.title = title
        this.message = message
        this.dismissLoading = dismissLoading

        this.noticeEvent = SelectionSpec.getInstance().noticeEvent
    }
}